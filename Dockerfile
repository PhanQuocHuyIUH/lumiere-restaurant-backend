FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw && ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# ENTRYPOINT ["java", "-Xmx450m", "-jar", "app.jar"]

# Cấu hình JVM ép xung chuyên biệt cho môi trường 512MB RAM
ENTRYPOINT ["java", \
            "-Xmx300m", \
            "-Xms300m", \
            "-XX:MaxMetaspaceSize=128m", \
            "-Xss256k", \
            "-XX:+UseSerialGC", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]