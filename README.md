# 🛡️ Robot Security Dashboard - Central Backend Unit

This repository contains the **Spring Boot 3** backend for a distributed security system. It acts as the "Command & Control" hub for an autonomous security robot simulated in **ROS2 and Gazebo** (running on a separate Linux node).

---

## 🏗️ System Architecture & Workflow

The project is designed to facilitate real-time monitoring and remote control of a security robot within a production warehouse.

### The Ecosystem:
1.  **Robot Node (Linux/ROS2):** Handles SLAM, navigation, and computer vision. It sends telemetry and security alerts to this backend.
2.  **Central Backend (This App):** Processes data, manages the MySQL database, handles JWT security, and broadcasts live updates via WebSockets.
3.  **Admin Dashboard (Frontend):** Connects to this backend to visualize the robot's state and send manual overrides.

### Data Flow:
* **Inbound (Telemetry):** Robot $\rightarrow$ REST API $\rightarrow$ Database & WebSocket Broadcast.
* **Inbound (Alerts):** Robot detects intruder $\rightarrow$ Sends Base64 image $\rightarrow$ Backend saves incident.
* **Outbound (Commands):** Admin Dashboard $\rightarrow$ Backend $\rightarrow$ Robot Flask Bridge $\rightarrow$ ROS2 Action.

---

## 🚀 Key Technical Features

### 🔐 Enterprise-Grade Security
* **JWT & Refresh Tokens:** Implements stateless authentication with `Json Web Tokens`.
* **Role-Based Access Control (RBAC):** * `ADMIN`: Authorized to send movement commands (`START`, `STOP`, `DOCK`).
    * `USER`: Authorized only to view the live dashboard and alert history.
* **Custom Security Filter:** Validates every request via `AuthFilterService` before reaching the controllers.

### 📊 Real-Time Monitoring & WebSockets
* **Live Telemetry:** Tracks $(x, y)$ coordinates and battery levels in real-time.
* **STOMP WebSockets:** Data is pushed instantly to the client via `/topic/telemetry` and `/topic/alerts`, ensuring zero-latency monitoring.
* **Internal Logic:** The backend automatically generates a `LOW_BATTERY` alert if the robot's battery level drops below 15% without a charging status.

### 🚨 Security Incident Handling
* **Base64 Image Processing:** The system handles `LONGTEXT` image snapshots from the robot's camera for immediate visual verification of alerts.
* **Persistence:** All events are logged in MySQL with original robot timestamps.

---

## 🛠️ Technology Stack

* **Framework:** Java 17, Spring Boot 3.4
* **Security:** Spring Security 6, JJWT (Json Web Token)
* **Database:** MySQL 8.x, Spring Data JPA (Hibernate)
* **Real-time:** Spring WebSocket (STOMP + SockJS)
* **Communication:** RestTemplate (for Robot HTTP Bridge)
* **Tooling:** Lombok, Maven

---

## 📂 Project Structure

| Package | Responsibility |
| :--- | :--- |
| **`.auth`** | Security configuration, JWT logic, and User/Role management. |
| **`.controller`** | REST Endpoints for Admin commands and Robot data ingestion. |
| **`.service`** | Business logic for commands, alerts, and telemetry processing. |
| **`.model` / `.dto`** | Database Entities (JPA) and Data Transfer Objects. |
| **`.repository`** | Interfaces for MySQL database operations. |
| **`.config`** | WebSocket and Global application beans. |

---

## ⚙️ Configuration & Setup

### 1. Prerequisites
* JDK 17+
* MySQL Server
* Local network connectivity between the Java host and the Linux/ROS2 host.

### 2. Database Setup
Update `src/main/resources/application.properties` with your MySQL credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/robot_security_db
spring.datasource.username=your_user
spring.datasource.password=your_password
