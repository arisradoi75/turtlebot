# 🤖 Robot Security Dashboard (Backend)

This is the backend for a **TurtleBot** security and monitoring system. Built using **Java Spring Boot 3**, it serves as the Command and Control (C2) center. It handles user authentication, receives real-time telemetry from the robot, processes security alerts, and sends control commands to the robot.

## 🌟 Key Features

### 1. Security & Authentication
* **JWT (JSON Web Tokens):** Complete system for login, registration, and token validation.
* **Refresh Tokens:** Mechanism to maintain active sessions without frequent re-logins.
* **RBAC (Role-Based Access Control):**
  * `USER`: Can view telemetry data and alerts.
  * `ADMIN`: Has full control over the robot (Start, Stop, Dock).

### 2. Real-time Monitoring (WebSocket)
* Uses **WebSocket (STOMP)** to push data to the Dashboard instantly.
* **Telemetry:** Position (X, Y), battery level, and current status.
* **Alerts:** Instant notifications including images (Base64) when intruders are detected.

### 3. Robot Integration
* **Ingest API:** REST endpoints optimized to receive data from the robot's Python/ROS scripts.
* **Command Service:** Sends HTTP commands to the robot's internal API (`http://172.20.10.12:5000`).
* **Battery Monitoring:** Automatically generates internal alerts if the battery drops below 15%.

---

## 🛠️ Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.x |
| **Database** | MySQL (Spring Data JPA) |
| **Security** | Spring Security 6, JJWT |
| **Real-time** | Spring WebSocket (STOMP) |
| **Tools** | Maven, Lombok |

---

## 🚀 Installation and Setup

### 1. Prerequisites
* JDK 17 or newer
* MySQL Server
* Maven

### 2. Database Configuration
Create an empty database named `robot_db`. Then, configure `src/main/resources/application.properties`:

properties
spring.datasource.url=jdbc:mysql://localhost:3306/robot_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT Secret (Change this in production!)
application.security.jwt.secret-key=BF7FD11ACE545745B7BA1AF98B6F156D127BC7BB544BAB6A4FD74E4FC7


# 📡 API Documentation

The backend exposes a RESTful API reachable at `http://localhost:8080`.

## 🔐 Authentication & Authorization
Most endpoints require a valid **JWT Token** in the HTTP Header.
* **Header Format:** `Authorization: Bearer <your_access_token>`
* **Roles:**
    * `USER`: Read-only access (Telemetry, Alerts).
    * `ADMIN`: Write access (Robot Commands).

---

## 1. Authentication Endpoints
**Base URL:** `/api/v1/auth`
*These endpoints are public.*

### 📝 Register
Create a new user account.
* **URL:** `/register`
* **Method:** `POST`
* **Body:**
    ```json
    {
      "name": "John Doe",
      "username": "johndoe",
      "email": "john@example.com",
      "password": "securepassword123"
    }
    ```
* **Response (200 OK):**
    ```json
    {
      "accessToken": "eyJhGciOiJIUzI1Ni...",
      "refreshToken": "550e8400-e29b-41d4...",
      "name": "John Doe",
      "email": "john@example.com"
    }
    ```

### 🔑 Login
Authenticate an existing user.
* **URL:** `/login`
* **Method:** `POST`
* **Body:**
    ```json
    {
      "email": "john@example.com",
      "password": "securepassword123"
    }
    ```

---

## 2. Admin Commands
**Base URL:** `/api/admin`
*Requires Role:* `ADMIN`

### ▶️ Start Robot
Initiates the robot's patrolling routine.
* **URL:** `/start`
* **Method:** `POST`
* **Response:** `Comanda [START] a fost trimisă!`

### ⏹️ Stop Robot (Emergency)
Forces the robot to stop immediately.
* **URL:** `/stop`
* **Method:** `POST`
* **Response:** `Robotul a fost OPRIT de urgență!`

### 🏠 Dock Robot
Sends the robot back to the charging station.
* **URL:** `/dock`
* **Method:** `POST`
* **Response:** `Comanda [DOCK] a fost trimisă!`

---

## 3. Robot Ingest API
**Base URL:** `/api/robot`
*These endpoints are used by the TurtleBot (ROS/Python) to push data to the server.*

### 📍 Send Telemetry
Updates the robot's current status, battery, and location.
* **URL:** `/telemetry`
* **Method:** `POST`
* **Body:**
    ```json
    {
      "x": 12.5,
      "y": -4.3,
      "batteryLevel": 88.5,
      "status": "PATROLLING",
      "timestamp": "2023-10-27T10:15:30"
    }
    ```
    * *Supported Statuses:* `PATROLLING`, `ALERT`, `CHARGING`, `DOCKING`.

### 🚨 Send Security Alert
Triggered when the robot detects an anomaly (e.g., intruder).
* **URL:** `/alert`
* **Method:** `POST`
* **Body:**
    ```json
    {
      "alertType": "INTRUDER_DETECTED",
      "message": "Motion detected in Sector 4",
      "imageBase64": "iVBORw0KGgoAAAANSUhEUgAA...", 
      "timestamp": "2023-10-27T10:20:00"
    }
    ```
    * *Note:* `imageBase64` should contain the raw Base64 string of the snapshot.

---

## ⚡ WebSocket API (Real-time)
Used by the Frontend Dashboard to receive live updates.

* **Connection Endpoint:** `ws://localhost:8080/ws-robot`
* **Protocol:** STOMP over SockJS

### Subscribe Topics
| Topic | Description | Payload Structure |
| :--- | :--- | :--- |
| `/topic/telemetry` | Live robot coordinates & battery | Same as Telemetry JSON |
| `/topic/alerts` | Incoming security alerts | Same as Alert JSON |

# 🏗️ System Architecture

The system is designed using a **Layered Architecture**, separating control logic, data persistence, and hardware communication into distinct modules.

## 📊 Data Flow Diagram

mermaid
graph TD
    %% Node Definitions
    User((User / Admin))
    FE[💻 Frontend Dashboard]
    BE[⚙️ Spring Boot Backend]
    DB[(🗄️ MySQL Database)]
    ROBOT[🤖 TurtleBot 3]
    %% Relationships
    User -->|UI Interaction| FE
    FE -->|REST API (Auth, Commands)| BE
    BE <-->|WebSocket (Live Data)| FE
    BE -->|JPA / Hibernate| DB
    ROBOT -->|POST /telemetry| BE
    ROBOT -->|POST /alert (Base64)| BE
    BE -.->|POST /command (HTTP)| ROBOT
    %% Sub-graph for Backend Internals
    subgraph "Backend Services"
    AUTH[Auth Service]
    CMD[Command Service]
    PROCESS[Telemetry Processor]
    end
