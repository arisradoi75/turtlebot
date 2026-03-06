#  TurtleBot Security System

An automated IoT security system based on **ROS2** and a **Full-Stack Web** architecture. 

The project integrates a virtual TurtleBot (simulated in a Gazebo environment - an Amazon warehouse) that patrols a mapped area, detects intruders, and transmits real-time security alerts with photo evidence to a secure web dashboard.

---

##  Demo Video
See the robot in action and the system's functionality here:
 **[Watch the demonstration on YouTube](PUT_YOUTUBE_LINK_HERE)**

---

##  System Architecture



The project is divided into three major components communicating via a virtual private network (**Tailscale**), allowing remote development and operation (MacBook <-> Linux).

1. ** Robot Component (ROS2 & Gazebo) - Running on Linux**
   * Handles navigation, mapping (SLAM), and the physical simulation of the TurtleBot in the Amazon warehouse.
   * The detection module recognizes intruders and triggers an event.
   * Communicates with the Backend via HTTP POST requests (sending telemetry and images) and exposes a Python/Flask server to receive commands (Start, Stop, Dock).

2. ** Backend Component (Spring Boot & Java 17) - Running on macOS**
   * The central server routing the information.
   * Handles user authentication and authorization (JWT, Spring Security).
   * Saves telemetry history and alerts in the MySQL database.
   * Broadcasts real-time data to the Frontend via **WebSockets**.

3. ** Frontend Component (React.js & Vite) - Running on macOS**
   * The User Interface (Dashboard).
   * Implements **Role-Based Access Control (RBAC)**:
     * **USER:** Can only monitor live status and alerts.
     * **ADMIN:** Can monitor and has access to the Control Panel to send commands to the robot.

---

## Repository Structure (Branches)

For efficient organization and parallel development, the source code has been separated into multiple branches:

* `main` ➔ The current branch; contains the centralized project documentation.
* `gaz+ros2` ➔ Source code for the Robotics part (ROS2, Gazebo, detection and control scripts).
* `back-end` ➔ Source code for the Backend (Spring Boot, Security, WebSockets).
* `front-end` ➔ Source code for the Frontend (React.js, web interface).

---

## Detailed Documentation

The comprehensive technical documentation is divided into two major sections, detailing the work of each team member:

1.  **[Web Documentation: Frontend & Backend (Author: Aris)](Backend_Frontend_Doc.md)** * *Contains details about the database, Spring Security, JWT, WebSockets, and the React interface.*
2.  **[Robotics Documentation: ROS2 & Gazebo (Author: Radu)](ROS2_Gazebo_Doc.md)**
   * *Contains details about the simulation, navigation, ROS2 nodes, and detection scripts.*

---
*Project developed by Aris Rădoi and Radu Asoltanei.*
