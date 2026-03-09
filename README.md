Autonomous Security Patrol Robot (ROS 2 & AI Integration)

1. System Overview
This project implements an autonomous security and surveillance robot system using ROS 2 (Humble/Foxy), Nav2, and YOLOv8 artificial intelligence. The system consists of two primary modules:
1.	Navigation and Patrol Node (patrol.py): Manages autonomous waypoint navigation, battery simulation, REST API external control, and backend telemetry.
2.	AI Security Vision Node (security_node.py): Processes live camera feeds, runs a custom YOLOv8 model for intruder detection, and dispatches visual alerts to a centralized server.
The robot communicates with a remote backend (e.g., Spring Boot) via HTTP POST requests, enabling real-time monitoring and remote dispatching.
________________________________________
2. Architecture & Communication
The system utilizes a hybrid communication model:
•	Internal Communication: ROS 2 Topics for inter-node data exchange (e.g., camera feeds, battery levels, intruder alerts).
•	External Communication: REST APIs (Flask) to receive commands, and Python requests to push telemetry and alerts to a remote server.








Topic	Type	Publisher	Subscriber	Purpose		
/intruder_alert	std_msgs/Bool	security_node	patrol_monitor	Triggers patrol pause on detection.		
/battery_level	std_msgs/Int32	patrol_monitor	security_node	Shares battery state for alert metadata.		
/camera/image_raw	sensor_msgs/Image	Simulation/Camera	security_node	Raw video feed input.		
/camera/image_ai	sensor_msgs/Image	security_node	Web UI / RViz	Annotated video feed with bounding boxes.		

3. Module Details
4. 
3.1. Patrol & Navigation Module (patrol.py)
This node acts as the "Brain" for movement and state management. It leverages the Nav2 BasicNavigator to execute autonomous patrols.
Core Features:
•	Waypoint Patrolling: Follows a predefined set of coordinates within a custom map (warehouse_map_noua.yaml).
•	Dynamic State Management: Handles state transitions between PATROLLING, CHARGING, and ALERT. If an intruder is detected, the robot pauses its patrol for 5 seconds before resuming.
•	Battery Simulation: Battery decays by 10% every 60 seconds. At <20%, the robot autonomously cancels its patrol and returns to the docking coordinates (x: 0.018, y: 0.067) to recharge.
•	Telemetry Sync: Pushes robot coordinates, battery level, and current status to the backend server (http://100.90.57.82:8080/api/robot/telemetry) every 2 seconds or on state change.
REST API Endpoints (Flask): Exposed on port 5000 to allow external control (e.g., from a web dashboard).
•	POST /start - Initiates the autonomous patrol.
•	POST /stop - Halts the robot in place.
•	POST /dock - Forces the robot to return to the charging station.
•	POST /api/command - Universal endpoint to handle JSON payloads containing {"command": "start/stop/dock"}.

3.2. AI Security & Vision Module (security_node.py)
This node serves as the "Eyes" of the robot, processing environmental data using Deep Learning.
Core Features:
•	Custom YOLOv8 Integration: Attempts to load a custom-trained model (best_v2.pt or best.pt) from the home directory. Falls back to the standard yolov8n.pt if the custom weights are missing.
•	Intruder Detection: Scans frames for the person class. An alert is only triggered if the AI confidence score exceeds 75% (> 0.75).
•	Alert Generation: Upon detection, the node:
1.	Draws red bounding boxes and confidence scores over the intruder.
2.	Encodes the annotated frame into a Base64 string.
3.	Dispatches an immediate HTTP POST request containing the image, timestamp, and battery level to the backend (/api/robot/alert).
4.	Saves a local copy of the alert to robot_alert.json.
5.	Publishes a True boolean to /intruder_alert to halt the navigation node.

3.3. Dataset Collection Utility (data_collector.py)
To train the custom YOLOv8 model (best_v2.pt) used by the security node, this utility script allows for rapid, in-simulation data gathering directly from the robot's perspective.
Core Features:
•	Direct Image Processing: Subscribes to the /camera/image_raw topic and manually converts byte arrays to OpenCV images, intentionally bypassing common cv_bridge and numpy version conflicts.
•	Interactive GUI: Opens a live camera feed window with real-time keyboard listener support.
•	Rapid Categorization: * Pressing i captures and saves the current frame tagged as an intruder.
o	Pressing b captures and saves the current frame tagged as background.
•	Automated Storage: Generates precision-timestamped .jpg files and safely stores them in a dedicated ~/dataset_robot directory within the system's home folder, preventing workspace clutter.
________________________________________
How to update your "Execution" section:
You should also add this quick command to the 5. Execution section of your README so users know how to launch it:
Optional: Run the Data Collector (For AI Training) If you want to gather new images to retrain the YOLOv8 model, run this node instead of the security node:
Bash
ros2 run security_robot_pkg data_collector
(Keep the simulation running in the background, click on the camera pop-up window, and use 'i' and 'b' on your keyboard to capture frames).

________________________________________
4. Setup & Dependencies
Prerequisites
•	OS: Ubuntu 22.04 (Recommended)
•	ROS 2: Humble Hawksbill
•	Python Packages: rclpy, opencv-python, numpy, ultralytics, requests, flask
Installation
1.	Clone the repository into your ROS 2 workspace:
Bash
cd ~/ros2_ws/src
git clone <your-repo-url> turtlebot
2.	Install Python dependencies:
Bash
pip install ultralytics flask requests opencv-python
3.	Build the workspace:
Bash
cd ~/ros2_ws
colcon build --packages-select security_robot_pkg
source install/setup.bash
Important Configuration (Pre-Flight Checklist)
Before deploying, ensure the following parameters are updated in the code:
•	Backend IP Address: Ensure the Spring Boot server IP (100.90.57.82) is accessible from the robot. Update the URLs in patrol.py and security_node.py if the server IP changes.
•	Model Path: Place your trained best_v2.pt model in your Ubuntu ~ (Home) directory, or the root of the ROS 2 workspace execution path.
•	Map File: Ensure warehouse_map_noua.yaml is correctly located inside the maps/ directory of the security_robot_pkg share folder.
________________________________________
5. Execution
Terminal 1: Launch the simulated environment and robot
Bash
ros2 launch aws_robomaker_small_warehouse_world no_roof_small_warehouse.launch.py
ros2 launch turtlebot3_gazebo robot_state_publisher.launch.py use_sim_time:=True
Terminal 2: Launch Nav2
Bash
ros2 launch turtlebot3_navigation2 navigation2.launch.py use_sim_time:=True map:=/home/$USER/ros2_ws/src/turtlebot/security_robot_pkg/maps/warehouse_map_noua.yaml
Terminal 3: Start AI Security Node
Bash
ros2 run security_robot_pkg security_node
Terminal 4: Start Patrol Manager
Bash
ros2 run security_robot_pkg patrol_monitor
________________________________________
6. Future Enhancements
•	Dynamic Waypoints: Implement an API endpoint to receive and update patrol waypoints dynamically from the web dashboard.
•	Multi-Camera Support: Expand security_node.py to handle 360-degree camera feeds.
•	Hardware Battery Integration: Replace the simulated battery decay logic with real voltage readings from the robot's hardware sensors (e.g., /battery_state).
