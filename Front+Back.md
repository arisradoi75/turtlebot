# 🛡️ TurtleBot Security Monitoring System

### Full-Stack Web Platform for Robot Monitoring and Control

A full-stack web application designed to **monitor, manage, and control a TurtleBot security robot in real time**.
The system combines **Spring Boot, React, WebSockets, and ROS2 integration** to provide a secure, reactive monitoring dashboard with role-based access control.

---

# 📦 Project Architecture

The system follows a **modern layered full-stack architecture** that ensures:

* real-time telemetry updates
* stateless security using JWT
* reactive UI updates through WebSockets
* strict role-based access control (RBAC)

```
Robot (ROS2 / Python)
        │
        │ HTTP POST
        ▼
Spring Boot Backend (REST + WebSockets)
        │
        │ STOMP / WebSocket
        ▼
React Frontend Dashboard
```

---

# ⚙️ Technology Stack

## Backend

| Technology                      | Purpose                        |
| ------------------------------- | ------------------------------ |
| **Java 17**                     | Core backend language          |
| **Spring Boot 3.2.3**           | REST API framework             |
| **Spring Security**             | Authentication & authorization |
| **JWT**                         | Stateless authentication       |
| **MySQL**                       | Relational database            |
| **Spring Data JPA / Hibernate** | ORM & persistence              |
| **WebSockets (STOMP + SockJS)** | Real-time robot telemetry      |

---

## Frontend

| Technology           | Purpose                        |
| -------------------- | ------------------------------ |
| **React.js**         | UI library                     |
| **Vite**             | Fast frontend bundler          |
| **React Router DOM** | Client-side routing            |
| **React Hooks**      | State and lifecycle management |
| **Axios**            | HTTP requests                  |
| **SockJS + stompjs** | WebSocket communication        |

---

# 🏗 Backend Architecture

The backend follows a **layered MVC / REST architecture** to ensure clear separation of responsibilities.

```
controller
│
├── RobotController
└── AdminController

service
│
├── RobotService
└── CommandService

repository
│
├── UserRepository
├── TelemetryRepository
└── AlertRepository

model
dto
auth
config
```

---

## Controllers

Location:

```
com.example.robot.controller
```

Responsibilities:

* expose REST endpoints
* receive HTTP requests
* delegate logic to services

Examples:

* `RobotController`
* `AdminController`

---

## Services

Location:

```
com.example.robot.service
```

Contain the **core business logic**.

### RobotService

Responsibilities:

* process telemetry data
* check battery levels
* trigger internal alerts

### CommandService

Acts as a **proxy between the web platform and the robot**.

Flow:

```
Admin Command → Spring Boot → Robot API
```

---

## Repositories & Models

Responsible for **database persistence** using Spring Data JPA.

Connected to **MySQL entities**.

---

## DTOs (Data Transfer Objects)

DTOs define the **exact shape of incoming JSON payloads**.

Benefits:

* decouples API contracts from database models
* prevents over-exposing entities
* improves security and maintainability

---

# 🔐 Authentication Module

All authentication logic is isolated inside a dedicated:

```
auth
```

package.

---

## Security Configuration

Located in:

```
config
```

Responsibilities:

* configure `SecurityFilterChain`
* disable CSRF for stateless APIs
* configure CORS
* enforce role-based routes

Example restriction:

```
/api/admin/**
```

Accessible only to:

```
ADMIN
```

---

## JWT Services

### JwtService

Handles:

* JWT generation
* token parsing
* cryptographic validation

---

### AuthFilterService

Custom filter extending:

```
OncePerRequestFilter
```

Responsibilities:

1. Intercept HTTP request
2. Extract Bearer token
3. Validate token
4. Load security context

---

### RefreshTokenService

Manages **persistent sessions** to avoid repeated logins.

---

# 🗄 Database Architecture

Database:

```
db_robot_security
```

Hibernate automatically updates the schema using:

```
ddl-auto=update
```

---

## Database Tables

### users

Stores user credentials.

Security features:

