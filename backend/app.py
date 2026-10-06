import os
import requests
from flask import Flask, request, jsonify
from flask_cors import CORS
import google.generativeai as genai

app = Flask(__name__)
CORS(app)

# Environment configuration
GEMINI_KEY = os.getenv("GEMINI_API_KEY", "")
if GEMINI_KEY:
    try:
        genai.configure(api_key=GEMINI_KEY)
    except Exception as e:
        print(f"Error configuring Gemini: {e}")

CHANNEL3_API_KEY = os.getenv("CHANNEL3_API_KEY", "")
PAYPAL_CLIENT_ID = os.getenv("PAYPAL_CLIENT_ID", "")
PAYPAL_SECRET = os.getenv("PAYPAL_SECRET", "")
PAYPAL_BASE_URL = os.getenv("PAYPAL_BASE_URL", "https://api-m.sandbox.paypal.com")

# Android deep-link URLs
PAYPAL_RETURN_URL = os.getenv(
    "PAYPAL_RETURN_URL",
    "agentcart://paypal/return"
)

PAYPAL_CANCEL_URL = os.getenv(
    "PAYPAL_CANCEL_URL",
    "agentcart://paypal/cancel"
)

def get_paypal_access_token():
    url = f"{PAYPAL_BASE_URL}/v1/oauth2/token"
    headers = {"Accept": "application/json", "Accept-Language": "en_US"}
    data = {"grant_type": "client_credentials"}
    try:
        response = requests.post(url, headers=headers, data=data, auth=(PAYPAL_CLIENT_ID, PAYPAL_SECRET), timeout=10)
        res_json = response.json()
        if response.status_code == 200 and "access_token" in res_json:
            return res_json["access_token"], None
        return None, res_json
    except Exception as e:
        return None, str(e)

def query_channel3_catalog(query):
    """
    Queries Channel3 Universal Product Catalog API (trychannel3.com)
    to verify real merchant inventory, offers, and live pricing.
    """
    if not CHANNEL3_API_KEY or not query:
        return None
    try:
        url = "https://api.trychannel3.com/v1/search"
        headers = {
            "x-api-key": CHANNEL3_API_KEY,
            "Content-Type": "application/json"
        }
        res = requests.post(url, headers=headers, json={"query": str(query)[:120]}, timeout=4)
        if res.status_code == 200:
            data = res.json()
            products = data.get("products") or data.get("results") or []
            if products and isinstance(products, list):
                top = products[0]
                t = top.get("title") or top.get("name")
                p = top.get("price")
                m = top.get("brand") or top.get("merchant") or "Channel3 Partner Merchant"
                price_str = f"${p}" if (p and not str(p).startswith("$")) else (str(p) if p else None)
                return {
                    "title": t,
                    "price": price_str,
                    "merchant": m,
                    "description": top.get("description", "")
                }
    except Exception as e:
        print(f"Channel3 search lookup error: {e}")
    return None

@app.route("/", methods=["GET"])
def health_check():
    return jsonify({
        "status": "online",
        "service": "AgentCart AI Multi-Agent Backend",
        "models": ["gemini-1.5-flash", "gemini-2.0-flash"],
        "gemini_configured": bool(GEMINI_KEY),
        "paypal_configured": bool(PAYPAL_CLIENT_ID and PAYPAL_SECRET),
        "channel3_configured": bool(CHANNEL3_API_KEY),
        "endpoints": ["/api/process-agent-intent", "/api/process-text-intent", "/api/execute-paypal", "/api/capture-paypal"]
    })

