"""The live judge (TODO.md 2.2l), against a loopback server and never the internet, with a fake key and never a real one.

The one test that calls the real API is `LiveSmoke`: it runs only when EVALUATOR_LIVE_SMOKE=1 and the key variable is set,
and says SKIPPED otherwise. This file may import `http` (guard.LOOPBACK_TEST) and may not read a key variable: the fake key is
set by `mock.patch.dict("os.environ", ...)`.
"""
import contextlib
import http.server
import io
import json
import logging
import os
import tempfile
import threading
import types
import unittest
from unittest import mock

from agent_runner import budget as budgets
from agent_runner import caps, judge, judgment, transport

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PRICES = os.path.join(ROOT, "prices", "2026-10-26.json")
FAKE_KEY = "test-key-not-real"
HAIKU, OPUS = "claude-haiku-5-5", "claude-opus-5-5"
SUMMARY = "A synthetic summary: a folder of notes and a short readme.\n"
ANSWER = '{"a": 1}'


def message(model=HAIKU, text=ANSWER, input_tokens=100, output_tokens=20, stop="end_turn", extra_usage=None, blocks=None):
    usage = {"input_tokens": input_tokens, "output_tokens": output_tokens}
    usage.update(extra_usage or {})
    content = blocks if blocks is not None else [{"type": "thinking", "thinking": "hmm", "signature": "s"}, {"type": "text", "text": text}]
    return {"id": "msg_x", "type": "message", "role": "assistant", "model": model, "content": content, "stop_reason": stop, "usage": usage}


def error(kind, text):
    return {"type": "error", "error": {"type": kind, "message": text}}


class Loopback:
    """A server on 127.0.0.1 that records each request and answers from a script of (status, headers, JSON-able or bytes)."""

    def __init__(self, script):
        self.script, self.requests = list(script), []
        outer = self

        class Handler(http.server.BaseHTTPRequestHandler):
            def do_POST(self):
                body = self.rfile.read(int(self.headers.get("content-length", 0)))
                outer.requests.append((self.path, {k.lower(): v for k, v in self.headers.items()}, body))
                status, headers, payload = outer.script[min(len(outer.requests), len(outer.script)) - 1]
                data = payload if isinstance(payload, bytes) else json.dumps(payload).encode("utf-8")
                self.send_response(status)
                self.send_header("content-type", "application/json")
                self.send_header("content-length", str(len(data)))
                for name, value in headers.items():
                    self.send_header(name, value)
                self.end_headers()
                self.wfile.write(data)

            def log_message(self, *args):
                pass

        self.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.url = "http://127.0.0.1:%d" % self.server.server_address[1]
        threading.Thread(target=self.server.serve_forever, kwargs={"poll_interval": 0.01}, daemon=True).start()

    def stop(self):
        self.server.shutdown()
        self.server.server_close()


def ok(**kw):
    return (200, {}, message(**kw))


def prompt_of(text="the rendered prompt", summary_text=SUMMARY):
    return judge.RenderedPrompt(text, "a" * 64, judge.summary_hash(summary_text))


