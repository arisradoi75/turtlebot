#!/usr/bin/env python3
import rclpy
from rclpy.node import Node
from std_msgs.msg import Bool, Int32
import json
from datetime import datetime
import requests
from geometry_msgs.msg import PoseStamped, Twist
from nav2_simple_commander.robot_navigator import BasicNavigator, TaskResult
import time
from flask import Flask, jsonify, request
import threading

class PatrolMonitor(Node):
    def __init__(self):
        super().__init__('patrol_monitor')
        self.intruder_detected = False
        self.subscription = self.create_subscription(
            Bool,
            '/intruder_alert',
            self.alert_callback,
            10
        )
        self.battery_pub = self.create_publisher(Int32, '/battery_level', 10)
        # self.cmd_vel_pub = self.create_publisher(Twist, '/cmd_vel', 10)

    def alert_callback(self, msg):
        if msg.data and not self.intruder_detected:
            self.intruder_detected = True
            self.get_logger().warn("INTRUDER DETECTED!")

    def publish_battery(self, level):
        msg = Int32()
        msg.data = int(level)
        self.battery_pub.publish(msg)

    # def publish_velocity(self, linear, angular):
    #     msg = Twist()
    #     msg.linear.x = float(linear)
    #     msg.angular.z = float(angular)
    #     # self.cmd_vel_pub.publish(msg)

# Global variables for rate limiting
last_telemetry_time = 0.0
last_sent_status = None

def update_status_file(status, x, y, battery_level=100):
    global last_telemetry_time, last_sent_status
    data = {
        "x": x,
        "y": y,
        "batteryLevel": battery_level,
        "status": status,
        "timestamp": datetime.now().isoformat()
    }
    with open("robot_status.json", "w") as f:
        json.dump(data, f)

    current_time = time.time()
    # Send if status changed OR cooldown (2 seconds) expired
    if status != last_sent_status or (current_time - last_telemetry_time) > 2.0:
        try:
            # TODO: Replace 192.168.1.X with your Spring Boot computer's actual IP
            url = "http://100.90.57.82:8080/api/robot/telemetry"
            headers = {'Content-Type': 'application/json'}
            requests.post(url, json=data, headers=headers, timeout=0.5)
            
            last_telemetry_time = current_time
            last_sent_status = status
        except requests.exceptions.ConnectionError:
            print("Telemetry error: Connection refused (Is Spring Boot running?)")
        except requests.exceptions.RequestException as e:
            print(f"Telemetry error: {e}")

def send_battery_alert(battery_level):
    alert_data = {
        "alertType": "BATTERY LOW",
        "message": f"Battery level is {battery_level}% remaining",
        "imageBase64": "",
        "timestamp": datetime.now().isoformat(),
        "status": "ALERT"
    }
    try:
        url = "http://100.90.57.82:8080/api/robot/alert"
        headers = {'Content-Type': 'application/json'}
        requests.post(url, json=alert_data, headers=headers, timeout=0.5)
    except requests.exceptions.RequestException as e:
        print(f"Battery alert error: {e}")

