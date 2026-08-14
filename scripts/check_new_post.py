"""
Checks the Blogger RSS feed for a new post. If found, sends a push
notification (title + thumbnail + caption) to everyone subscribed to the
"new_posts" FCM topic, and remembers the post so it isn't sent twice.

Runs on a schedule via GitHub Actions (.github/workflows/notify.yml).
"""

import os
import re
import json
import feedparser
import firebase_admin
from firebase_admin import credentials, messaging

BLOGGER_RSS_URL = "https://rnexflix.top/feeds/posts/default?alt=rss"
LAST_POST_FILE = "scripts/last_post.txt"


def get_thumbnail(entry):
    # Blogger includes a <media:thumbnail> most of the time
    if "media_thumbnail" in entry and entry.media_thumbnail:
        return entry.media_thumbnail[0].get("url")
    # Fallback: pull the first <img> out of the post's HTML content
    html = ""
    if "content" in entry and entry.content:
        html = entry.content[0].value
    elif "summary" in entry:
        html = entry.summary
    match = re.search(r'<img[^>]+src="([^">]+)"', html)
    return match.group(1) if match else None


def clean_caption(entry):
    html = entry.summary if "summary" in entry else ""
    text = re.sub("<[^<]+?>", "", html).strip()
    return (text[:120] + "...") if len(text) > 120 else text


def main():
    feed = feedparser.parse(BLOGGER_RSS_URL)
    if not feed.entries:
        print("No entries found in feed.")
        return

    latest = feed.entries[0]
    latest_id = latest.get("id", latest.link)

    last_seen = ""
    if os.path.exists(LAST_POST_FILE):
        with open(LAST_POST_FILE, "r") as f:
            last_seen = f.read().strip()

    if latest_id == last_seen:
        print("No new post.")
        return

    # Init Firebase Admin using the service account secret
    cred_json = os.environ["FIREBASE_SERVICE_ACCOUNT"]
    cred = credentials.Certificate(json.loads(cred_json))
    firebase_admin.initialize_app(cred)

    title = latest.title
    body = clean_caption(latest)
    image = get_thumbnail(latest)
    link = latest.link

    notif = messaging.Notification(title=title, body=body, image=image) if image else messaging.Notification(title=title, body=body)

    message = messaging.Message(
        notification=notif,
        data={
            "title": title,
            "body": body,
            "image": image or "",
            "url": link,
        },
        topic="new_posts",
        android=messaging.AndroidConfig(priority="high"),
    )

    response = messaging.send(message)
    print("Notification sent:", response)

    with open(LAST_POST_FILE, "w") as f:
        f.write(latest_id)


if __name__ == "__main__":
    main()
