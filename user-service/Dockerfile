FROM ubuntu:latest
LABEL authors="ommakwana"

ENTRYPOINT ["top", "-b"]

#-----------------
# Build Stage
#-----------------
FROM gradle:8-jdk22-alpine AS builder

# Copy the application code to the builder image
COPY --chown=gradle:gradle . /home/gradle/src

# Set the working directory
WORKDIR /home/gradle/src

# Build the application, excluding tests
RUN gradle build -x test

#-----------------
# Package Stage
#-----------------
FROM openjdk:22-jdk

# Create a directory for the application
RUN mkdir /app

# Copy the built JAR file from the builder stage
COPY --from=builder /home/gradle/src/build/libs/*jar /app/user-service.jar



# Expose the port the application runs on (port 9003 for user-service)
EXPOSE 9006

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/user-service.jar"]