* passwords encrypted using `BCryptPasswordEncoder`
* role stored in:

```
type
```

Values:

```
ADMIN
USER
```

---

### refresh_token

Stores long-lived authentication sessions.

Fields:

* UUID token
* associated user
* expiration timestamp

---

### robot_telemetry

Stores the **robot's historical state**.

Example fields:

* X coordinate
* Y coordinate
* battery level
* operational status
* timestamp

---

### security_alert

Stores alerts triggered by the robot detection system.

Fields:

* alert type
* message
* timestamp
* intruder snapshot

The snapshot is stored as:

```
Base64 encoded LONGTEXT
```

---

# 👤 Application Workflows

The application uses **Role-Based Access Control (RBAC)**.

Two user types exist:

```
USER
ADMIN
```

---

# 📝 Account Registration Flow

1️⃣ User enters:

* name
* username
* email
* password

---

2️⃣ React sends request:

```
POST /v1/auth/register
```

---

3️⃣ Backend processing

`AuthService`:

1. extracts password
2. encrypts it with `BCryptPasswordEncoder`
3. saves `User` entity to MySQL

---

4️⃣ Completion

Backend returns:

```
JWT Access Token
Refresh Token
```

User is redirected to the **login page**.

---

# 👁 Standard User Flow (Passive Monitoring)

Role:

```
USER
```

Capabilities:

* view telemetry
* view alerts
* monitor robot status

Restrictions:

* **no robot control**

---

## Authentication

Request:

```
POST /v1/auth/login
```

---

## Token Handling

Backend returns:

```
Access Token
Refresh Token
```

Stored in:

```
localStorage
```

---

## Dashboard Initialization

React navigates to:

```
/dashboard
```

Requests latest stored data:

```
GET /api/robot/latest-telemetry
GET /api/robot/latest-alert
```

---

## WebSocket Connection

Frontend connects to:

```
ws://localhost:8080/ws-robot
```

Subscriptions:

```
/topic/telemetry
/topic/alerts
```

---

## Live Monitoring

The interface updates **instantly** as new telemetry arrives.

No manual refresh required.

---

## Security Restriction

User interface **hides the control panel**.

If a forged request is attempted:

```
POST /api/admin/start
```

Spring Security returns:

```
403 Forbidden
```

---

# 👨‍💻 Administrator Flow (Active Control)

Role:

```
ADMIN
```

Capabilities:

* monitoring
* command execution
* robot control

---

## Admin Dashboard

React detects the role inside the **JWT payload** and renders:

```
Admin Control Panel
```

---

## Command Execution

Admin buttons:

```
START
STOP
DOCK
```

---

## Secure Request

Example request:

```
POST /api/admin/stop
```

Axios automatically attaches:

```
Authorization: Bearer <token>
```

---

## Authorization

Spring Security validates:

```
@PreAuthorize("hasAuthority('ADMIN')")
```

---

## Command Routing

`CommandService` forwards the request to the robot API.

Example endpoint:

```
http://172.20.10.12:5000/api/command
```

---

## Robot Execution

The **TurtleBot simulation in Gazebo**:

1. executes the command
2. updates robot status
3. sends telemetry updates

---

# 🔄 Real-Time Data Flow

To minimize latency, the system uses a **publish-subscribe model** instead of HTTP polling.

---

## 1. Data Ingestion

The ROS2 Python node sends:

```
POST /api/robot/telemetry
POST /api/robot/alert
```

Payload includes:

* telemetry JSON
* Base64 encoded image snapshots

---

## 2. Data Processing

Spring Boot:

* maps JSON to DTOs
* evaluates business rules

Example rule:

```
battery < 15% → LOW_BATTERY alert
```

Data is persisted in MySQL.

---

## 3. WebSocket Broadcast

Using:

```
SimpMessagingTemplate
```

Data is published to:

```
/topic/telemetry
/topic/alerts
```

---

## 4. Frontend Reactivity

React components subscribe using `useEffect`.

When new data arrives:

```
setState()
```

This triggers an **instant UI re-render** with live metrics.
