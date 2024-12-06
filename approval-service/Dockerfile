FROM ubuntu:latest
LABEL authors="ommakwana"

ENTRYPOINT ["top", "-b"]


# -----------------
# Build Stage
# -----------------
FROM gradle:8-jdk22-alpine AS builder

# Copy the application source code to the builder image
COPY --chown=gradle:gradle . /home/gradle/src

# Set the working directory for the builder
WORKDIR /home/gradle/src

# Build the application without running tests
RUN gradle build -x test

# -----------------
# Package Stage
# -----------------
FROM openjdk:22-jdk

# Create a directory for the application
RUN mkdir /app

# Copy the built JAR file from the builder stage
COPY --from=builder /home/gradle/src/build/libs/*jar /app/approval-service.jar



# Expose the application port defined in application-docker.properties
EXPOSE 9007

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/approval-service.jar"]
