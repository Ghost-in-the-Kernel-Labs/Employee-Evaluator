"""The one door to the network and to a key (TODO.md 2.2l, E-087): the live `Judge`, the counterpart of MOD-05 in the kernel.

  live_judge(budget, output_limit, ...)  a `BudgetedJudge` over the live judge. This is the only way to get one: the class is
                                         private and the factory needs a `budget.Budget`, so a call that the budget refuses
                                         is never made and nothing reaches the network without passing it.
  key_present()                          True when the key variable is set and not empty; a bool, never the value.
  TransportError                         a failure of the door; its message names a status and the API's error type and
                                         message, never a header or the key.

One call is a POST of `{base_url}/v1/messages` (default `https://api.anthropic.com`), standard-library HTTP, no SDK (Ghost's
choice). Headers: `content-type: application/json`, `anthropic-version: 2023-06-01`, and `x-api-key` only when the environment
variable `ANTHROPIC_KEY` is set and not empty: with it unset no key header is sent at all, so a proxy may inject one. This file
alone reads that variable (guard.py bans it elsewhere); the key is read for each call, never stored on the judge, and is never
printed, logged, written, put in a repr, in an exception message or in a recording. A key a header cannot carry is refused
without being shown. Body: `model`, `max_tokens` (the output limit), `messages` (the prompt text as one user message) and
`output_config` with `format` {type json_schema, schema: `api_schema` of the parsed 2.2d schema file} and `effort` (EFFORT). The
schema is enforced by the API's structured output, not asked for in prose. The API refuses `minimum` and `maximum` on an
integer (HTTP 400, the first smoke run, 2026-10-26), so `api_schema` drops the numeric range keywords (NUMERIC_RANGE) from
the copy sent; each range is also in its field's description, and `judgment.parse` still enforces it, so an answer out of
range is an unparsable one. The file, and so the prompt hash, are unchanged. No `thinking`, `temperature`, `top_p`, `top_k` or fallback is
sent: both models think adaptively by default, thinking tokens are billed as output and count inside `max_tokens`.

The answer is the concatenation of the response's `text` blocks (`thinking` and other blocks skipped). Tokens are `usage`'s
`input_tokens` plus any `cache_creation_input_tokens` and `cache_read_input_tokens`, and `output_tokens`. A response whose
`model` is not the one asked for raises (a record must name the model that answered). A `stop_reason` of `max_tokens` or
`refusal` is not an error here: the text it has (possibly empty or cut) is the answer, and the layers above meet an unparsable
one (2.2h retries it once). `stop_log`, a list given to the factory, receives (model, run, stop_reason) for every answer.

Retries: HTTP 429, 500 to 599 (529, overloaded, included) and connection errors or timeouts are retried up to `retries` times
(default RETRIES = 3), after sleeps of BACKOFF * FACTOR**attempt seconds: 3, 9, then 27 (Ghost, 2026-10-29: a jammed
connection is a spike of load or a flaky link, and both clear in widening steps); after the third retry fails, the call fails.
A `retry-after` header is not read: the steps are the rule. `sleep` is injectable. Any other status fails at once. A certificate that does not verify, or a proxy that
refuses the tunnel, fails at once too. TLS is never relaxed; trust comes from the default context, which OpenSSL fills from
`SSL_CERT_FILE` itself. The environment's HTTPS proxy (`urllib.request.getproxies`, its no-proxy list honoured) carries a call to
a real host through a CONNECT tunnel; a loopback host (127.0.0.1, ::1, localhost) is never sent through a proxy. The key travels
only over https, or over http to a loopback host (the tests' server); any other base URL is refused.

Recordings: with `recordings_path`, which must lie under a directory named `build` (E-014, refused at construction otherwise),
each answer is merged into the 2.2f recordings file (load, add the key (prompt hash, summary hash, model, run), `judge.dumps`,
replace the file) before the budget charges it; the file holds answers and token counts and nothing of the key.
"""
import base64
import http.client
import json
import os
import re
import ssl
import time
import urllib.parse
import urllib.request

from agent_runner import budget as budgets
from agent_runner import judge as judges
from agent_runner import judgment

