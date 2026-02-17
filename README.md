# Robot Security Dashboard - Frontend

A real-time monitoring and control interface built with **React** for a TurtleBot security system. This frontend communicates with a **Spring Boot** backend using REST APIs for authentication/history and WebSockets for live telemetry.

## 🚀 Features

* **Real-time Telemetry:** Live updates of robot status, battery levels, and coordinates $(X, Y)$ via WebSockets (STOMP/SockJS).
* **Live Alerts:** Instant notifications with Base64 image snapshots when security events are detected.
* **Role-Based Access Control (RBAC):** * **User:** Can view live data and historical logs.
    * **Admin:** Full access to the Control Panel (Start, Stop, and Dock commands).
* **Authentication:** Secure JWT-based login and registration system.
* **Historical Data:** Fetches the last known robot state and previous alerts from the database upon login.
* **Responsive Design:** Clean, organized layout with separate sections for Live Data, Control, and Database History.

## 🛠️ Tech Stack

* **Framework:** React (Vite)
* **State Management:** React Hooks (`useState`, `useEffect`, `useRef`)
* **Routing:** React Router v6
* **Networking:** Axios (with Interceptors for JWT)
* **Real-time Communication:** SockJS-client & StompJS
* **Security:** JWT Decoding (`jwt-decode`)

## 📋 Prerequisites

Before running this application, ensure you have:
* [Node.js](https://nodejs.org/) (v16 or higher)
* The Spring Boot Backend server running on `http://localhost:8080`

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

3.  **Configuration:**
    Open `src/services/api.js` and verify the `API_URL`:
    ```javascript
    const API_URL = 'http://localhost:8080/api';
    ```

4.  **Run the development server:**
    ```bash
    npm run dev
    ```

## 🔌 API & WebSocket Integration

| Connection Type | Endpoint | Description |
| :--- | :--- | :--- |
| **REST** | `/api/v1/auth/*` | Login & Registration |
| **REST** | `/api/robot/latest-telemetry` | Get last saved status from DB |
| **REST** | `/api/admin/{command}` | Send commands (Admin only) |
| **WebSocket** | `/ws-robot` | Connection endpoint |
| **Topic** | `/topic/telemetry` | Live robot status updates |
| **Topic** | `/topic/alerts` | Live security alerts with snapshots |

## 📂 Project Structure

```text
src/
├── services/
│   └── api.js       # Axios instance with JWT interceptors
├── pages/
│   ├── Login.jsx     # Auth page with role extraction
│   ├── Register.jsx  # User registration
│   └── Dashboard.jsx # Main monitoring hub (WS & REST)
├── App.jsx           # Routing configuration
└── main.jsx          # Entry point & Global Polyfills