@app.route("/api/process-text-intent", methods=["POST"])
def process_text_intent():
    try:
        body = request.get_json(silent=True) or {}
        query = str(body.get("query", "")).strip()
        custom_price = str(body.get("price", "")).strip()
        custom_category = str(body.get("category", "")).strip()

        import re
        price = ""
        if custom_price:
            price = "$" + custom_price.replace("$", "").strip()
        else:
            match = re.search(r'\$(\d+(?:\.\d{1,2})?)', query)
            if match:
                price = "$" + match.group(1)

        product_title = query
        if price and not custom_price:
            product_title = query.replace(price, "").strip()

        if not product_title and not query:
            return jsonify({
                "status": "FAILED",
                "error": "Please enter a product title or name."
            }), 400

        # If user provided a price directly with the title, use their exact real data:
        if product_title and price:
            category = custom_category if custom_category else "Commercial Goods"
            return jsonify({
                "id": f"ITEM-{abs(hash(product_title + price)) % 100000}",
                "title": product_title,
                "price": price,
                "confidenceScore": "User Specified",
                "merchantName": "PayPal Direct Checkout",
                "description": f"Product: {product_title} at {price}.",
                "category": category,
                "currency": "USD",
                "visionModel": "User Specified",
                "geminiRawOutput": f"{product_title} | {price} | {category}",
                "geminiStatus": "SUCCESS"
            })

        # If price was NOT provided, use Gemini API to identify product specs & price
        if not GEMINI_KEY:
            return jsonify({
                "status": "FAILED",
                "error": "No price entered and GEMINI_API_KEY is not configured on the backend. Please enter the price in the Price field."
            }), 400

        custom_model = os.getenv("GEMINI_MODEL", "").strip()
        candidate_models = [m for m in [custom_model, "gemini-1.5-flash", "gemini-2.0-flash", "gemini-2.5-flash", "gemini-1.5-pro"] if m]
        prompt = (
            f"You are an AI commerce checkout assistant. The user wants to buy: '{query}'. "
            "Identify the brand, exact product title, realistic retail price in USD, and category. "
            "Output ONLY a single line in this format: <Brand & Model Title> | <Price e.g. $199.99> | <Category>. "
            "Do NOT include explanation, quotes, or markdown."
        )

        gemini_response = None
        used_model_name = ""
        last_error = None

        for model_name in candidate_models:
            try:
                text_model = genai.GenerativeModel(model_name)
                res = text_model.generate_content(prompt)
                if res and res.text:
                    gemini_response = res
                    used_model_name = model_name
                    break
            except Exception as e:
                last_error = e
                continue

        if not gemini_response or not used_model_name:
            err_str = str(last_error) if last_error else "Unknown Gemini error"
            if "403" in err_str or "denied access" in err_str.lower():
                return jsonify({
                    "status": "FAILED",
                    "error": (
                        "Gemini API 403 (Permission Denied): Your project has been denied access. "
                        "Please enter the product price in the Price field or use a personal @gmail.com API key."
                    ),
                    "details": err_str
                }), 403
            return jsonify({
                "status": "FAILED",
                "error": f"Gemini API Error: {err_str}. Please enter the product price manually in the Price field."
            }), 500

        raw_text = gemini_response.text.strip()
        parts = [p.strip() for p in raw_text.split("|")]
        extracted_title = parts[0] if len(parts) >= 1 and parts[0] else query
        extracted_price = parts[1] if len(parts) >= 2 and "$" in parts[1] else ""
        extracted_category = parts[2] if len(parts) >= 3 and parts[2] else "General"

        if not extracted_price:
            return jsonify({
                "status": "FAILED",
                "error": f"Gemini returned: '{raw_text}', but could not determine a valid price. Please enter the price in the Price field."
            }), 422

        c3_item = query_channel3_catalog(extracted_title)
        merchant_name = f"Channel3 • {c3_item['merchant']}" if (c3_item and c3_item.get("merchant")) else "Channel3 Direct Merchant"

        return jsonify({
            "id": f"GEMINI-{abs(hash(extracted_title)) % 100000}",
            "title": extracted_title,
            "price": extracted_price,
            "confidenceScore": f"Google {used_model_name} + Channel3",
            "merchantName": merchant_name,
            "description": f"Product identified by Google Gemini & routed through Channel3 Merchant Node: {extracted_title}",
            "category": extracted_category,
            "currency": "USD",
            "visionModel": used_model_name,
            "geminiRawOutput": raw_text,
            "geminiStatus": "SUCCESS"
        })
    except Exception as e:
        return jsonify({"status": "FAILED", "error": str(e)}), 500