class Base(unittest.TestCase):
    def serve(self, *script):
        server = Loopback(script)
        self.addCleanup(server.stop)
        return server

    def live(self, server, budget=None, output_limit=500, **kw):
        kw.setdefault("sleep", self.sleeps.append)
        kw.setdefault("retries", 3)
        return transport.live_judge(budget or budgets.Budget(), output_limit, base_url=server.url, **kw)

    def setUp(self):
        self.sleeps = []
        patcher = mock.patch.dict("os.environ", {"ANTHROPIC_KEY": FAKE_KEY})
        patcher.start()
        self.addCleanup(patcher.stop)

    def build_dir(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        target = os.path.join(tmp.name, "build")
        os.mkdir(target)
        return target


class RequestShape(Base):
    def test_the_request_carries_the_model_the_limit_the_prompt_the_schema_and_the_effort(self):
        server = self.serve(ok())
        got = self.live(server, output_limit=777).judge(prompt_of(), HAIKU, 1)
        path, headers, raw = server.requests[0]
        body = json.loads(raw.decode("utf-8"))
        self.assertEqual("/v1/messages", path)
        with open(judgment.SCHEMA_PATH, encoding="utf-8") as handle:
            schema = json.load(handle)
        self.assertEqual({"model", "max_tokens", "messages", "output_config"}, set(body))
        self.assertEqual((HAIKU, 777, [{"role": "user", "content": "the rendered prompt"}]), (body["model"], body["max_tokens"], body["messages"]))
        self.assertEqual({"format": {"type": "json_schema", "schema": schema}, "effort": transport.EFFORT}, body["output_config"])
        self.assertEqual("medium", transport.EFFORT)
        self.assertEqual(("2023-06-01", "application/json"), (headers["anthropic-version"], headers["content-type"]))
        self.assertEqual(judge.Answer(ANSWER, 100, 20), got)

    def test_the_fake_key_is_sent_as_x_api_key_and_with_no_variable_no_key_header_is_sent(self):
        server = self.serve(ok(), ok(), ok())
        self.live(server).judge(prompt_of(), HAIKU, 1)
        self.assertEqual(FAKE_KEY, server.requests[0][1]["x-api-key"])
        with mock.patch.dict("os.environ", {}, clear=True):
            self.assertFalse(transport.key_present())
            self.live(server).judge(prompt_of(), HAIKU, 1)
        with mock.patch.dict("os.environ", {"ANTHROPIC_KEY": ""}):
            self.assertFalse(transport.key_present())
            self.live(server).judge(prompt_of(), HAIKU, 1)
        self.assertEqual([True, False, False], ["x-api-key" in h for _, h, _ in server.requests])
        self.assertTrue(transport.key_present())

    def test_the_answer_is_the_text_blocks_joined_and_the_tokens_include_the_cache(self):
        blocks = [{"type": "thinking", "thinking": "x", "signature": "s"}, {"type": "text", "text": '{"a"'},
                  {"type": "tool_use", "id": "t", "name": "n", "input": {}}, {"type": "text", "text": ": 1}"}]
        server = self.serve(ok(blocks=blocks, input_tokens=10, output_tokens=5,
                               extra_usage={"cache_creation_input_tokens": 3, "cache_read_input_tokens": 4, "service_tier": "standard"}))
        self.assertEqual(judge.Answer(ANSWER, 17, 5), self.live(server).judge(prompt_of(), HAIKU, 1))

    def test_a_cut_or_refused_answer_is_returned_as_the_text_it_has_and_the_stop_reason_is_logged(self):
        server = self.serve(ok(text='{"a": ', stop="max_tokens"), ok(blocks=[], stop="refusal"))
        stops = []
        live = self.live(server, stop_log=stops)
        self.assertEqual('{"a": ', live.judge(prompt_of(), HAIKU, 1).text)
        self.assertEqual("", live.judge(prompt_of(), HAIKU, 2).text)
        self.assertEqual([(HAIKU, 1, "max_tokens"), (HAIKU, 2, "refusal")], stops)

    def test_a_model_that_answers_under_another_id_raises(self):
        server = self.serve(ok(model="claude-other"))
        with self.assertRaises(transport.TransportError) as caught:
            self.live(server).judge(prompt_of(), HAIKU, 1)
        self.assertIn("claude-other", str(caught.exception))

    def test_a_response_that_is_not_a_message_raises(self):
        for payload in (b"not json", b"[]", b'{"model": "%s"}' % HAIKU.encode(), json.dumps(message(input_tokens="x")).encode()):
            with self.subTest(payload[:20]):
                server = self.serve((200, {}, payload))
                with self.assertRaises(transport.TransportError):
                    self.live(server).judge(prompt_of(), HAIKU, 1)


class Retries(Base):
    def test_429_then_200_succeeds_after_one_retry_and_sleeps_the_retry_after(self):
        server = self.serve((429, {"retry-after": "2"}, error("rate_limit_error", "slow down")), ok())
        self.assertEqual(ANSWER, self.live(server).judge(prompt_of(), HAIKU, 1).text)
        self.assertEqual(2, len(server.requests))
        self.assertEqual([2.0], self.sleeps)

    def test_without_retry_after_the_sleep_doubles(self):
        server = self.serve((500, {}, error("api_error", "x")), (529, {}, error("overloaded_error", "busy")), (503, {}, b"<html>"), ok())
        self.live(server).judge(prompt_of(), HAIKU, 1)
        self.assertEqual(4, len(server.requests))
        self.assertEqual([transport.BACKOFF * 1, transport.BACKOFF * 2, transport.BACKOFF * 4], self.sleeps)

    def test_a_retry_after_that_is_not_a_number_or_is_huge_is_bounded(self):
        server = self.serve((429, {"retry-after": "Wed, 21 Oct 2026 07:28:00 GMT"}, error("rate_limit_error", "x")),
                            (429, {"retry-after": "100000"}, error("rate_limit_error", "x")), ok())
        self.live(server).judge(prompt_of(), HAIKU, 1)
        self.assertEqual([transport.BACKOFF, transport.MAX_SLEEP], self.sleeps)

    def test_repeated_500s_stop_after_the_set_number_and_raise_naming_the_status_and_type(self):
        server = self.serve((500, {}, error("api_error", "internal trouble")))
        with self.assertRaises(transport.TransportError) as caught:
            self.live(server, retries=2).judge(prompt_of(), HAIKU, 1)
        self.assertEqual(3, len(server.requests))
        self.assertEqual(2, len(self.sleeps))
        for part in ("HTTP 500", "api_error", "internal trouble", "2 retries"):
            self.assertIn(part, str(caught.exception))

    def test_zero_retries_means_one_try(self):
        server = self.serve((429, {}, error("rate_limit_error", "x")))
        with self.assertRaises(transport.TransportError):
            self.live(server, retries=0).judge(prompt_of(), HAIKU, 1)
        self.assertEqual((1, []), (len(server.requests), self.sleeps))

    def test_a_400_is_not_retried(self):
        server = self.serve((400, {}, error("invalid_request_error", "max_tokens is too large")), ok())
        with self.assertRaises(transport.TransportError) as caught:
            self.live(server).judge(prompt_of(), HAIKU, 1)
        self.assertEqual((1, []), (len(server.requests), self.sleeps))
        for part in ("HTTP 400", "invalid_request_error", "max_tokens is too large"):
            self.assertIn(part, str(caught.exception))
        for status in (401, 403, 404):
            with self.subTest(status):
                server = self.serve((status, {}, b"plain text"))
                with self.assertRaises(transport.TransportError):
                    self.live(server).judge(prompt_of(), HAIKU, 1)
                self.assertEqual(1, len(server.requests))

    def test_a_connection_that_fails_is_retried_then_raises(self):
        server = self.serve(ok())
        url = server.url
        server.stop()  # nothing listens there now
        live = transport.live_judge(budgets.Budget(), 10, base_url=url, retries=2, sleep=self.sleeps.append)
        with self.assertRaises(transport.TransportError) as caught:
            live.judge(prompt_of(), HAIKU, 1)
        self.assertEqual(2, len(self.sleeps))
        self.assertIn("no answer", str(caught.exception))


class Refusals(Base):
    def test_a_budget_refusal_never_reaches_the_server(self):
        server = self.serve(ok())
        with self.assertRaises(budgets.BudgetRefused):
            self.live(server, budgets.Budget(per_call=10), output_limit=500).judge(prompt_of(), HAIKU, 1)
        self.assertEqual(0, len(server.requests))

    def test_a_dollar_cap_refuses_unmade_and_a_call_under_it_is_charged_in_dollars(self):
        prices = budgets.load_prices(PRICES)
        server = self.serve(ok(input_tokens=1000, output_tokens=200))
        with self.assertRaises(budgets.BudgetRefused) as caught:
            self.live(server, budgets.Budget(prices=prices, dollars_per_call=0.0001), output_limit=8000).judge(prompt_of(), OPUS, 1)
        self.assertEqual("dollars", caught.exception.unit)
        self.assertEqual(0, len(server.requests))
        budget = budgets.Budget(prices=prices, dollars_per_call=0.01)
        self.live(server, budget, output_limit=500).judge(prompt_of(), HAIKU, 1)
        self.assertEqual(1, len(server.requests))
        self.assertEqual(("0.0002", HAIKU), (budget.records[0]["dollars"], budget.records[0]["model"]))

    def test_a_prompt_above_the_price_limit_is_refused_unmade(self):
        server = self.serve(ok())
        budget = budgets.Budget(prices=budgets.load_prices(PRICES))
        with self.assertRaises(budgets.PromptTooLong):
            self.live(server, budget).judge(prompt_of("x" * 300010), HAIKU, 1)  # estimate 100,004 tokens
        self.assertEqual(0, len(server.requests))

    def test_the_live_judge_cannot_be_built_without_a_budget(self):
        server = self.serve(ok())
        for bad in (None, "budget", 5, object()):
            with self.assertRaises(transport.TransportError):
                transport.live_judge(bad, 100, base_url=server.url)
        self.assertNotIn("_LiveJudge", transport.__all__)
        self.assertIsInstance(transport.live_judge(budgets.Budget(), 100, base_url=server.url), budgets.BudgetedJudge)

    def test_a_base_url_that_would_send_the_key_in_the_clear_is_refused(self):
        for url in ("http://example.invalid", "ftp://127.0.0.1", "api.anthropic.com", "https://"):
            with self.assertRaises(transport.TransportError, msg=url):
                transport.live_judge(budgets.Budget(), 100, base_url=url)

    def test_a_key_a_header_cannot_carry_is_refused_without_being_shown(self):
        server = self.serve(ok())
        for bad in ("two words", "line\nbreak", "tab\there"):
            with mock.patch.dict("os.environ", {"ANTHROPIC_KEY": bad}):
                with self.assertRaises(transport.TransportError) as caught:
                    self.live(server).judge(prompt_of(), HAIKU, 1)
            self.assertNotIn(bad.split()[0], str(caught.exception))
        self.assertEqual(0, len(server.requests))


class Proxies(Base):
    def test_a_loopback_server_is_never_sent_through_a_proxy(self):
        server = self.serve(ok())
        with mock.patch.dict("os.environ", {"HTTPS_PROXY": "http://127.0.0.1:9", "https_proxy": "http://127.0.0.1:9", "HTTP_PROXY": "http://127.0.0.1:9"}):
            self.live(server).judge(prompt_of(), HAIKU, 1)
        self.assertEqual(1, len(server.requests))

    def test_a_real_host_goes_through_the_environments_https_proxy_as_a_tunnel(self):
        url = types.SimpleNamespace(scheme="https", hostname="api.anthropic.com", port=None)
        with mock.patch.dict("os.environ", {"HTTPS_PROXY": "http://user:pw@proxy.invalid:8080"}, clear=True):
            connection = transport._connection(url, 5)
        self.assertEqual(("proxy.invalid", 8080, "api.anthropic.com", 443), (connection.host, connection.port, connection._tunnel_host, connection._tunnel_port))
        self.assertIn("Proxy-Authorization", connection._tunnel_headers)
        with mock.patch.dict("os.environ", {}, clear=True):
            connection = transport._connection(url, 5)
        self.assertEqual(("api.anthropic.com", 443, None), (connection.host, connection.port, connection._tunnel_host))
        with mock.patch.dict("os.environ", {"HTTPS_PROXY": "http://proxy.invalid:8080", "NO_PROXY": "api.anthropic.com"}, clear=True):
            self.assertIsNone(transport._connection(url, 5)._tunnel_host)


class Recordings(Base):
    def test_each_answer_is_one_added_line_in_the_recordings_format_and_replays(self):
        target = os.path.join(self.build_dir(), "runs", "rec.jsonl")
        server = self.serve(ok(text="first", input_tokens=7, output_tokens=3), ok(text="second"), ok(model=OPUS, text="third"), ok(text="again"))
        live = self.live(server, recordings_path=target)
        live.judge(prompt_of(), HAIKU, 1)
        live.judge(prompt_of(), HAIKU, 2)
        live.judge(prompt_of(), OPUS, 1)
        with open(target, encoding="utf-8") as handle:
            text = handle.read()
        recordings = judge.parse_recordings(text)
        self.assertEqual(3, len(recordings))
        self.assertEqual(text, judge.dumps(recordings))
        replay = judge.judge_from_recordings(target)
        self.assertEqual(judge.Answer("first", 7, 3), replay.judge(prompt_of(), HAIKU, 1))
        self.assertEqual("second", replay.judge(prompt_of(), HAIKU, 2).text)
        self.assertEqual("third", replay.judge(prompt_of(), OPUS, 1).text)
        with self.assertRaises(judge.MissingRecording):
            replay.judge(prompt_of(), HAIKU, 3)
        live.judge(prompt_of(), HAIKU, 1)  # the same key again replaces its line
        self.assertEqual(["again", "second", "third"], sorted(a.text for a in judge.judge_from_recordings(target)._recordings.values()))
        self.assertEqual(4, len(judge.dumps(judge.load_recordings(target)).splitlines()))
        self.assertFalse(os.path.exists(target + ".tmp"))

    def test_a_failed_call_records_nothing(self):
        target = os.path.join(self.build_dir(), "rec.jsonl")
        server = self.serve((400, {}, error("invalid_request_error", "no")))
        with self.assertRaises(transport.TransportError):
            self.live(server, recordings_path=target).judge(prompt_of(), HAIKU, 1)
        self.assertFalse(os.path.exists(target))

    def test_a_path_outside_build_is_refused_at_construction(self):
        server = self.serve(ok())
        with tempfile.TemporaryDirectory() as tmp:
            for path in (os.path.join(tmp, "rec.jsonl"), os.path.join(tmp, "builds", "rec.jsonl"), os.path.join(tmp, "rec.jsonl", "build")):
                with self.assertRaises(transport.TransportError, msg=path):
                    self.live(server, recordings_path=path)
            self.assertEqual([], os.listdir(tmp))
        self.assertEqual(0, len(server.requests))


class TheKeyAppearsNowhere(Base):
    def test_not_in_a_recording_a_cost_record_a_log_stderr_stdout_an_exception_or_a_repr(self):
        build = self.build_dir()
        recordings, costs = os.path.join(build, "rec.jsonl"), os.path.join(build, "costs.json")
        seen = []
        server = self.serve(ok(), (400, {}, error("invalid_request_error", "the key %s is wrong" % FAKE_KEY)), (500, {}, b"leak " + FAKE_KEY.encode()),
                            ok(model="claude-other"))
        budget = budgets.Budget(prices=budgets.load_prices(PRICES))
        logs, out, err = io.StringIO(), io.StringIO(), io.StringIO()
        handler = logging.StreamHandler(logs)
        root = logging.getLogger()
        root.addHandler(handler)
        previous = root.level
        root.setLevel(logging.DEBUG)
        try:
            with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
                live = self.live(server, budget, recordings_path=recordings, retries=0)
                seen.append(repr(live))
                seen.append(repr(live._judge))
                live.judge(prompt_of(), HAIKU, 1)
                for run in (2, 3, 4):
                    with self.assertRaises(transport.TransportError) as caught:
                        live.judge(prompt_of(), HAIKU, run)
                    seen += [str(caught.exception), repr(caught.exception)]
                with mock.patch.dict("os.environ", {"ANTHROPIC_KEY": "bad key"}):
                    with self.assertRaises(transport.TransportError) as caught:
                        live.judge(prompt_of(), HAIKU, 5)
                    seen.append(str(caught.exception))
        finally:
            root.removeHandler(handler)
            root.setLevel(previous)
        budget.write_records(costs)
        for path in (recordings, costs):
            with open(path, encoding="utf-8") as handle:
                seen.append(handle.read())
        seen += [logs.getvalue(), out.getvalue(), err.getvalue(), budget.dumps_records(), repr(budget.refusals)]
        self.assertGreater(len(seen), 8)
        for text in seen:
            self.assertNotIn(FAKE_KEY, text)
        self.assertIn("[key]", seen[3] + seen[5])  # the API's own echo was scrubbed, not passed on


class LiveSmoke(unittest.TestCase):
    """The one call to the real API, on a short synthetic summary. The orchestrator runs it after review."""

    def test_one_real_haiku_call_returns_an_answer_that_parses(self):
        if os.environ.get("EVALUATOR_LIVE_SMOKE") != "1":
            self.skipTest("SKIPPED: EVALUATOR_LIVE_SMOKE=1 is not set (the smoke test calls the real API once)")
        if not transport.key_present():
            self.skipTest("SKIPPED: no key in the environment (the smoke test needs the API key variable)")
        with tempfile.TemporaryDirectory() as tmp:
            build = os.path.join(tmp, "build")
            os.mkdir(build)
            prices = budgets.load_prices(PRICES)
            budget = budgets.Budget(prices=prices, dollars_per_evaluation=0.05)
            stops = []
            live = transport.live_judge(budget, caps.load_caps().output_limit, recordings_path=os.path.join(build, "smoke.jsonl"), stop_log=stops)
            answer = live.judge(judge.rendered(SUMMARY), HAIKU, 1)
            budget.write_records(os.path.join(build, "smoke-costs.json"))
            try:
                judgment.parse(answer.text)
            except judgment.JudgmentError as problem:
                self.fail("the answer does not parse (stop reason %s, %d output tokens): %s" % (stops[-1][2], answer.output_tokens, problem))
            self.assertEqual(1, len(judge.judge_from_recordings(os.path.join(build, "smoke.jsonl"))._recordings))
            self.assertEqual(1, len(budget.records))


if __name__ == "__main__":
    unittest.main()
