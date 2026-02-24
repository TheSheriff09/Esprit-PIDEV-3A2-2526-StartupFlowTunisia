import sys
import json
from transformers import pipeline

# Models chosen to be lightweight + CPU friendly
SUGGEST_MODEL = "google/flan-t5-base"        # text2text instruction style
REWRITE_MODEL  = "google/flan-t5-base"
SENT_MODEL     = "distilbert-base-uncased-finetuned-sst-2-english"

def suggest_replies(title: str, description: str):
    gen = pipeline("text2text-generation", model=SUGGEST_MODEL)

    prompt = f"""
You are a professional customer support agent.

Generate exactly 3 different response suggestions in JSON format.

Return STRICT JSON like this:
{{
  "suggestions": [
    "reply 1",
    "reply 2",
    "reply 3"
  ]
}}

Complaint title: {title}
Complaint description: {description}
"""

    out = gen(prompt, max_new_tokens=250, do_sample=False)[0]["generated_text"]

    # Try to extract JSON safely
    try:
        start = out.find("{")
        end = out.rfind("}") + 1
        json_text = out[start:end]
        data = json.loads(json_text)
        return data
    except Exception:
        return {"suggestions": [out.strip()]}

def rewrite_reply(mode: str, text: str):
    gen = pipeline("text2text-generation", model=REWRITE_MODEL)

    instruction = {
        "polite": "Rewrite to be more polite and professional.",
        "short": "Rewrite shorter (max 50 words) without losing meaning.",
        "formal": "Rewrite in a formal tone.",
    }.get(mode, "Rewrite to be clearer and professional.")

    prompt = f"{instruction}\n\nText:\n{text}\n"
    out = gen(prompt, max_new_tokens=160, do_sample=False)[0]["generated_text"]
    return {"rewritten": out.strip(), "mode": mode}

def sentiment_check(text: str):
    clf = pipeline("sentiment-analysis", model=SENT_MODEL)  # sentiment pipeline :contentReference[oaicite:3]{index=3}
    res = clf(text)[0]
    # Example output: {'label': 'NEGATIVE', 'score': 0.999...} :contentReference[oaicite:4]{index=4}
    return {"label": res["label"], "score": float(res["score"])}

def main():
    # Usage examples:
    # python ai/response_ai.py suggest "<title>" "<desc>"
    # python ai/response_ai.py rewrite polite "<text>"
    # python ai/response_ai.py sentiment "<text>"

    if len(sys.argv) < 3:
        print(json.dumps({"error": "Missing args"}))
        return

    cmd = sys.argv[1].lower()

    try:
        if cmd == "suggest":
            if len(sys.argv) < 4:
                print(json.dumps({"error": "Missing args: title, description"}))
                return
            title = sys.argv[2]
            desc = sys.argv[3]
            print(json.dumps(suggest_replies(title, desc), ensure_ascii=False))

        elif cmd == "rewrite":
            if len(sys.argv) < 4:
                print(json.dumps({"error": "Missing args: mode, text"}))
                return
            mode = sys.argv[2]
            text = sys.argv[3]
            print(json.dumps(rewrite_reply(mode, text), ensure_ascii=False))

        elif cmd == "sentiment":
            text = sys.argv[2]
            print(json.dumps(sentiment_check(text), ensure_ascii=False))

        else:
            print(json.dumps({"error": f"Unknown command: {cmd}"}))

    except Exception as e:
        print(json.dumps({"error": str(e)}))

if __name__ == "__main__":
    main()