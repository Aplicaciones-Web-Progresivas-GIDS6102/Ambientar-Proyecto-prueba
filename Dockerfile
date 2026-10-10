# Etapa 1: Build de la aplicación con Gradle
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copiar el wrapper de Gradle y archivos de configuración
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./

# Asegurar saltos de línea de Linux (LF) en gradlew y darle permisos de ejecución
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copiar el código fuente
COPY src src

# Compilar la aplicación produciendo el JAR ejecutable
RUN ./gradlew bootJar -x test --no-daemon

# Etapa 2: Imagen de ejecución ligera
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el ejecutable JAR desde la etapa de compilación
COPY --from=build /app/build/libs/*.jar app.jar

# Render asignará automáticamente la variable $PORT (por defecto 8080)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
