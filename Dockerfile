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

# Tuned for Render free tier (512MB):
# Heap 210m + Metaspace 150m + CodeCache 56m + JVM overhead ~50m ≈ 466m → ~46m buffer
ENTRYPOINT ["java", \
            "-Xmx210m", \
            "-Xms128m", \
            "-XX:MaxMetaspaceSize=150m", \
            "-XX:CompressedClassSpaceSize=48m", \
            "-XX:ReservedCodeCacheSize=56m", \
            "-Xss256k", \
            "-XX:+UseSerialGC", \
            "-XX:+TieredCompilation", \
            "-XX:TieredStopAtLevel=1", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]