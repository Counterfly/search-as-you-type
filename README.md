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
   sbt run
   ```

4. **Access the log endpoint:**
   Once the server is running, you can send log messages to the endpoint:
   ```
   POST http://localhost:8080/log
   Content-Type: application/json

   {
       "message": "Your log message here"
   }
   ```

## Usage Examples

- To log a message, send a POST request to the `/log` endpoint with a JSON body containing the log message.

## Dependencies

This project uses the following dependencies:
- Finagle HTTP for building the service.
- Logback for logging configuration.
- TODO: kafka to produce logs to Kafka.
