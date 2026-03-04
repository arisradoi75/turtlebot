# Robot Security Dashboard - Frontend

A real-time monitoring and control interface built with **React** for a TurtleBot-based security system. This application provides a live bridge between the robot's hardware telemetry and the end-user, featuring secure authentication and administrative controls.



## 🚀 Key Features

* **Real-time Telemetry:** Live updates for robot status, battery level, and spatial coordinates $(X, Y)$ using WebSockets (STOMP over SockJS).
* **Live Incident Alerts:** Immediate notification stream for security alerts, including Base64-rendered snapshots captured by the robot.
* **Hybrid Data Sourcing:** * **Live:** WebSocket connection for "hot" data.
    * **Historical:** REST API integration to fetch the last known state and alert history from the database upon login.
* **Role-Based Access Control (RBAC):**
    * **USER:** View-only access to telemetry and alerts.
    * **ADMIN:** Access to the **Command Control Panel** (Start, Stop, Dock).
* **JWT Security:** Secure authentication flow with token decoding to determine user permissions.

## 🛠️ Tech Stack

* **Core:** React (Vite)
* **Communication:** * **Axios:** For RESTful API calls with interceptors for automatic JWT injection.
    * **SockJS & StompJS:** For full-duplex WebSocket communication.
* **Security:** `jwt-decode` for client-side role verification.
* **Routing:** React Router v6.

## 📋 Prerequisites

Before running the dashboard, ensure you have:
* [Node.js](https://nodejs.org/) (v16.x or newer)
* A running instance of the **Spring Boot Backend** (configured for port `8080`).

## ⚙️ Installation & Setup

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/your-username/robot-dashboard.git](https://github.com/your-username/robot-dashboard.git)
    cd robot-dashboard
    ```

2.  **Install dependencies:**
    ```bash
    npm install
    ```

3.  **Configure API URL:**
    Check `src/services/api.js` to ensure the `API_URL` matches your backend:
    ```javascript
    const API_URL = 'http://localhost:8080/api';
    ```

4.  **Start the development server:**
    ```bash
    npm run dev
    ```

## 🔌 API & Connection Map

| Type | Path | Purpose |
| :--- | :--- | :--- |
| **REST (POST)** | `/v1/auth/login` | User authentication |
| **REST (GET)** | `/robot/latest-telemetry` | Fetch last state from DB |
| **REST (POST)** | `/admin/{command}` | Admin-only robot triggers |
| **WS Connection** | `/ws-robot` | Handshake for WebSockets |
| **WS Topic** | `/topic/telemetry` | Incoming live robot data |
| **WS Topic** | `/topic/alerts` | Incoming security alerts |



## 📂 Project Structure

* `src/pages/Dashboard.jsx`: The main hub managing WebSocket lifecycles and historical data fetches.
* `src/services/api.js`: Centralized Axios config with request interceptors for the `Authorization: Bearer <token>` header.
* `src/main.jsx`: Includes global polyfills required for `stompjs` compatibility in modern browser environments.

## 🛡️ Security Implementation Note

The frontend uses a secure request interceptor. Once a user logs in, the `accessToken` is stored in `localStorage`. Every subsequent request automatically carries the JWT in the headers, ensuring the backend can validate the user's identity and role for every action.

---
*Created as part of the TurtleBot Security System Integration.*
