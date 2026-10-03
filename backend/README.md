# AgentCart AI - Python Render Backend

This is the Python backend service for **AgentCart AI**, orchestrating **Gemini 2.5 Flash Vision**, **Channel3 Inventory Mapping**, and **PayPal Checkout REST APIs**.

## Environment Variables Required on Render

Set the following environment variables in your Render Dashboard:
- `GEMINI_API_KEY`: Your Google Gemini API Key for multimodal product recognition
- `CHANNEL3_API_KEY`: Channel3 API token for real-time inventory and merchant mapping
- `PAYPAL_CLIENT_ID`: PayPal Developer Sandbox Client ID
- `PAYPAL_SECRET`: PayPal Developer Sandbox Secret Key
- `PAYPAL_BASE_URL`: `https://api-m.sandbox.paypal.com` (Default)
- `ZAPIER_WEBHOOK_URL`: (Optional) Zapier Webhook URL to track purchases in real time

## Deployment on Render

1. Create a **New Web Service** on [Render](https://render.com).
2. Connect your repository and set the Root Directory to `backend` (or deploy root).
3. Set **Runtime** to `Python 3`.
4. Set **Build Command**: `pip install -r requirements.txt`
5. Set **Start Command**: `gunicorn app:app`
6. Add the environment variables above.
7. Copy your assigned `.onrender.com` URL and enter it in the **Settings** panel inside the **AgentCart AI** Android mobile app.
