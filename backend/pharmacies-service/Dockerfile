FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY backend ./backend
RUN mvn -B -ntp -pl backend/pharmacies-service -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/backend/pharmacies-service/target/pharmacies-service-0.1.0-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
