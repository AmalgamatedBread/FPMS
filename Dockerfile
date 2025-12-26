FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy source code
COPY . .

# Build the app
RUN chmod +x mvnw && ./mvnw clean package -Pprod -DskipTests

# Create uploads directory
RUN mkdir -p /app/uploads && chmod 755 /app/uploads

EXPOSE 8080

# Run the app
CMD ["java", "-jar", "target/fpms-backend-*.jar"]