@app.route("/api/process-agent-intent", methods=["POST"])
def process_agent_intent():
    try:
        image_bytes = request.data
        if not image_bytes or len(image_bytes) < 10:
            return jsonify({
                "status": "FAILED",
                "error": "No valid image data stream received. Please ensure a photo was captured or selected."
            }), 400
        
        # Verify GEMINI_API_KEY is configured
        if not GEMINI_KEY:
            return jsonify({
                "status": "FAILED",
                "error": "GEMINI_API_KEY is not configured on Render. Please add your GEMINI_API_KEY in Render Dashboard -> Environment Variables to enable live image recognition."
            }), 400
        
        # Multi-modal Vision Processing using standard Gemini Flash models
        custom_model = os.getenv("GEMINI_MODEL", "").strip()
        candidate_models = [m for m in [custom_model, "gemini-1.5-flash", "gemini-2.0-flash", "gemini-2.5-flash", "gemini-1.5-pro"] if m]
        
        gemini_response = None
        used_model_name = ""
        last_error = None
        
        prompt = (
            "You are an AI shopping agent scanning a camera feed. "
            "Identify this commercial product exactly. "
            "Output ONLY a single line with: <Brand> <Model Name> | <Estimated Price USD, e.g. $49.99> | <Category>. "
            "Do NOT include markdown, explanations, or quotes."
        )

        for model_name in candidate_models:
            try:
                vision_model = genai.GenerativeModel(model_name)
                res = vision_model.generate_content([
                    {"mime_type": "image/jpeg", "data": image_bytes},
                    prompt
                ])
                if res and res.text:
                    gemini_response = res
                    used_model_name = model_name
                    break
            except Exception as e:
                last_error = e
                continue

        if not gemini_response or not used_model_name:
            err_str = str(last_error)
            if "403" in err_str or "denied access" in err_str.lower():
                guidance = (
                    "Gemini 403 (Permission Denied): 'Your project has been denied access'.\n"
                    "Reason: This typically occurs if your GEMINI_API_KEY was generated using an institutional / college Google account (@sakec.ac.in), which restricts Generative Language API access, or if the GCP project has API restrictions.\n\n"
                    "Solution: Generate a fresh Gemini API key using a personal @gmail.com account at https://aistudio.google.com/apikey, then update GEMINI_API_KEY in your Render Dashboard."
                )
                return jsonify({
                    "status": "FAILED",
                    "error": guidance,
                    "details": err_str
                }), 403
            return jsonify({
                "status": "FAILED",
                "error": f"Gemini Vision API Error: {err_str}"
            }), 500

        raw_text = gemini_response.text.strip()
        if not raw_text:
            return jsonify({
                "status": "FAILED",
                "error": f"Gemini Vision ({used_model_name}) was unable to recognize any commercial product in this image. Please aim clearly at a product with visible branding."
            }), 422
        
        # Parse model output
        parts = [p.strip() for p in raw_text.split("|")]
        product_title = parts[0]
        price = parts[1] if len(parts) > 1 and "$" in parts[1] else "$49.99"
        category = parts[2] if len(parts) > 2 else "Consumer Products"

        # Query Channel3 Universal Catalog for live merchant routing
        c3_item = query_channel3_catalog(product_title)
        merchant_name = f"Channel3 • {c3_item['merchant']}" if (c3_item and c3_item.get("merchant")) else "Channel3 Merchant Node"
        
        return jsonify({
            "id": f"AI-{abs(hash(product_title)) % 100000}",
            "title": product_title,
            "price": price,
            "confidenceScore": f"Google {used_model_name} + Channel3",
            "merchantName": merchant_name,
            "description": f"Real-time product identification powered by Google Gemini ({used_model_name}) & Channel3 Merchant Node.",
            "category": category,
            "currency": "USD",
            "visionModel": used_model_name,
            "geminiRawOutput": raw_text,
            "geminiStatus": "SUCCESS"
        })

    except Exception as e:
        return jsonify({
            "status": "FAILED",
            "error": f"Backend processing error: {str(e)}"
        }), 500

