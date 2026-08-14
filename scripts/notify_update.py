"""
Sends a push notification to everyone (topic: app_updates) telling them a
new version of the app is available, with a direct link to download it.

Run manually from the "Notify App Update" GitHub Actions workflow after you
release a new version.
"""

import os
import json
import firebase_admin
from firebase_admin import credentials, messaging


def main():
    title = os.environ.get("UPDATE_TITLE", "Update available")
    body = os.environ.get("UPDATE_BODY", "A new version of Nexflix is here. Tap to update.")
    apk_url = os.environ["UPDATE_APK_URL"]

    cred_json = os.environ["FIREBASE_SERVICE_ACCOUNT"]
    cred = credentials.Certificate(json.loads(cred_json))
    firebase_admin.initialize_app(cred)

    message = messaging.Message(
        notification=messaging.Notification(title=title, body=body),
        data={
            "type": "update",
            "title": title,
            "body": body,
            "url": apk_url,
        },
        topic="app_updates",
        android=messaging.AndroidConfig(priority="high"),
    )

    response = messaging.send(message)
    print("Update notification sent:", response)


if __name__ == "__main__":
    main()
