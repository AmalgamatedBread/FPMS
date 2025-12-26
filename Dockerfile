FROM openjdk:21-slim

WORKDIR /app

COPY target/fpms-backend-*.jar app.jar

RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]