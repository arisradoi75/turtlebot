import os
from glob import glob
from setuptools import setup

package_name = 'security_robot_pkg'

setup(
    name=package_name,
    version='0.0.0',
    packages=[package_name],
    data_files=[
        ('share/ament_index/resource_index/packages',
            ['resource/' + package_name]),
        ('share/' + package_name, ['package.xml']),
        (os.path.join('share', package_name, 'launch'), glob('launch/*.launch.py')),
    ],
    install_requires=['setuptools'],
    zip_safe=True,
    maintainer='radu',
    maintainer_email='radu@todo.todo',
    description='Security Robot Package',
    license='TODO: License declaration',
    tests_require=['pytest'],
    entry_points={
        'console_scripts': [
            'security_node = security_robot_pkg.security_node:main',
            'data_collector = security_robot_pkg.data_collector:main',
            'patrol = security_robot_pkg.patrol:main',
            'train_model = security_robot_pkg.train_model:main',
        ],
    },
)

