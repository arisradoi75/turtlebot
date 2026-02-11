#!/usr/bin/env python3
import rclpy
from rclpy.node import Node
from std_msgs.msg import Bool
import json
from datetime import datetime
import requests
from geometry_msgs.msg import PoseStamped
from nav2_simple_commander.robot_navigator import BasicNavigator, TaskResult
import time

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

    def alert_callback(self, msg):
        if msg.data:
            self.intruder_detected = True
            self.get_logger().warn("INTRUDER DETECTED! Stopping patrol.")

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
            url = "http://172.20.10.2:8080/api/robot/telemetry"
            headers = {'Content-Type': 'application/json'}
            requests.post(url, json=data, headers=headers, timeout=0.5)
            
            last_telemetry_time = current_time
            last_sent_status = status
        except requests.exceptions.ConnectionError:
            print("Telemetry error: Connection refused (Is Spring Boot running?)")
        except requests.exceptions.RequestException as e:
            print(f"Telemetry error: {e}")

def main(args=None):
    rclpy.init(args=args)
    
    # Create the monitor node to listen for alerts
    monitor = PatrolMonitor()
    
    # Initialize Nav2 BasicNavigator
    navigator = BasicNavigator()
    
    # Coordinates provided
    waypoints_coords = [
        (-3.6, 9.0),
        (-3.5, -9.5),
        (1.8, -9.6),
        (1.8, 7.4)
    ]
    
    # Wait for Nav2 to be fully active
    navigator.waitUntilNav2Active()
    
    self_patrolling = True
    last_x, last_y = 0.0, 0.0
    
    while rclpy.ok() and self_patrolling:
        for x, y in waypoints_coords:
            # Check for intruder before sending the next goal
            rclpy.spin_once(monitor, timeout_sec=0.1)
            if monitor.intruder_detected:
                update_status_file("ALERT", last_x, last_y)
                self_patrolling = False
                break
            
            # Create goal pose
            goal = PoseStamped()
            goal.header.frame_id = 'map'
            goal.header.stamp = navigator.get_clock().now().to_msg()
            goal.pose.position.x = x
            goal.pose.position.y = y
            goal.pose.orientation.w = 1.0
            
            navigator.goToPose(goal)
            
            # Wait for the robot to reach the waypoint
            while not navigator.isTaskComplete():
                feedback = navigator.getFeedback()
                if feedback:
                    pos = feedback.current_pose.pose.position
                    last_x, last_y = pos.x, pos.y
                    update_status_file("PATROLLING", last_x, last_y)

                # Check for alerts while moving
                rclpy.spin_once(monitor, timeout_sec=0.1)
                if monitor.intruder_detected:
                    navigator.cancelTask()
                    update_status_file("ALERT", last_x, last_y)
                    self_patrolling = False
                    break
            
            if not self_patrolling:
                break

    monitor.destroy_node()
    rclpy.shutdown()

if __name__ == '__main__':
    main()