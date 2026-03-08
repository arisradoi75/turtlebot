from launch import LaunchDescription
from launch_ros.actions import Node
from launch.actions import ExecuteProcess
import os

def generate_launch_description():
    # Path to the patrol.py script
    patrol_script = os.path.join(
        os.path.expanduser("~"),
        "ros2_ws/src/turtlebot/security_robot_pkg/security_robot_pkg/patrol.py"
    )

    return LaunchDescription([
        # Start the Security Node (AI detection)
        Node(
            package='security_robot_pkg',
            executable='security_node',
            name='security_node',
            output='screen',
            emulate_tty=True
        ),
        
        # Start the Web Video Server on port 8080
        Node(
            package='web_video_server',
            executable='web_video_server',
            name='web_video_server',
            output='screen',
            parameters=[{'port': 8080}]
        ),
        
        # Start the Patrol Logic (Python Script)
        ExecuteProcess(
            cmd=['python3', patrol_script],
            output='screen'
        )
    ])