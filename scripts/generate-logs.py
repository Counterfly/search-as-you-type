import random
import requests
import sys
import time

###
# Dependencies:
# requests==2.31.0
# 
# Entrypoint:
# python3 -m venv /pyenv
# source /pyenv/bin/activate
# python3 -m pip install --upgrade pip
# python3 -m pip install requests==2.31.0
###

def send_log(host_address: str, log: str) -> None:
    # Send POST request to the log endpoint
    try:
        response = requests.post(
            url=f"{host_address}/log",
            data=log.encode("utf-8"),
            headers={"Content-Type": "application/text"}
        )
        
        # Check if request was successful
        if response.status_code != 200:
            print(f"Failed to send log message. Status code: {response.status_code}")
            print(f"Response: {response.text}")
            
    except requests.exceptions.RequestException as e:
        print(f"Error sending request: {e}")


# Check if a topic is provided as a command-line argument
if len(sys.argv) < 2:
    print("Usage: python generate-logs.py <message1> <message2Opt> ... <messageNOpt>")
    sys.exit(1)

# Get topic name from command-line arguments
messages = sys.argv[1:]

print(f"Messages: {messages}")

def random_message():
    # Generate a random message
    return random.choice(messages)

while True:
    time.sleep(4)

    message = random_message()
    sb = []
    for c in message:
        sb.append(c)
        send_log("http://localhost:8888/v1", ''.join(sb))
    print(f"Generated logs for {message}")
