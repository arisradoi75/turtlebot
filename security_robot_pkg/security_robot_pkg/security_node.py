#!/usr/bin/env python3
import rclpy
from rclpy.node import Node
import cv2
import numpy as np
from sensor_msgs.msg import Image
from std_msgs.msg import Bool, Int32
import json
import base64
from datetime import datetime
import requests
import os
from ultralytics import YOLO

class SecurityNode(Node):
    def __init__(self):
        super().__init__('security_node')
        
        # Subscribe to the camera topic
        # In ROS 2 TurtleBot simulations, the topic is often '/camera/image_raw'
        # or '/intel_realsense_r200_depth/image_raw' depending on the model.
        self.subscription = self.create_subscription(
            Image,
            '/camera/image_raw',
            self.image_callback,
            10  # QoS history depth
        )
        
        # Publisher for intruder alerts
        self.publisher_ = self.create_publisher(Bool, '/intruder_alert', 10)
        
        # Publisher for the AI video stream
        self.ai_image_pub = self.create_publisher(Image, '/camera/image_ai', 10)
        
        # Flag to prevent spamming messages for the same detection event
        self.red_object_detected = False
        
        self.current_battery_level = 100
        self.battery_subscription = self.create_subscription(
            Int32,
            '/battery_level',
            self.battery_callback,
            10
        )

        # Load the trained AI model
        # Priority 1: Check Home Directory for best_v2.pt
        home_model_path = os.path.join(os.path.expanduser("~"), "best_v2.pt")
        # Fallback: Check Home Directory for best.pt
        home_model_path_fallback = os.path.join(os.path.expanduser("~"), "best.pt")
        
        if os.path.exists(home_model_path):
            model_path = home_model_path
            self.get_logger().info(f"Loading custom model from Home: {model_path}")
        elif os.path.exists("best_v2.pt"):
            model_path = "best_v2.pt"
            self.get_logger().info(f"Loading custom model from current directory: {model_path}")
        elif os.path.exists(home_model_path_fallback):
            model_path = home_model_path_fallback
            self.get_logger().warn(f"'best_v2.pt' not found, but found 'best.pt' in Home. Loading: {model_path}")
        else:
            self.get_logger().error(f"Custom model 'best_v2.pt' NOT FOUND in {home_model_path} or current directory.")
            self.get_logger().warn("Switching to standard YOLOv8n (person detection).")
            model_path = "yolov8n.pt"  # Ultralytics will download this automatically

        try:
            self.model = YOLO(model_path)
            self.get_logger().info(f"AI Model loaded successfully: {model_path}")
        except Exception as e:
            self.get_logger().error(f"Failed to load AI model: {e}")
            self.model = None
        
        self.get_logger().info("Security Node started. AI Surveillance Active.")

    def battery_callback(self, msg):
        self.current_battery_level = msg.data

    def image_callback(self, msg):
        # Manual conversion to avoid cv_bridge/numpy 2.x conflict
        if msg.encoding == 'bgr8':
            cv_image = np.frombuffer(msg.data, dtype=np.uint8).reshape(msg.height, msg.width, 3)
        elif msg.encoding == 'rgb8':
            cv_image = np.frombuffer(msg.data, dtype=np.uint8).reshape(msg.height, msg.width, 3)
            cv_image = cv2.cvtColor(cv_image, cv2.COLOR_RGB2BGR)
        else:
            self.get_logger().warn(f"Unsupported encoding: {msg.encoding}")
            return

        # AI Detection Logic
        intruder_detected = False
        
        if self.model is not None:
            # Run YOLOv8 inference on the frame
            results = self.model(cv_image, verbose=False)
            class_names = results[0].names
            
            # Check if any objects were detected with sufficient confidence
            # results[0].boxes contains the detection boxes
            for box in results[0].boxes:
                confidence = float(box.conf)
                class_id = int(box.cls)
                class_name = class_names[class_id]
                x1, y1, x2, y2 = map(int, box.xyxy[0])

                # Only trigger alerts for the 'person' class.
                if class_name == 'person' and confidence > 0.75:
                    intruder_detected = True
                    # Draw bounding box on the image for the alert/display
                    cv2.rectangle(cv_image, (x1, y1), (x2, y2), (0, 0, 255), 2)
                    cv2.putText(cv_image, f"PERSON {confidence:.2f}", (x1, y1 - 10),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
                elif confidence > 0.5: # For debugging, show other detected objects without alerting
                    # Draw with a different color (e.g., cyan) for non-intruder objects
                    cv2.rectangle(cv_image, (x1, y1), (x2, y2), (255, 255, 0), 2)
                    cv2.putText(cv_image, f"{class_name} {confidence:.2f}", (x1, y1 - 10),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 0), 2)

        if intruder_detected:
            if not self.red_object_detected:
                self.get_logger().warn("SECURITY ALERT: HIGH THREAT! Intruder Detected by AI!")
                self.red_object_detected = True
                
                # Encode image to Base64
                _, buffer = cv2.imencode('.jpg', cv_image)
                image_base64 = base64.b64encode(buffer).decode('utf-8')
                
                alert_data = {
                    "alertType": "High Threat",
                    "message": "INTRUDER DETECTED! Alarm Triggered.",
                    "imageBase64": image_base64,
                    "timestamp": datetime.now().isoformat(),
                    "status": "ALERT",
                    "batteryLevel": self.current_battery_level,
                }
                self.get_logger().info(f"Alert data prepared: {alert_data}")

                with open("robot_alert.json", "w") as f:
                    json.dump(alert_data, f)
            
                # Send alert to Spring Boot application
                try:
                    # Send to Spring Boot backend
                    url = "http://100.90.57.82:8080/api/robot/alert"
                    headers = {'Content-Type': 'application/json'}
                    response = requests.post(url, json=alert_data, headers=headers, timeout=2.0)
                    if response.status_code == 200:
                        self.get_logger().info("Alert sent to backend successfully.")
                    else:
                        self.get_logger().warn(f"Failed to send alert to backend. Status: {response.status_code}")
                except requests.exceptions.RequestException as e:
                    self.get_logger().error(f"Error sending alert to backend: {e}")

            # Publish alert to /intruder_alert
            msg = Bool()
            msg.data = True
            self.publisher_.publish(msg)

            # Pop up window with alert message
            cv2.putText(cv_image, "INTRUDER ALERT!", (30, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 3)
        else:
            self.red_object_detected = False
            
        # Publish the processed image to /camera/image_ai for the web stream
        msg_out = Image()
        msg_out.header.stamp = self.get_clock().now().to_msg()
        msg_out.header.frame_id = "camera_link"
        msg_out.height = cv_image.shape[0]
        msg_out.width = cv_image.shape[1]
        msg_out.encoding = "bgr8"
        msg_out.is_bigendian = 0
        msg_out.step = cv_image.shape[1] * 3
        msg_out.data = cv_image.tobytes()
        self.ai_image_pub.publish(msg_out)

        # Show locally
        cv2.imshow("Security Camera", cv_image)
        cv2.waitKey(1)

def main(args=None):
    rclpy.init(args=args)
    node = SecurityNode()
    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        pass
    finally:
        node.destroy_node()
        rclpy.shutdown()

if __name__ == '__main__':
    main()