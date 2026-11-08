FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY backend ./backend
RUN mvn -B -ntp -pl backend/app -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/backend/app/target/backend-0.1.0-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8080
# Railway bills the memory a container holds. Without limits the JVM sizes its heap from the host (GBs) and
# keeps what it grew to; these flags cap it for a low-traffic API. Override JAVA_TOOL_OPTIONS in Railway if needed.
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -Xms48m -Xmx384m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:TieredStopAtLevel=1 -XX:+UseCompactObjectHeaders -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]
