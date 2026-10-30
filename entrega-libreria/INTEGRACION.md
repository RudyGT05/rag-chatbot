# chatbot-rag 1.0.0 — Cómo integrarlo en tu proyecto

Feature 2 (Chatbot con RAG) empaquetada como librería Spring Boot. Al agregarla,
tu aplicación expone el chatbot sin escribir código: se auto-configura sola.

## Contenido de esta entrega

| Archivo | Para qué |
|---|---|
| `chatbot-rag-1.0.0.jar` | La librería |
| `pom.xml` | Sus dependencias (Maven las descarga solas) |
| `docker-compose.infra.yml` | Postgres+pgvector, Ollama y DynamoDB Local |
| `INTEGRACION.md` | Esta guía (backend) |
| `INTEGRACION-WEB.md` y `web/` | El botón del chat para tu página Next.js |

## Requisitos de tu proyecto

- **Spring Boot 3.x** (probada con 3.5.6) con **Spring MVC** (no WebFlux)
- **Java 17** o superior
- **Docker**, para la infraestructura

## Pasos

### 1. Instalar la librería en tu Maven local

Desde la carpeta donde están el `.jar` y el `pom.xml`:

```powershell
mvn install:install-file "-Dfile=chatbot-rag-1.0.0.jar" "-DpomFile=pom.xml"
```

Sin Maven instalado, con Docker:

```powershell
docker run --rm -v "${PWD}:/w" -v "$env:USERPROFILE\.m2:/root/.m2" -w /w maven:3.9-eclipse-temurin-17 mvn -q install:install-file "-Dfile=chatbot-rag-1.0.0.jar" "-DpomFile=pom.xml"
```

### 2. Agregar la dependencia en tu `pom.xml`

```xml
<dependency>
    <groupId>gt.edu.umg.desarrolloweb</groupId>
    <artifactId>chatbot-rag</artifactId>
    <version>1.0.0</version>
</dependency>
```

No hace falta `@ComponentScan` ni `@Import`.

### 3. Levantar la infraestructura

```powershell
docker compose -f docker-compose.infra.yml up -d
```

La primera vez Ollama descarga ~3 GB de modelos.

### 4. Configurar (en tu `application.yml`)

Todo tiene valores por defecto que apuntan a los contenedores del paso 3, así que
**sin configurar nada ya funciona** con las preguntas frecuentes (FAQ).

Para leer tarifas, formas de pago y sectores de la réplica del monolito
(solo lectura) y guardar el historial:

```yaml
chatbot:
  monolito:
    enabled: true
    url: jdbc:mysql://HOST:3306/aqua_gest?allowPublicKeyRetrieval=true
    username: ${MONOLITO_DB_USERNAME}
    password: ${MONOLITO_DB_PASSWORD}
  historial:
    enabled: true            # guarda las conversaciones en DynamoDB Local
```

> Las credenciales de la réplica se piden a Rudy por privado. **Nunca** se
> escriben en el `application.yml` ni se suben al repositorio: van en variables
> de entorno.

Sin el monolito, las preguntas de precio se responden con el FAQ y no con las
tarifas vigentes reales.

### 5. Probar

Arranca tu aplicación. En el log debe salir:

```
Ingesta completada: 27 fragmentos FAQ, 6 del monolito, ... ms
```

(`0 del monolito` si no lo configuraste). Luego:

```powershell
curl.exe -X POST http://localhost:8080/api/v1/chat -H "Content-Type: application/json" -d '{\"question\": \"cuanto cuesta la media paja?\"}'
```

(Cambia `8080` por el puerto de tu aplicación.)

## Endpoints que agrega

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/v1/chat` | Body `{"question": "..."}` (3 a 500 caracteres). Responde `answer` y `sources` |
| `POST` | `/api/v1/ingest` | Vuelve a cargar el FAQ y los datos del monolito |

Límite: 10 preguntas por minuto por IP (`chatbot.rate-limit.preguntas-por-minuto`).

## Si algo choca con tu proyecto

| Situación | Solución |
|---|---|
| Quieres apagar el chatbot sin quitar la dependencia | `chatbot.enabled: false` |
| Tu proyecto **no** usa base de datos y falla al arrancar pidiendo una URL | `spring.autoconfigure.exclude: org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` |
| Ya usas el puerto 5433, 11434 u 8000 | Cambia el puerto en el compose y en `chatbot.vector-db.url`, `spring.ai.ollama.base-url` o `chatbot.historial.endpoint` |
| Tu proyecto ya usa Spring AI con otro modelo | Avísale a Rudy: hay que revisar qué `ChatClient` se usa |
| Tu clase `main` está **directamente** en `gt.edu.umg.desarrolloweb` (no en un subpaquete) | Funciona, pero tu escaneo también encuentra las clases del chatbot y `chatbot.enabled: false` deja de apagarlo. Lo normal es tener el main en un subpaquete, ej. `gt.edu.umg.desarrolloweb.consulta` |

La librería **no** toca la base de datos de tu proyecto: usa conexiones propias,
y la del monolito es de solo lectura.
