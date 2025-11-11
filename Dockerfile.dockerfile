# Dockerfile multistage para Spring Boot (Maven wrapper)
# Stage 1: build
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace/app

# copia mvnw y dependencias para usar cache
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
# bajar dependencias (offline) para cachear si no cambian pom.xml
RUN ./mvnw dependency:go-offline -B

# copia el código y construye
COPY src ./src
RUN ./mvnw -DskipTests clean package -Pprod -Dmaven.test.skip=true -B

# Stage 2: runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# copia jar creado (ajusta el nombre si tu artifactId/version es distinto)
ARG JAR_FILE=target/*.jar
COPY --from=builder /workspace/app/${JAR_FILE} app.jar

# puerto que usa spring-boot por defecto
EXPOSE 8080

# salud simple (opcional): si tu app tiene /actuator/health cambia si hace falta
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# run
ENTRYPOINT ["java","-Xms256m","-Xmx512m","-jar","/app/app.jar"]
