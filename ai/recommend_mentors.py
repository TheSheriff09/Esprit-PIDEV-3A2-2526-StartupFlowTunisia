# ai/recommend_mentors.py
import os
import sys
import json
import mysql.connector
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

DB_HOST = "127.0.0.1"
DB_PORT = 3306
DB_NAME = "hamod"
DB_USER = "root"
DB_PASS = ""

def risk_label(cnt: int) -> str:
    if cnt >= 7:
        return "HIGH"
    if cnt >= 3:
        return "MID"
    return "LOW"

need_text = " ".join(sys.argv[1:]).strip()
if not need_text:
    need_text = "business startup funding marketing pitch deck"

cnx = mysql.connector.connect(
    host=DB_HOST,
    port=DB_PORT,
    user=DB_USER,
    password=DB_PASS,
    database=DB_NAME
)
cur = cnx.cursor(dictionary=True)

cur.execute("""
            SELECT id, full_name, mentor_expertise
            FROM users
            WHERE role='MENTOR' AND status='ACTIVE'
            """)
mentors = cur.fetchall()

cur.execute("""
            SELECT targetId AS mentor_id, COUNT(*) AS cnt
            FROM reclamation
            WHERE title='USER_PROBLEM'
              AND targetId IS NOT NULL
              AND created_at >= NOW() - INTERVAL 90 DAY
            GROUP BY targetId
            """)
rc_map = {row["mentor_id"]: int(row["cnt"]) for row in cur.fetchall()}

cur.close()
cnx.close()

texts = []
ids = []
for m in mentors:
    exp = (m.get("mentor_expertise") or "").strip()
    texts.append(exp if exp else "no expertise provided")
    ids.append(m["id"])

vectorizer = TfidfVectorizer(stop_words="english")
X = vectorizer.fit_transform(texts + [need_text])
mentor_vecs = X[:-1]
query_vec = X[-1]

sims = cosine_similarity(mentor_vecs, query_vec).reshape(-1)

# Build output
out = []
for i, m in enumerate(mentors):
    mentor_id = m["id"]
    cnt = int(rc_map.get(mentor_id, 0))
    score = float(sims[i])
    out.append({
        "id": mentor_id,
        "fullName": m["full_name"],
        "expertise": m.get("mentor_expertise") or "",
        "matchScore": round(score * 100.0, 2),  # %
        "reclamations90d": cnt,
        "risk": risk_label(cnt)
    })

# Sort by: highest match, then lowest complaints
out.sort(key=lambda x: (-x["matchScore"], x["reclamations90d"]))

best_count = 0
for item in out:
    if best_count < 3 and item["risk"] != "HIGH":
        item["best"] = True
        best_count += 1
    else:
        item["best"] = False

print(json.dumps(out, ensure_ascii=False))