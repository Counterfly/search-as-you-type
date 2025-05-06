# Finagle Logging Service

This project is a basic Scala service using Finagle HTTP that provides a single endpoint for logging messages. The service is structured to handle incoming log requests and respond appropriately.

## Project Structure

```
search-as-you-type
├── src
│   └── main
│       ├── scala
│       │   └── com
│       │       └── counterfly
│       │           └── logging
│       │               ├── Server.scala
│       │               ├── LogService.scala
│       │               └── LogController.scala
│       └── resources
│           └── logback.xml
├── build.sbt
├── docker-compose.yml
├── project
│   └── build.properties
└── README.md
```

## Setup Instructions

1. **Clone the repository:**
   ```
   git clone <repository-url>
   cd finagle-logging-service
   ```

2. **Build the project:**
   Make sure you have [sbt](https://www.scala-sbt.org/) installed. Run the following command to build the project:
   ```
   sbt compile
   ```

3. **Run the service:**
   You can start the Finagle HTTP server by running:
   ```
   sbt "server/run -config base"
   ```

4. **Access the log endpoint:**
   Once the server is running, you can send log messages to the endpoint:
   ```
   POST http://localhost:8080/log
   Content-Type: text/plain
   Hello, World
   ```
   e.g. using `curl`:
   ```bash
   curl -X POST -H "Content-Type: text/plain" http://localhost:8080/log -d "Hello, World"
   ```

   e.g. using `just`:
   ```bash
   just log "Hello, World"
   ```

## Kafka Setup

The service is configured to send log messages to a Kafka topic.
You can use the included Docker Compose file to set up Kafka and monitor the messages:

1. **Start Kafka and related services:**
   ```bash
   docker-compose up -d
   ```

2. 2. **Monitor log messages:**
   The docker-compose file includes a consumer that automatically prints messages from the `log_received` topic to stdout.
   You can view these logs with:
   ```bash
   python ./scripts/consumer.py log_received
   ```
<!--
2. **Monitor log messages:**
   The docker-compose file includes a consumer that automatically prints messages from the `log_received` topic to stdout.
   You can view these logs with:
   ```bash
   docker logs -f log-consumer
   ```
-->

3. **Access Kafka UI:**
   A Kafka UI is available at http://localhost:8081 for monitoring topics, messages, and consumer groups.

4. **Stop all services:**
   ```bash
   docker-compose down
   ```

## Usage Examples

- To log a message, send a POST request to the `/log` endpoint with a JSON body containing the log message.

## Dependencies

This project uses the following dependencies:
- Finagle HTTP for building the service.
- Logback for logging configuration.
- Kafka to produce logs to Kafka


## Flow Diagram

![Log Processing Flow](./docs/architecture.draw.io.svg)
  - first the log is sent to a Kafka topic, log_received
  - TODO: then a consumer reads the log from the topic and applies a filtering logic to determine searches
  
