"""Terminal chatbot powered by the Claude API: streaming replies with multi-turn memory.

Needs credentials: export ANTHROPIC_API_KEY=... (or run `ant auth login`).
"""

import anthropic

MODEL = "claude-opus-5-5"
SYSTEM_PROMPT = (
    "You are a friendly, concise assistant running in a terminal. "
    "Keep answers short unless the user asks for detail, and prefer plain text over heavy markdown."
)
FALLBACK_BETA = "server-side-fallback-2026-07-01"


class Refusal(Exception):
    """Claude's safety classifiers declined to answer (and no fallback model could)."""

    def __init__(self, category=None):
        super().__init__("request declined (%s)" % (category or "unspecified"))
        self.category = category


class Chatbot:
    def __init__(self, client=None, model=MODEL, system=SYSTEM_PROMPT, max_tokens=16000):
        self.client = client or anthropic.Anthropic()
        self.model = model
        self.system = system
        self.max_tokens = max_tokens
        self.messages = []  # the API is stateless, so we resend the whole history every turn

    def reset(self):
        self.messages = []

    def ask(self, user_message, on_text=None):
        """Send one user message, stream the reply through `on_text`, and return the full text.

        If the request fails or is declined, the user message is dropped from the history so
        the conversation stays valid and the user can simply try again.
        """
        self.messages.append({"role": "user", "content": user_message})
        try:
            with self.client.beta.messages.stream(
                model=self.model,
                max_tokens=self.max_tokens,
                system=self.system,
                messages=self.messages,
                output_config={"effort": "medium"},
                # Opt in to server-side fallback: if a safety classifier declines the request,
                # the API re-runs it on the model Anthropic recommends instead of refusing.
                betas=[FALLBACK_BETA],
                extra_body={"fallbacks": "default"},
            ) as stream:
                for text in stream.text_stream:
                    if on_text:
                        on_text(text)
                final = stream.get_final_message()
        except BaseException:  # includes Ctrl-C mid-stream
            self.messages.pop()
            raise

        if final.stop_reason == "refusal":
            self.messages.pop()
            details = final.stop_details
            raise Refusal(details.category if details else None)

        # Keep the full content (not just the text) so thinking blocks round-trip unchanged.
        self.messages.append({"role": "assistant", "content": final.content})
        return "".join(block.text for block in final.content if block.type == "text")


def main():
    bot = Chatbot()
    print("Chat with Claude (%s). Commands: /reset, /quit" % bot.model)
    while True:
        try:
            line = input("\nyou> ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if not line:
            continue
        if line in ("/quit", "/exit"):
            break
        if line == "/reset":
            bot.reset()
            print("(conversation cleared)")
            continue

        print("claude> ", end="", flush=True)
        try:
            bot.ask(line, on_text=lambda t: print(t, end="", flush=True))
            print()
        except KeyboardInterrupt:
            print("\n[interrupted]")
        except Refusal as e:
            print("\n[declined: %s]" % (e.category or "no reason given"))
        except TypeError as e:
            # With no credentials at all the SDK raises a bare TypeError on the first request.
            if "authentication method" not in str(e):
                raise
            print("\nNo credentials found. Set ANTHROPIC_API_KEY (or run `ant auth login`).")
        except anthropic.AuthenticationError:
            print("\nAuthentication failed. Check ANTHROPIC_API_KEY (or run `ant auth login`).")
        except anthropic.RateLimitError:
            print("\nRate limited. Wait a moment and try again.")
        except anthropic.APIStatusError as e:
            print("\nAPI error (%s): %s" % (e.status_code, e.message))
        except anthropic.APIConnectionError:
            print("\nCouldn't reach the API. Check your connection.")


if __name__ == "__main__":
    main()
