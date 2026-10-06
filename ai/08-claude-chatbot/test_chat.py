"""Offline tests: the real Anthropic SDK runs, but HTTP is served by a canned mock transport.

No API key or network is needed, and the tests inspect the exact request the SDK sends.
"""

import json
import unittest

import anthropic
import httpx2

from chat import FALLBACK_BETA, MODEL, Chatbot, Refusal


def sse(*events):
    return "".join("event: %s\ndata: %s\n\n" % (e["type"], json.dumps(e)) for e in events).encode()


def reply(*chunks, stop_reason="end_turn", stop_details=None):
    """Build a streamed Messages API response that emits `chunks` as text."""
    events = [{"type": "message_start", "message": {
        "id": "msg_test", "type": "message", "role": "assistant", "model": MODEL, "content": [],
        "stop_reason": None, "stop_sequence": None,
        "usage": {"input_tokens": 10, "output_tokens": 1}}}]
    if chunks:
        events.append({"type": "content_block_start", "index": 0,
                       "content_block": {"type": "text", "text": ""}})
        events += [{"type": "content_block_delta", "index": 0,
                    "delta": {"type": "text_delta", "text": c}} for c in chunks]
        events.append({"type": "content_block_stop", "index": 0})
    delta = {"stop_reason": stop_reason, "stop_sequence": None}
    if stop_details:
        delta["stop_details"] = stop_details
    events += [{"type": "message_delta", "delta": delta, "usage": {"output_tokens": 5}},
               {"type": "message_stop"}]
    return httpx2.Response(200, content=sse(*events), headers={"content-type": "text/event-stream"})


def make_bot(*responses):
    """A Chatbot wired to a real SDK client whose HTTP layer replays `responses` in order."""
    requests, queue = [], list(responses)

    def handler(request):
        requests.append(request)
        return queue.pop(0)

    client = anthropic.Anthropic(
        api_key="test-key",
        max_retries=0,
        http_client=anthropic.DefaultHttpxClient(transport=httpx2.MockTransport(handler)),
    )
    return Chatbot(client), requests


class ChatbotTests(unittest.TestCase):
    def test_streams_text_and_returns_full_reply(self):
        bot, _ = make_bot(reply("Hello", ", ", "world!"))
        seen = []
        text = bot.ask("hi", on_text=seen.append)
        self.assertEqual(seen, ["Hello", ", ", "world!"])
        self.assertEqual(text, "Hello, world!")

    def test_request_shape(self):
        bot, requests = make_bot(reply("ok"))
        bot.ask("What is 2+2?")
        request = requests[0]
        body = json.loads(request.content)
        self.assertEqual(body["model"], "claude-opus-5-5")
        self.assertTrue(body["stream"])
        self.assertEqual(body["fallbacks"], "default")
        self.assertEqual(body["output_config"], {"effort": "medium"})
        self.assertEqual(body["messages"], [{"role": "user", "content": "What is 2+2?"}])
        self.assertIn(FALLBACK_BETA, request.headers["anthropic-beta"])
        self.assertNotIn("temperature", body)  # sampling params are rejected on current models

    def test_history_is_resent_on_the_next_turn(self):
        bot, requests = make_bot(reply("Nice to meet you, Alice."), reply("Your name is Alice."))
        bot.ask("My name is Alice.")
        bot.ask("What's my name?")
        second = json.loads(requests[1].content)["messages"]
        self.assertEqual([m["role"] for m in second], ["user", "assistant", "user"])
        self.assertEqual(second[0]["content"], "My name is Alice.")
        self.assertEqual(second[1]["content"][0]["text"], "Nice to meet you, Alice.")

    def test_refusal_raises_and_leaves_history_clean(self):
        details = {"type": "refusal", "category": "cyber", "explanation": None}
        bot, _ = make_bot(reply(stop_reason="refusal", stop_details=details))
        with self.assertRaises(Refusal) as ctx:
            bot.ask("something declined")
        self.assertEqual(ctx.exception.category, "cyber")
        self.assertEqual(bot.messages, [])

    def test_api_error_rolls_back_the_user_message(self):
        error = httpx2.Response(401, json={"type": "error", "error": {
            "type": "authentication_error", "message": "invalid x-api-key"}})
        bot, _ = make_bot(error)
        with self.assertRaises(anthropic.AuthenticationError):
            bot.ask("hello")
        self.assertEqual(bot.messages, [])

    def test_reset_clears_history(self):
        bot, _ = make_bot(reply("ok"))
        bot.ask("hi")
        self.assertEqual(len(bot.messages), 2)
        bot.reset()
        self.assertEqual(bot.messages, [])


if __name__ == "__main__":
    unittest.main()
