#!/usr/bin/env python3
import rclpy
from rclpy.node import Node
import cv2
import numpy as np
from sensor_msgs.msg import Image
import os
from datetime import datetime

class DataCollector(Node):
    def __init__(self):
        super().__init__('data_collector')
        
        # Subscribe to the camera topic
        self.subscription = self.create_subscription(
            Image,
            '/camera/image_raw',
            self.image_callback,
            10
        )
        
        # Create dataset directory if it doesn't exist
        self.save_dir = "dataset_robot"
        if not os.path.exists(self.save_dir):
            os.makedirs(self.save_dir)
            
        self.get_logger().info(f"Data Collector Node Started.\nPress 'i' for Intruder, 'b' for Background.\nSaving to '{self.save_dir}/'.")

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

        # Display the image
        cv2.imshow("Data Collector - 'i': Intruder, 'b': Background", cv_image)
        
        # Check for key press (1ms delay)
        key = cv2.waitKey(1) & 0xFF
        
        if key == ord('i'):
            self.save_image(cv_image, "intruder")
        elif key == ord('b'):
            self.save_image(cv_image, "background")

    def save_image(self, image, label):
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")
        filename = os.path.join(self.save_dir, f"{label}_{timestamp}.jpg")
        cv2.imwrite(filename, image)
        self.get_logger().info(f"Saved {label}: {filename}")

def main(args=None):
    rclpy.init(args=args)
    node = DataCollector()
    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        pass
    finally:
        node.destroy_node()
        if rclpy.ok():
            rclpy.shutdown()
        cv2.destroyAllWindows()

if __name__ == '__main__':
    main()