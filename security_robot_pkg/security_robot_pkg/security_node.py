#!/usr/bin/env python3
import rclpy
from rclpy.node import Node
import cv2
import numpy as np
from cv_bridge import CvBridge, CvBridgeError
from sensor_msgs.msg import Image
from std_msgs.msg import Bool

class SecurityNode(Node):
    def __init__(self):
        super().__init__('security_node')
        
        # Initialize CvBridge
        self.bridge = CvBridge()
        
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
        
        # Flag to prevent spamming messages for the same detection event
        self.red_object_detected = False
        
        self.get_logger().info("Security Node started. Watching for suspicious red objects...")

    def image_callback(self, msg):
        try:
            # Convert ROS Image message to OpenCV image
            cv_image = self.bridge.imgmsg_to_cv2(msg, "bgr8")
        except CvBridgeError as e:
            self.get_logger().error(f"CvBridge Error: {e}")
            return

        # Convert BGR to HSV (Hue, Saturation, Value)
        hsv = cv2.cvtColor(cv_image, cv2.COLOR_BGR2HSV)

        # Define range for red color (Red wraps around 0/180 in HSV)
        # Lower red range (0-10)
        lower_red1 = np.array([0, 100, 100])
        upper_red1 = np.array([10, 255, 255])
        
        # Upper red range (170-180)
        lower_red2 = np.array([170, 100, 100])
        upper_red2 = np.array([180, 255, 255])

        # Create masks for red color
        mask1 = cv2.inRange(hsv, lower_red1, upper_red1)
        mask2 = cv2.inRange(hsv, lower_red2, upper_red2)
        mask = mask1 + mask2

        # If we find a significant amount of red pixels, trigger the warning
        if cv2.countNonZero(mask) > 500:  # Threshold to filter out noise
            if not self.red_object_detected:
                self.get_logger().warn("SECURITY ALERT: Low Threat - Suspicious red object detected!")
                self.red_object_detected = True
            
            # Publish alert to /intruder_alert
            msg = Bool()
            msg.data = True
            self.publisher_.publish(msg)

            # Pop up window with alert message
            cv2.putText(cv_image, "INTRUDER ALERT!", (30, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 3)
            cv2.imshow("Security Camera", cv_image)
            cv2.waitKey(1)
        else:
            self.red_object_detected = False
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