# Packages the jar built by Maven; run `mvn package` from the repository root first.
FROM eclipse-temurin:17-jre

RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /app
COPY target/*.jar app.jar

USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
