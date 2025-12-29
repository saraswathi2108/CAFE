FROM maven:3.9.9-eclipse-temurin-21-alpine  AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar ./my-application.jar
EXPOSE 8090
CMD ["java", "-jar", "my-application.jar"]