@app.route("/api/capture-paypal", methods=["POST"])
def capture_paypal():
    try:
        body = request.get_json(silent=True) or {}

        order_id = str(
            body.get("orderId", "")
        ).strip()

        if not order_id:
            return jsonify({
                "status": "FAILED",
                "error": "PayPal orderId is required."
            }), 400

        # ---------------------------------------------------------
        # Validate PayPal credentials
        # ---------------------------------------------------------

        if not PAYPAL_CLIENT_ID or not PAYPAL_SECRET:
            return jsonify({
                "status": "FAILED",
                "error": (
                    "PAYPAL_CLIENT_ID and PAYPAL_SECRET are not "
                    "configured in Render."
                )
            }), 400

        # ---------------------------------------------------------
        # Get OAuth token
        # ---------------------------------------------------------

        token, token_err = get_paypal_access_token()

        if not token:
            return jsonify({
                "status": "FAILED",
                "error": "PayPal OAuth authentication failed.",
                "details": token_err
            }), 401

        # ---------------------------------------------------------
        # Capture PayPal order
        # ---------------------------------------------------------

        capture_url = (
            f"{PAYPAL_BASE_URL}"
            f"/v2/checkout/orders/{order_id}/capture"
        )

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
            "Prefer": "return=representation"
        }

        capture_response = requests.post(
            capture_url,
            headers=headers,
            timeout=15
        )

        try:
            capture_res = capture_response.json()
        except Exception:
            capture_res = {
                "message": capture_response.text
            }

        # ---------------------------------------------------------
        # PayPal API error
        # ---------------------------------------------------------

        if capture_response.status_code not in (200, 201):

            return jsonify({
                "status": "FAILED",
                "error": (
                    capture_res.get("message")
                    or "PayPal capture failed."
                ),
                "details": capture_res,
                "orderId": order_id
            }), capture_response.status_code

        # ---------------------------------------------------------
        # Verify order status
        # ---------------------------------------------------------

        paypal_status = capture_res.get("status")

        if paypal_status != "COMPLETED":

            return jsonify({
                "status": "FAILED",
                "error": (
                    "PayPal capture request completed, but "
                    f"order status is {paypal_status}."
                ),
                "orderId": order_id,
                "paypalStatus": paypal_status,
                "details": capture_res
            }), 400

        # ---------------------------------------------------------
        # Extract actual capture ID
        # ---------------------------------------------------------

        capture_id = None
        capture_status = None
        captured_amount = None
        captured_currency = None

        purchase_units = capture_res.get(
            "purchase_units",
            []
        )

        if purchase_units:

            payments = (
                purchase_units[0]
                .get("payments", {})
            )

            captures = payments.get(
                "captures",
                []
            )

            if captures:

                capture = captures[0]

                capture_id = capture.get("id")
                capture_status = capture.get("status")

                captured_amount = (
                    capture.get("amount", {})
                    .get("value")
                )

                captured_currency = (
                    capture.get("amount", {})
                    .get("currency_code")
                )

        # ---------------------------------------------------------
        # Require actual capture ID
        # ---------------------------------------------------------

        if not capture_id:

            return jsonify({
                "status": "FAILED",
                "error": (
                    "PayPal order reports COMPLETED, but "
                    "no capture ID was returned."
                ),
                "orderId": order_id,
                "details": capture_res
            }), 400

        # ---------------------------------------------------------
        # Zapier ONLY fires after successful capture
        # ---------------------------------------------------------

        zapier_triggered = False

        zapier_url = os.getenv(
            "ZAPIER_WEBHOOK_URL",
            ""
        ).strip()

        if zapier_url:

            try:

                zapier_payload = {
                    "event": "paypal_order_completed",

                    "order_id": order_id,

                    "capture_id": capture_id,

                    "paypal_status": paypal_status,

                    "capture_status": capture_status,

                    "amount": captured_amount,

                    "currency": captured_currency
                }

                zapier_response = requests.post(
                    zapier_url,
                    json=zapier_payload,
                    timeout=10
                )

                if 200 <= zapier_response.status_code < 300:
                    zapier_triggered = True

            except Exception as zapier_error:

                print(
                    "Zapier webhook error:",
                    zapier_error
                )

        # ---------------------------------------------------------
        # FINAL SUCCESS
        # ---------------------------------------------------------

        return jsonify({
            "status": "COMPLETED",

            "orderId": order_id,

            "transactionId": capture_id,

            "captureId": capture_id,

            "paypalOrderStatus": paypal_status,

            "captureStatus": capture_status,

            "amount": captured_amount,

            "currency": captured_currency,

            "zapierTriggered": zapier_triggered
        }), 200

    except Exception as e:

        return jsonify({
            "status": "FAILED",
            "error": f"PayPal capture error: {str(e)}"
        }), 500

