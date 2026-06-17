FROM openjdk:21-jdk-slim AS build
RUN apt-get update && \
    apt-get install -y python3 python3-pip g++ && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM openjdk:21-jdk-slim
RUN apt-get update && \
    apt-get install -y python3 python3-pip g++ && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]