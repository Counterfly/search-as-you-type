from kafka import KafkaConsumer
import json
import os
import sys

###
# Dependencies:
# kafka-python==2.1.5
# 
# Entrypoint:
# python3 -m venv /pyenv
# source /pyenv/bin/activate
# python3 -m pip install --upgrade pip
# python3 -m pip install kafka-python==2.2.4
###

# Check if a topic is provided as a command-line argument
if len(sys.argv) < 2:
    print("Usage: python console-consumer.py <topic_name>")
    sys.exit(1)

# Get topic name from command-line arguments
topic_name = sys.argv[1]
print(f"Starting consumer for topic: {topic_name}")

user = os.getlogin()
print(f"User logged in: {user}")

# Define consumer configuration
consumer = KafkaConsumer(
    topic_name,
    bootstrap_servers=['localhost:9092'],
    auto_offset_reset='earliest',  # Start consuming from the beginning of the topic if no offset is stored
    enable_auto_commit=True,  # Automatically commit offsets
    group_id=f"python-script-console-consumer-{user}", # consumer group name
    value_deserializer=lambda x: json.loads(x.decode('utf-8'))  # Deserialize JSON messages
)

# Consume messages
try:
    for message in consumer:
        print(f"Received message: {message.value} from partition: {message.partition}, offset: {message.offset}")
        # Process the message as needed
except KeyboardInterrupt:
    print("Consumer stopped by user")
finally:
    consumer.close()
