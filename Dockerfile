FROM gradle:8.7-jdk17 AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY src ./src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:18-jre
WORKDIR /app

RUN apt-get update && \
    apt-get install -y python3 python3-pip python3-scipy && \
    pip3 install simpy psutil && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/tmp

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]