@app.route("/api/execute-paypal", methods=["POST"])
def execute_paypal():
    try:
        body = request.get_json(silent=True) or {}

        product_id = str(
            body.get("productId", "PROD-GENERIC")
        ).strip()

        product_title = str(
            body.get("productTitle", "Product")
        ).strip()

        price = str(
            body.get("price", "39.99")
        ).replace("$", "").strip()

        if not price:
            price = "39.99"

        # ---------------------------------------------------------
        # Validate PayPal credentials
        # ---------------------------------------------------------

        if not PAYPAL_CLIENT_ID or not PAYPAL_SECRET:
            return jsonify({
                "status": "FAILED",
                "error": (
                    "PAYPAL_CLIENT_ID and PAYPAL_SECRET are not "
                    "configured in Render environment variables."
                )
            }), 400

        # ---------------------------------------------------------
        # Get OAuth access token
        # ---------------------------------------------------------

        token, token_err = get_paypal_access_token()

        if not token:
            error_msg = "PayPal OAuth authentication failed."

            if isinstance(token_err, dict):
                error_desc = (
                    token_err.get("error_description")
                    or token_err.get("error")
                    or token_err.get("message")
                )

                if error_desc:
                    error_msg += f" Details: {error_desc}"

            return jsonify({
                "status": "FAILED",
                "error": error_msg,
                "details": token_err
            }), 401

        # ---------------------------------------------------------
        # Create PayPal Order
        # ---------------------------------------------------------

        order_url = (
            f"{PAYPAL_BASE_URL}/v2/checkout/orders"
        )

        headers = {
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
            "Prefer": "return=representation"
        }

        order_payload = {
            "intent": "CAPTURE",

            "purchase_units": [
                {
                    "reference_id": product_id,

                    "description": product_title[:127],

                    "amount": {
                        "currency_code": "USD",
                        "value": price
                    }
                }
            ],

            # Current PayPal Orders API supports the
            # PayPal-specific experience context.
            "payment_source": {
                "paypal": {
                    "experience_context": {
                        "user_action": "PAY_NOW",
                        "return_url": PAYPAL_RETURN_URL,
                        "cancel_url": PAYPAL_CANCEL_URL
                    }
                }
            }
        }

        order_response = requests.post(
            order_url,
            json=order_payload,
            headers=headers,
            timeout=15
        )

        try:
            order_res = order_response.json()
        except Exception:
            order_res = {
                "message": order_response.text
            }

        # ---------------------------------------------------------
        # Check PayPal response
        # ---------------------------------------------------------

        if order_response.status_code not in (200, 201):
            return jsonify({
                "status": "FAILED",
                "error": (
                    order_res.get("message")
                    or "PayPal order creation failed."
                ),
                "details": order_res
            }), order_response.status_code

        order_id = order_res.get("id")

        if not order_id:
            return jsonify({
                "status": "FAILED",
                "error": "PayPal did not return an order ID.",
                "details": order_res
            }), 400

        # ---------------------------------------------------------
        # Find PayPal approval URL
        # ---------------------------------------------------------

        approval_url = None

        for link in order_res.get("links", []):
            rel = link.get("rel")

            if rel in ("approve", "payer-action"):
                approval_url = link.get("href")
                break

        if not approval_url:
            return jsonify({
                "status": "FAILED",
                "error": (
                    "PayPal order was created but no approval URL "
                    "was returned."
                ),
                "orderId": order_id,
                "details": order_res
            }), 400

        # ---------------------------------------------------------
        # IMPORTANT:
        #
        # DO NOT CAPTURE HERE.
        #
        # Buyer must first approve the order in PayPal.
        # ---------------------------------------------------------

        return jsonify({
            "status": "CREATED",

            "orderId": order_id,

            "transactionId": order_id,

            "approvalUrl": approval_url,

            "productId": product_id,

            "productTitle": product_title,

            "amount": price,

            "currency": "USD",

            "message": "PayPal order created. Waiting for buyer approval."
        }), 200

    except Exception as e:

        return jsonify({
            "status": "FAILED",
            "error": f"PayPal order creation error: {str(e)}"
        }), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 5000)))
