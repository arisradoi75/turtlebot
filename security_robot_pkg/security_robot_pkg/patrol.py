#!/usr/bin/env python3
import rclpy
from rclpy.node import Node
from std_msgs.msg import Bool
from geometry_msgs.msg import PoseStamped
from nav2_simple_commander.robot_navigator import BasicNavigator, TaskResult

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
    
    while rclpy.ok() and self_patrolling:
        for x, y in waypoints_coords:
            # Check for intruder before sending the next goal
            rclpy.spin_once(monitor, timeout_sec=0.1)
            if monitor.intruder_detected:
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
                # Check for alerts while moving
                rclpy.spin_once(monitor, timeout_sec=0.1)
                if monitor.intruder_detected:
                    navigator.cancelTask()
                    self_patrolling = False
                    break
            
            if not self_patrolling:
                break

    monitor.destroy_node()
    rclpy.shutdown()

if __name__ == '__main__':
    main()