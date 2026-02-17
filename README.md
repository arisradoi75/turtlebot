# Robot Security Dashboard (Backend)

This is the backend for a **TurtleBot** security and monitoring system. Built using **Java Spring Boot 3**, it serves as the Command and Control (C2) center. It handles user authentication, receives real-time telemetry from the robot, processes security alerts, and sends control commands to the robot.

## Key Features

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

## Tech Stack

| Category | Technology |
| :--- | :--- |
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.x |
| **Database** | MySQL (Spring Data JPA) |
| **Security** | Spring Security 6, JJWT |
| **Real-time** | Spring WebSocket (STOMP) |
| **Tools** | Maven, Lombok |

---

## Installation and Setup

### 1. Prerequisites
* JDK 17 or newer
* MySQL Server
* Maven

### 2. Database Configuration
Create an empty database named `robot_db`. Then, configure `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/robot_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT Secret (Change this in production!)
application.security.jwt.secret-key=BF7FD11ACE545745B7BA1AF98B6F156D127BC7BB544BAB6A4FD74E4FC7
```

### 3. Robot IP Configuration
 **Important:** The robot's IP address is currently hardcoded in `CommandService.java`.
Check the following line before running:
```java
private final String ROBOT_API_URL = "[http://172.20.10.12:5000/api/command](http://172.20.10.12:5000/api/command)";
```
If your robot has a different IP on the local network, update this string.

### 4. Running the App
```bash
mvn clean install
mvn spring-boot:run
```

---

##  API Documentation

The backend exposes a RESTful API reachable at `http://localhost:8080`.

###  Authentication & Authorization
Most endpoints require a valid **JWT Token** in the HTTP Header.
* **Header Format:** `Authorization: Bearer <your_access_token>`
* **Roles:**
    * `USER`: Read-only access (Telemetry, Alerts).
    * `ADMIN`: Write access (Robot Commands).

### 1. Authentication Endpoints
**Base URL:** `/api/v1/auth` (Public)

#### Register
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

#### Login
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

### 2. Admin Commands
**Base URL:** `/api/admin`
*Requires Role:* `ADMIN`

* **Start Robot:** `POST /start`
* **Stop Robot (Emergency):** `POST /stop`
* **Dock Robot:** `POST /dock`

---

### 3. Robot Ingest API
**Base URL:** `/api/robot`
*Used by the TurtleBot (ROS/Python) to push data.*

#### Send Telemetry
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

#### Send Security Alert
Triggered when the robot detects an anomaly.
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

---

## WebSocket API (Real-time)
Used by the Frontend Dashboard to receive live updates.

* **Connection Endpoint:** `ws://localhost:8080/ws-robot`
* **Protocol:** STOMP over SockJS

### Subscribe Topics
| Topic | Description | Payload Structure |
| :--- | :--- | :--- |
| `/topic/telemetry` | Live robot coordinates & battery | Same as Telemetry JSON |
| `/topic/alerts` | Incoming security alerts | Same as Alert JSON |

---

## Project Structure

```bash
src/main/java/com/example/robot
├──  auth/                 # JWT Logic, User Details, Auth Service
├──  config/               # Security and WebSocket Configuration
├──  controller/           # REST Controllers
├──  dto/                  # Data Transfer Objects
├──  model/                # Database Entities (Hibernate)
├──  repository/           # JPA Interfaces
└──  service/              # Business Logic (Commands, Processing)
```