# Decision: E-003
# Decision: E-013
# Decision: E-014
# Decision: E-017
# Decision: E-087
# Decision: E-089
DEFAULT_BASE_URL = "https://api.anthropic.com"
API_VERSION = "2023-06-01"
KEY_VARIABLE = "ANTHROPIC_KEY"
EFFORT = "medium"
RETRIES = 3
BACKOFF = 3.0
FACTOR = 3
TIMEOUT = 300
LOOPBACK = frozenset({"127.0.0.1", "::1", "localhost"})
NUMERIC_RANGE = frozenset({"minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum"})
_KEY_SHAPE = re.compile(r"[\x21-\x7e]+")

__all__ = ("live_judge", "key_present", "TransportError", "EFFORT")


class TransportError(RuntimeError):
    """A failure of the door: a refused request, a bad response, a failed call. Never carries a header or the key."""


def _key():
    value = os.environ.get(KEY_VARIABLE)
    if not value:
        return None
    if not _KEY_SHAPE.fullmatch(value):
        raise TransportError("the %s variable holds characters a header cannot carry" % KEY_VARIABLE)
    return value


def key_present():
    """True when the key variable is set and not empty. A bool only: the value never leaves this file."""
    return bool(os.environ.get(KEY_VARIABLE))


def _scrub(text):
    """`text` without the key, were it in there, cut to 300 characters."""
    value = os.environ.get(KEY_VARIABLE)
    if value:
        text = text.replace(value, "[key]")
    return text[:300]


def _loopback(host):
    return host in LOOPBACK


def _check_recordings_path(path):
    if "build" not in os.path.abspath(path).split(os.sep)[:-1]:
        raise TransportError("recordings are written under build/ only, not %s" % path)


def _connection(url, timeout):
    """An http.client connection to `url`'s host: direct, or through the environment's HTTPS proxy for a real host."""
    host, scheme = url.hostname, url.scheme
    if scheme == "http" and _loopback(host):
        return http.client.HTTPConnection(host, url.port or 80, timeout=timeout)
    if scheme != "https":
        raise TransportError("the base URL must be https (or http to a loopback host): the key travels over it")
    port = url.port or 443
    context = ssl.create_default_context()
    proxy = None if _loopback(host) or urllib.request.proxy_bypass(host) else urllib.request.getproxies().get("https")
    if not proxy:
        return http.client.HTTPSConnection(host, port, timeout=timeout, context=context)
    parts = urllib.parse.urlsplit(proxy if "://" in proxy else "http://" + proxy)
    connection = http.client.HTTPSConnection(parts.hostname, parts.port or 3128, timeout=timeout, context=context)
    headers = {}
    if parts.username is not None:
        token = "%s:%s" % (urllib.parse.unquote(parts.username), urllib.parse.unquote(parts.password or ""))
        headers["Proxy-Authorization"] = "Basic " + base64.b64encode(token.encode("utf-8")).decode("ascii")
    connection.set_tunnel(host, port, headers)
    return connection


def api_schema(schema):
    """The copy of the 2.2d schema the API is sent: every numeric range keyword dropped, everything else as in the file."""
    if isinstance(schema, dict):
        return {k: api_schema(v) for k, v in schema.items() if k not in NUMERIC_RANGE}
    if isinstance(schema, list):
        return [api_schema(v) for v in schema]
    return schema


def _error_detail(body):
    """The API's error type and message from an error body, scrubbed; "" when the body is not the API's shape."""
    try:
        error = json.loads(body.decode("utf-8")).get("error", {})
        return _scrub("%s: %s" % (error.get("type", "?"), error.get("message", "")))
    except (ValueError, AttributeError, UnicodeDecodeError):
        return ""


