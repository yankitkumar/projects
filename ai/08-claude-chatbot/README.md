# 08 · Claude Chatbot

A terminal chatbot built on the [Claude API](https://platform.claude.com/docs) with the official Python SDK. It streams replies as they are generated and remembers the conversation.

```bash
pip install -r ../requirements.txt
export ANTHROPIC_API_KEY=...      # or run `ant auth login`
python chat.py
```

Commands: `/reset` clears the conversation and `/quit` exits.

## How it works

- The API is stateless, so `Chatbot` keeps the message history and resends it each turn.
- Replies stream through `client.beta.messages.stream`, printing text as it arrives.
- The model is `claude-opus-5-5` with `effort: medium`. Change `MODEL` in `chat.py` to use another.
- **Server-side fallback** is switched on (`fallbacks: "default"`). If a safety classifier declines a request, the API retries it on a recommended fallback model instead of just refusing.
- The assistant's full content blocks, not only the text, go back into the history, so thinking blocks round-trip unchanged.
- If a request fails or is declined, the user message is removed from the history so the conversation stays valid and you can try again.
- A missing key, an invalid key, rate limits and network errors print a one-line message instead of a traceback.

## Tests

```bash
python -m unittest
```

The tests run the real SDK against a mock HTTP transport that replays canned streaming responses, so they need no key and no network. They check streaming, history across turns, the refusal path, error rollback and the exact request the SDK sends: model, `fallbacks`, the beta header and the body.

**Not tested here:** live calls to the API. The request and response shapes are verified offline, but I didn't run a real conversation, so try it once with your key.
