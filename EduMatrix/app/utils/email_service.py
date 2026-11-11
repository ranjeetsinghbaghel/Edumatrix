import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import os
import logging

# Configure Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Load Environment Variables (Ensure dotenv is loaded in main.py)
SMTP_SERVER = os.getenv("SMTP_SERVER", "smtp.gmail.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USERNAME = os.getenv("SMTP_USERNAME") 
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD") 
SENDER_EMAIL = os.getenv("SENDER_EMAIL", SMTP_USERNAME)

def send_attendance_email(student_email: str, student_name: str, subject_name: str, date_str: str, time_str: str, status: str = "Present", guardian_email: str = None):
    """
    Sends an attendance confirmation email to the student and guardian.
    Intended to be run as a BackgroundTask.
    """
    recipients = []
    if student_email: recipients.append(student_email)
    if guardian_email: recipients.append(guardian_email)
    
    if not recipients:
        logger.warning(f"⚠️ Skipping email for {student_name}: No email address provided.")
        return

    if not SMTP_USERNAME or not SMTP_PASSWORD:
        logger.warning(f"⚠️ Skipping email for {student_name}: SMTP credentials not configured in .env")
        return

    try:
        subject = f"Attendance Marked: {subject_name}"
        
        body = f"""
        <html>
        <head>
            <style>
                body {{ font-family: Arial, sans-serif; color: #333; }}
                .container {{ padding: 20px; border: 1px solid #ddd; border-radius: 8px; max-width: 600px; }}
                .header {{ color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }}
                .details {{ margin-top: 15px; }}
                .footer {{ margin-top: 20px; font-size: 12px; color: #777; }}
            </style>
        </head>
        <body>
            <div class="container">
                <h2 class="header">EduMatrix Attendance Update</h2>
                <p>Hi,</p>
                <p>Attendance for <strong>{student_name}</strong> has been successfully recorded.</p>
                
                <div class="details">
                    <p><strong>Subject:</strong> {subject_name}</p>
                    <p><strong>Date:</strong> {date_str}</p>
                    <p><strong>Time:</strong> {time_str}</p>
                    <p><strong>Status:</strong> <span style="color: green; font-weight: bold;">{status}</span></p>
                </div>
                
                <p class="footer">This is an automated message. Please do not reply directly to this email.</p>
            </div>
        </body>
        </html>
        """

        msg = MIMEMultipart()
        msg['From'] = f"EduMatrix System <{SENDER_EMAIL}>"
        msg['To'] = ", ".join(recipients)
        msg['Subject'] = subject
        msg.attach(MIMEText(body, 'html'))

        with smtplib.SMTP(SMTP_SERVER, SMTP_PORT) as server:
            server.set_debuglevel(0) # Set to 1 for debug output
            server.starttls()
            server.login(SMTP_USERNAME, SMTP_PASSWORD)
            server.send_message(msg)
            
        logger.info(f"📧 mail sent successfully to {recipients}")

    except smtplib.SMTPAuthenticationError as e:
        logger.error(f"❌ Authentication Failed: {e}")
        logger.error("💡 HINT: If using Gmail, you MUST use an 'App Password', not your login password.")
        logger.error("   1. Enable 2-Step Verification on your Google Account.")
        logger.error("   2. Go to https://myaccount.google.com/apppasswords")
        logger.error("   3. Generate a new App Password and update SMTP_PASSWORD in your .env file.")

    except Exception as e:
        logger.error(f"❌ Failed to send email to {recipients}: {e}")