class PatrolManager:
    def __init__(self, navigator, monitor, waypoints):
        self.navigator = navigator
        self.monitor = monitor
        self.waypoints = waypoints
        self.patrolling = False
        self.charging = False
        self.thread = None
        self.battery_level = 100
        self.last_x = 0.0
        self.last_y = 0.0
        self.last_battery_decay_time = time.time()
        self.battery_dead = False

    def start(self):
        if self.battery_dead:
            print("Cannot start: Battery is dead.")
            return False
        if self.patrolling:
            return False
        
        # If currently charging, stop it to switch to patrol
        if self.charging:
            self.charging = False
            self.navigator.cancelTask()
        
        use_cooldown = self.monitor.intruder_detected

        self.patrolling = True
        self.monitor.intruder_detected = False  # Reset intruder flag
        self.thread = threading.Thread(target=self.run, args=(use_cooldown,))
        self.thread.start()
        return True

    def stop(self):
        if not self.patrolling and not self.charging:
            return False
        self.patrolling = False
        self.charging = False
        self.navigator.cancelTask()
        return True

    def go_to_charge(self):
        if self.charging:
            return False
        # Stop patrol if active
        if self.patrolling:
            self.patrolling = False
            self.navigator.cancelTask()
            
        self.charging = True
        self.monitor.intruder_detected = False
        self.thread = threading.Thread(target=self.run_charge)
        self.thread.start()
        return True

    def run_charge(self):
        x, y = -3.6, 9.0
        self.monitor.publish_battery(self.battery_level)
        
        goal = PoseStamped()
        goal.header.frame_id = 'map'
        goal.header.stamp = self.navigator.get_clock().now().to_msg()
        goal.pose.position.x = x
        goal.pose.position.y = y
        goal.pose.orientation.w = 1.0
        
        self.navigator.goToPose(goal)
        
        while not self.navigator.isTaskComplete():
            if not self.charging:
                self.navigator.cancelTask()
                break
            
            feedback = self.navigator.getFeedback()
            if feedback:
                pos = feedback.current_pose.pose.position
                self.last_x, self.last_y = pos.x, pos.y
                update_status_file("RETURNING_TO_BASE", self.last_x, self.last_y, self.battery_level)
            
            # Check for intruder (optional, but good for security)
            rclpy.spin_once(self.monitor, timeout_sec=0.1)
            if self.monitor.intruder_detected:
                self.navigator.cancelTask()
                update_status_file("ALERT", self.last_x, self.last_y, self.battery_level)
                self.charging = False
                break
        
        if self.charging:
            update_status_file("CHARGING", x, y, self.battery_level)
            
            while self.charging and self.battery_level < 100:
                time.sleep(10)
                if not self.charging: break
                self.battery_level = min(100, self.battery_level + 10)
                self.monitor.publish_battery(self.battery_level)
                update_status_file("CHARGING", x, y, self.battery_level)
            
            self.charging = False

    def run(self, cooldown=False):
        if cooldown:
            print("Alert Cooldown: Waiting 10 seconds before resuming patrol...")
            time.sleep(10)

        self.monitor.publish_battery(self.battery_level)
        
        while rclpy.ok() and self.patrolling:
            for x, y in self.waypoints:
                if not self.patrolling: break
                
                # Battery decay logic
                if time.time() - self.last_battery_decay_time >= 60.0:
                    self.battery_level = max(0, self.battery_level - 10)
                    self.last_battery_decay_time = time.time()
                    self.monitor.publish_battery(self.battery_level)
                    
                    if self.battery_level == 0:
                        self.battery_dead = True
                        self.stop()
                        return

                    if self.battery_level < 20:
                        send_battery_alert(self.battery_level)
                        self.go_to_charge()
                        return

                # Check for intruder
                rclpy.spin_once(self.monitor, timeout_sec=0.1)
                if self.monitor.intruder_detected:
                    self.monitor.get_logger().warn("Intruder detected! Pausing patrol for 5 seconds.")
                    update_status_file("ALERT", self.last_x, self.last_y, self.battery_level)
                    time.sleep(5)
                    self.monitor.intruder_detected = False  # Reset alert
                    # Proceed to the current waypoint
                
                # Create goal pose
                goal = PoseStamped()
                goal.header.frame_id = 'map'
                goal.header.stamp = self.navigator.get_clock().now().to_msg()
                goal.pose.position.x = x
                goal.pose.position.y = y
                goal.pose.orientation.w = 1.0
                
                self.navigator.goToPose(goal)
                
                while not self.navigator.isTaskComplete():
                    if not self.patrolling:
                        self.navigator.cancelTask()
                        break
                    
                    feedback = self.navigator.getFeedback()
                    if feedback:
                        pos = feedback.current_pose.pose.position
                        self.last_x, self.last_y = pos.x, pos.y
                        
                        if time.time() - self.last_battery_decay_time >= 60.0:
                            self.battery_level = max(0, self.battery_level - 10)
                            self.last_battery_decay_time = time.time()
                            self.monitor.publish_battery(self.battery_level)
                            
                            if self.battery_level == 0:
                                self.battery_dead = True
                                self.stop()
                                return

                            if self.battery_level < 20:
                                send_battery_alert(self.battery_level)
                                self.go_to_charge()
                                return
                        
                        update_status_file("PATROLLING", self.last_x, self.last_y, self.battery_level)
                    
                    rclpy.spin_once(self.monitor, timeout_sec=0.1)
                    if self.monitor.intruder_detected:
                        self.navigator.cancelTask()
                        self.monitor.get_logger().warn("Intruder detected! Pausing patrol for 5 seconds.")
                        update_status_file("ALERT", self.last_x, self.last_y, self.battery_level)
                        time.sleep(5)
                        self.monitor.intruder_detected = False # Reset alert
                        # Resume the current task
                        self.navigator.goToPose(goal)
                        continue
                
                if not self.patrolling:
                    break