class _LiveJudge(judges.Judge):
    """The live judge. Private: `live_judge` is the only way to one, and it needs a `Budget`."""

    def __init__(self, output_limit, base_url, retries, sleep, timeout, recordings_path, stop_log):
        url = urllib.parse.urlsplit(base_url)
        if url.scheme not in ("http", "https") or not url.hostname:
            raise TransportError("the base URL is not an http(s) URL")
        if url.scheme == "http" and not _loopback(url.hostname):
            raise TransportError("the base URL must be https (or http to a loopback host): the key travels over it")
        if not (isinstance(retries, int) and not isinstance(retries, bool) and retries >= 0):
            raise TransportError("retries is an integer of 0 or more")
        if recordings_path is not None:
            _check_recordings_path(recordings_path)
        self._url, self._base = url, base_url.rstrip("/")
        self._output_limit, self._retries, self._sleep, self._timeout = output_limit, retries, sleep, timeout
        self._recordings_path, self._stop_log = recordings_path, stop_log
        self._schema = api_schema(judgment.load_schema())

    def __repr__(self):
        return "LiveJudge(base_url=%r, retries=%d, output_limit=%d)" % (self._base, self._retries, self._output_limit)

    def _body(self, prompt, model):
        return json.dumps({
            "model": model, "max_tokens": self._output_limit,
            "messages": [{"role": "user", "content": prompt.text}],
            "output_config": {"format": {"type": "json_schema", "schema": self._schema}, "effort": EFFORT},
        }, ensure_ascii=False).encode("utf-8")

    def _headers(self):
        headers = {"content-type": "application/json", "anthropic-version": API_VERSION}
        key = _key()
        if key is not None:
            headers["x-api-key"] = key
        return headers

    def _once(self, body):
        """(status, body bytes) of one POST; OSError and HTTPException go to the caller."""
        connection = _connection(self._url, self._timeout)
        try:
            connection.request("POST", self._url.path.rstrip("/") + "/v1/messages", body=body, headers=self._headers())
            response = connection.getresponse()
            return response.status, response.read()
        finally:
            connection.close()

    def _delay(self, attempt):
        return BACKOFF * FACTOR ** attempt

    def _post(self, body):
        """The 200 response's bytes; retries as the module docstring says, else `TransportError`."""
        for attempt in range(self._retries + 1):
            try:
                status, data = self._once(body)
                problem = None
            except ssl.SSLCertVerificationError:
                raise TransportError("the API host's certificate did not verify") from None
            except (OSError, http.client.HTTPException) as error:
                if str(error).startswith("Tunnel connection failed"):
                    raise TransportError("the proxy refused the tunnel: %s" % _scrub(str(error))) from None
                status, data = None, b""
                problem = "no answer (%s)" % type(error).__name__
            if status == 200:
                return data
            if problem is None:
                detail = _error_detail(data)
                problem = "HTTP %d%s" % (status, " " + detail if detail else "")
                if status != 429 and not 500 <= status <= 599:
                    raise TransportError(problem)
            if attempt == self._retries:
                raise TransportError("%s, after %d retries" % (problem, self._retries))
            self._sleep(self._delay(attempt))

    def judge(self, prompt, model, run):
        data = self._post(self._body(prompt, model))
        try:
            reply = json.loads(data.decode("utf-8"))
            if reply["model"] != model:
                raise TransportError("asked for model %s, and %s answered" % (model, _scrub(str(reply["model"]))))
            text = "".join(block["text"] for block in reply["content"] if block.get("type") == "text")
            usage = reply["usage"]
            input_tokens = usage["input_tokens"] + usage.get("cache_creation_input_tokens", 0) + usage.get("cache_read_input_tokens", 0)
            answer = judges.Answer(text, input_tokens, usage["output_tokens"])
            stop = reply.get("stop_reason")
        except TransportError:
            raise
        except (ValueError, KeyError, TypeError, AttributeError):
            raise TransportError("the response is not the API's message shape") from None
        if self._stop_log is not None:
            self._stop_log.append((model, run, stop))
        if self._recordings_path is not None:
            self._record(prompt, model, run, answer)
        return answer

    def _record(self, prompt, model, run, answer):
        path = self._recordings_path
        recordings = judges.load_recordings(path) if os.path.exists(path) else {}
        recordings[judges.key_of(prompt.prompt_hash, prompt.summary_hash, model, run)] = answer
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        temporary = path + ".tmp"
        with open(temporary, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(judges.dumps(recordings))
        os.replace(temporary, path)


def live_judge(budget, output_limit, base_url=DEFAULT_BASE_URL, recordings_path=None, retries=RETRIES,
               sleep=time.sleep, timeout=TIMEOUT, stop_log=None):
    """The live `Judge`, run under `budget` (a `budget.Budget`): a `BudgetedJudge` that sends `max_tokens` = `output_limit`."""
    if not isinstance(budget, budgets.Budget):
        raise TransportError("the live judge is built under a Budget, and only under one")
    inner = _LiveJudge(output_limit, base_url, retries, sleep, timeout, recordings_path, stop_log)
    return budgets.BudgetedJudge(inner, budget, output_limit)
