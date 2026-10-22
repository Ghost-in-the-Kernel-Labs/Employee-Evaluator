import { grade } from "./examples.js";
import { neverUsed } from "./examples.js";

export async function onRequest(context) {
  const url = new URL(context.request.url);
  if (!url.pathname.startsWith("/api")) {
    return new Response("not found", { status: 404 });
  }
  try {
    const body = await context.request.json();
    return Response.json({ grade: grade(body.score ?? 0) });
  } catch (e) {
    return new Response(String(e), { status: e.status || 500 });
  }
}