app = Flask(__name__)
patrol_manager = None

@app.route('/start', methods=['POST'])
def start_patrol():
    if patrol_manager.start():
        return jsonify({"status": "Patrol started"}), 200
    return jsonify({"status": "Already patrolling"}), 400

@app.route('/stop', methods=['POST'])
def stop_patrol():
    if patrol_manager.stop():
        return jsonify({"status": "Patrol stopped"}), 200
    return jsonify({"status": "Not patrolling"}), 400

@app.route('/dock', methods=['POST'])
def dock_robot():
    if patrol_manager.go_to_charge():
        return jsonify({"status": "Returning to base"}), 200
    return jsonify({"status": "Could not start charging sequence"}), 400

@app.route('/api/command', methods=['POST'])
def handle_command():
    # Handle generic command endpoint from Spring Boot
    # Try to parse JSON (force=True allows missing Content-Type)
    data = request.get_json(silent=True, force=True) or request.form.to_dict() or {}
    
    # Check common keys or fall back to raw body
    command = data.get('command') or data.get('action')
    if not command:
        command = request.get_data(as_text=True)
        
    command = str(command).lower()
    print(f"DEBUG: Received command payload: {command}")
    
    if 'start' in command:
        return start_patrol()
    elif 'stop' in command:
        return stop_patrol()
    elif 'dock' in command:
        return dock_robot()
    # elif command in ['w', 'a', 's', 'd']:
    #     return manual_control(command)
    return jsonify({"status": "Invalid command", "received": data}), 400

# @app.route('/api/admin/jog/<direction>', methods=['POST'])
# def manual_control(direction):
#     command = str(direction).lower()
#     
#     if patrol_manager and patrol_manager.charging:
#         return jsonify({"status": "Ignored: Robot is charging"}), 409
# 
#     # If admin takes control, stop the patrol immediately
#     if patrol_manager and patrol_manager.patrolling:
#         patrol_manager.stop()
# 
#     linear = 0.0
#     angular = 0.0
#     
#     if command == 'w':
#         linear = 0.2
#     elif command == 's':
#         linear = -0.2
#     elif command == 'a':
#         angular = 0.5
#     elif command == 'd':
#         angular = -0.5
#     elif command == 'stop':
#         pass
# 
#     if patrol_manager and patrol_manager.monitor:
#         patrol_manager.monitor.publish_velocity(linear, angular)
#         return jsonify({"status": "Command executed", "linear": linear, "angular": angular}), 200
#     return jsonify({"status": "Error: Monitor not initialized"}), 500

def main(args=None):
    global patrol_manager
    rclpy.init(args=args)
    
    # Create the monitor node to listen for alerts
    monitor = PatrolMonitor()
    
    # Initialize Nav2 BasicNavigator
    navigator = BasicNavigator()
    navigator.waitUntilNav2Active()
    
    # Coordinates provided
    waypoints_coords = [
        (-3.6, 9.0),
        (-3.67, -9.33),
        (1.73, -9.43),
        (1.71, 6.43)
    ]
    patrol_manager = PatrolManager(navigator, monitor, waypoints_coords)
    
    # Wait for Nav2 to be fully active
    navigator.waitUntilNav2Active()
    
    # Auto-start patrol when application launches
    patrol_manager.start()
    
    # Run Flask in a separate thread
    flask_thread = threading.Thread(target=lambda: app.run(host='0.0.0.0', port=5000))
    flask_thread.daemon = True
    flask_thread.start()
    
    print(f"Patrol Node Started. \nListening for commands on: http://172.20.10.12:5000\nSending telemetry to: http://100.90.57.82:8080 (Check this IP!)")
    
    try:
        while rclpy.ok():
            if not patrol_manager.patrolling and not patrol_manager.charging:
                rclpy.spin_once(monitor, timeout_sec=0.1)
            time.sleep(0.1)
    except KeyboardInterrupt:
        pass

    monitor.destroy_node()
    rclpy.shutdown()

if __name__ == '__main__':
    main()