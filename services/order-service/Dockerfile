FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /otel/agent.jar
COPY --from=build /app/target/*.jar app.jar
RUN useradd -r app && chown -R app /app /otel
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -javaagent:/otel/agent.jar"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
