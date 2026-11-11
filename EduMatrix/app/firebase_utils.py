import firebase_admin
from firebase_admin import credentials, messaging
from typing import List, Optional
import os
import logging

logger = logging.getLogger(__name__)

# Initialize Firebase Admin SDK
try:
    # Check if a service account file exists
    if os.path.exists("serviceAccountKey.json"):
        cred = credentials.Certificate("serviceAccountKey.json")
        firebase_admin.initialize_app(cred)
        logger.info("Firebase Admin SDK initialized with serviceAccountKey.json")
    else:
        logger.warning("serviceAccountKey.json not found. Firebase features will be disabled.")
        # Alternatively, you can initialize with default credentials if running on GCP
        # firebase_admin.initialize_app()
except ValueError:
    # App already initialized
    pass
except Exception as e:
    logger.error(f"Failed to initialize Firebase Admin SDK: {e}")

def send_topic_notification(topic: str, title: str, body: str, data: dict = None, image_url: str = None):
    try:
        if not firebase_admin._apps:
            logger.warning("Firebase not initialized. Skipping notification.")
            return

        notification = messaging.Notification(
            title=title,
            body=body,
            image=image_url if image_url else None
        )

        message = messaging.Message(
            notification=notification,
            data=data,
            topic=topic,
        )
        response = messaging.send(message)
        logger.info(f"Successfully sent message to topic {topic}: {response}")
        return response
    except Exception as e:
        logger.error(f"Error sending message to topic {topic}: {e}")
        return None

def send_multicast_notification(tokens: List[str], title: str, body: str, data: dict = None, image_url: str = None, is_emergency: bool = False):
    try:
        if not firebase_admin._apps:
           logger.warning("Firebase not initialized. Skipping notification.")
           return

        if not tokens:
            logger.warning("No tokens provided for multicast notification.")
            return

        # Prepare payload
        if data is None:
            data = {}
        
        # Ensure title and body are in data for data-only messages
        data["title"] = title
        data["body"] = body
        if image_url:
            data["image_url"] = image_url

        # Configure the Notification Payload (Trigger system tray/popup)
        notification = messaging.Notification(
            title=title,
            body=body,
            image=image_url if image_url else None
        )

        # Configure Android Notification settings for Popup/Heads-up
        android_notification = messaging.AndroidNotification(
            channel_id="auraface_alerts_channel_v2",
            priority="high",
            default_sound=True,
            default_vibrate_timings=True,
            visibility="public"
        )

        # Configure Android Message Delivery Priority
        android_config = messaging.AndroidConfig(
            priority="high",
            notification=android_notification
        )
        
        # Construct and send message
        message = messaging.MulticastMessage(
            notification=notification, 
            data=data,
            tokens=tokens,
            android=android_config
        )
            
        response = messaging.send_each_for_multicast(message)
        logger.info(f"Successfully sent multicast message (Emergency={is_emergency}): {response.success_count} success, {response.failure_count} failure")
        return response
    except Exception as e:
        logger.error(f"Error sending multicast message: {e}")
        return None
