# AgentCart AI 🛒⚡
> **Autonomous Commerce Agent: Visual & Text Product Detection to 1-Click PayPal Sandbox Checkout & Automation**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Jetpack%20Compose-green.svg)](https://developer.android.com/jetpack/compose)
[![AI Engine](https://img.shields.io/badge/AI-Google%20Gemini%20Flash-orange.svg)](https://ai.google.dev/)
[![Payments](https://img.shields.io/badge/Payments-PayPal%20Sandbox%20v2-003087.svg)](https://developer.paypal.com/)

AgentCart AI is an autonomous, AI-driven mobile shopping and checkout agent. It eliminates traditional e-commerce friction (search bars, checkout forms, card entry, shipping addresses) by translating real-world visual items or natural-language product queries into verified commercial orders, backed by biometric authorization, real-time PayPal sandbox transactions, and automated Zapier workflow triggers.

---

## 🚀 Key Features

* **Multi-modal Product Recognition (Google Gemini Flash)**:
  * **Visual Mode**: Real-time camera viewfinder or gallery photo snapshot processed through Gemini Flash Vision to recognize brands, models, and commercial attributes.
  * **Direct Details Mode**: Enter real product titles and prices directly, or ask Gemini to automatically parse and estimate retail prices. Zero fake or dummy data.
* **Biometric 1-Click Authorization (`BiometricPrompt`)**:
  * Crypto-backed fingerprint/face authentication before payment dispatch, with graceful emulator fallback.
* **Live 2-Step PayPal Sandbox Checkout**:
  * **Order Creation (`POST /api/execute-paypal`)**: Generates an official `v2/checkout/orders` with `CAPTURE` intent and custom deep links.
  * **Deep Link Approval Return**: Buyer completes authorization via browser and returns seamlessly via `agentcart://paypal/return?token={orderId}`.
  * **Immediate Capture (`POST /api/capture-paypal`)**: Captures authorized funds and generates an official PayPal transaction capture ID.
* **Channel3 Universal Commerce & Merchant Inventory**:
  * Resolves and routes live merchant inventory, partner store offers, and pricing via the Channel3 Universal Product Catalog API (`trychannel3.com`).
* **Automated Post-Purchase Webhook (Zapier)**:
  * Dispatches automated purchase webhooks for CRM logging, Slack notifications, or email receipt generation upon order capture.
* **Persistent Transaction Ledger**:
  * Local, real-time ledger tracking completed orders with transaction IDs, timestamps, and payment statuses.

---

## 🛠️ Architecture & Tools Used

| Tool / Technology | Role in Project |
| :--- | :--- |
| **PayPal Sandbox REST v2 API** | Primary payment rails: OAuth2 bearer token negotiation, `v2/checkout/orders` order creation, and capture lifecycle. |
| **Channel3 API (`trychannel3.com`)** | Official commerce sponsor tool: Universal product catalog search and real-time merchant inventory resolution. |
| **Google Gemini Flash API** | Multi-modal vision and natural-language comprehension for extracting brand, model, and pricing. |
| **Android Jetpack Compose & Material 3** | Declarative mobile user interface, dynamic dark/light themes, animations, edge-to-edge window insets. |
| **Android CameraX** | Low-latency hardware camera preview and high-resolution JPEG capture for visual recognition. |
| **Androidx Biometric** | Hardware-level biometric prompt authentication for secure 1-click payment confirmation. |
| **Retrofit 2 & OkHttp 3** | Type-safe REST API client communicating with backend order and inference endpoints. |
| **Python & Flask** | Backend microservice orchestrating Gemini inference, Channel3 inventory lookups, PayPal ledger authorization, and webhooks. |
| **Zapier Webhooks** | Post-checkout automated pipeline triggering receipts, CRM updates, and fulfillment notifications. |

---

## 📋 Prerequisites & Setup Instructions

### 1. Android Client (Mobile App)
* **Requirements**: Android Studio Ladybug / Meerkat (or JDK 17+ with Android SDK 35).
* **Build & Run**:
  ```bash
  # From project root:
  gradle assembleDebug
  ```
* Open the root directory in Android Studio, select an emulator or physical device (Android 10+), and click **Run (Shift + F10)**.

### 2. Python Backend (`backend/`)
* **Requirements**: Python 3.10+
* **Installation**:
  ```bash
  cd backend
  pip install -r requirements.txt
  ```
* **Environment Variables** (create `backend/.env`):
  ```env
  GEMINI_API_KEY=your_personal_gemini_api_key_here
  PAYPAL_CLIENT_ID=your_paypal_sandbox_client_id
  PAYPAL_SECRET=your_paypal_sandbox_client_secret
  PAYPAL_BASE_URL=https://api-m.sandbox.paypal.com
  ZAPIER_WEBHOOK_URL=https://hooks.zapier.com/hooks/catch/...
  PORT=5000
  ```
* **Run Server**:
  ```bash
  python app.py
  ```

---

## 🔒 Security & Privacy

* API keys and client secrets are stored server-side or in private environment configuration.
* All PayPal API calls run against the official Sandbox environment (`api-m.sandbox.paypal.com`).
* Deep linking is restricted to the verified `agentcart://paypal` scheme.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
