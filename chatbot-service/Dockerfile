# ---- Etapa 1: compilar con Maven (no necesitas Maven instalado para esto) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Etapa 2: imagen liviana solo con el JRE y el jar EJECUTABLE (*-exec.jar) ----
# El otro jar (chatbot-rag-1.0.0.jar, sin "-exec") es la LIBRERÍA para otros proyectos.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/chatbot-rag-*-exec.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
