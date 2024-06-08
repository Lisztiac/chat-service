FROM maven:3.9.7-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar chatter.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "chatter.jar"]
