# --- build the Spring Boot jar
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package \
 && cp "$(ls target/*.jar | grep -v plain | head -1)" /src/app.jar

# --- run it
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/app.jar app.jar
RUN mkdir -p /app/data
# H2 database lives in /app/data (mounted as a volume by docker-compose)
ENV DB_URL=jdbc:h2:file:/app/data/bodywise
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]
