# Feature 2 — Chatbot con RAG (Oficina del Agua · Fase 2)

Dos piezas:

- **`chatbot-service`** (Spring Boot 3.5 + Spring AI): responde preguntas usando **RAG**.
- **`web-publica`** (Next.js 16): página pública + BFF + widget del chat
  (ver [`web-publica/README.md`](web-publica/README.md), incluye cómo integrar la Feature 1).

El RAG busca en:

1. **FAQ propio** (`src/main/resources/conocimiento/*.md`)
2. **Datos públicos del monolito** (tarifas), leídos en **solo lectura**

```
 Navegador ──► Next.js (BFF) ──► chatbot-service ──► Postgres + pgvector  (BD nueva, del chatbot)
                /api/chat          :8082          ├─► Ollama / OpenAI       (LLM + embeddings)
                                                  └─► MySQL del monolito    (solo lectura, opcional)
```

---

## 1. Qué instalar

| Herramienta | Para qué | Verificar |
|---|---|---|
| Docker Desktop | Postgres, Ollama, MySQL de prueba y el propio servicio | `docker --version` |
| JDK 17 (Temurin) | Correr el servicio fuera de Docker al desarrollar | `java -version` |
| IntelliJ IDEA Community | Editar y correr. **Trae Maven incluido** | — |
| Git | Repo del equipo | `git --version` |

> **Maven:** no hace falta instalarlo. Con Docker, el `Dockerfile` compila
> dentro de un contenedor que ya trae Maven. En IntelliJ se usa el Maven incluido
> (panel *Maven* a la derecha). Si prefieres la terminal, instálalo
> (`winget install Apache.Maven`) o copia `mvnw`, `mvnw.cmd` y `.mvn/` del
> proyecto de ejemplo de la Clase 11.

---

## 2. Correrlo por primera vez (solo con Docker)

```bash
cd feature2-chatbot
docker compose up -d --build
docker compose logs -f chatbot-service
```

La **primera vez** tarda: Ollama descarga dos modelos (~3 GB en total):

- `qwen2.5:3b` → el LLM que redacta las respuestas
- `bge-m3` → el modelo de **embeddings** (convierte texto en vectores; buen soporte de español)

Está listo cuando en el log aparece:

```
Ingesta completada: 27 fragmentos FAQ, 0 del monolito, ... ms
```

Luego abre la **página pública con el chat**: http://localhost:3000 (botón azul abajo a la derecha).

### Probar

- **Swagger UI:** http://localhost:8082/swagger-ui/index.html
- **curl:**

```bash
curl -X POST http://localhost:8082/api/v1/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"¿Qué hago si tengo una fuga en mi casa?"}'
```

Casos para el Demo Day:

| Prueba | Resultado esperado |
|---|---|
| `"¿Dónde puedo pagar?"` | 200, respuesta basada en `01-pagos.md`, con `sources` |
| `"¿Cuánto debe el contador MTR-00012?"` | 200, remite a la Consulta pública (regla de privacidad) |
| `"¿Quién ganó el mundial?"` | 200, "No tengo información sobre eso…" (no inventa) |
| `{"question":"ho"}` | 400 `VALIDATION_ERROR` |
| 11 preguntas en menos de 1 minuto | 429 `RATE_LIMITED` + header `Retry-After` |
| `docker compose stop ollama` y preguntar | 503 `LLM_UNAVAILABLE` |

> En una PC sin GPU, Ollama responde en ~5-20 s. La primera pregunta es la más
> lenta porque carga el modelo en memoria.

---

## 3. Desarrollar desde IntelliJ

1. Levanta solo la infraestructura: `docker compose up -d postgres-vector ollama`
2. Abre la carpeta `chatbot-service` en IntelliJ (detecta el `pom.xml`).
3. Panel Maven → **Lifecycle → compile**. Esto **genera** el código del contrato
   en `target/generated-sources/openapi` (`ChatApi`, `IngestaApi`, `*Dto`).
   Si IntelliJ marca `ChatApi` en rojo: clic derecho en el proyecto →
   *Maven → Reload project*.
4. Corre `ChatbotApplication`.

---

## 4. Cómo funciona (lo que tienes que poder explicar)

### Contract-first
`src/main/resources/static/openapi/chatbot-api.yaml` es el contrato. El plugin
`openapi-generator-maven-plugin` genera las interfaces y DTOs, y
`ChatController implements ChatApi`. Si cambia el YAML, el controller no
compila hasta actualizarlo.

### Ingesta (llenar la base vectorial)
`IngestaService` se ejecuta al arrancar y con `POST /api/v1/ingest`:

1. `FaqLoader` parte cada `.md` por sus secciones `## ` → 1 sección = 1 fragmento (*chunk*).
2. `TarifasMonolitoReader` (si `MONOLITO_ENABLED=true`) lee las tarifas vigentes y
   convierte cada fila en una **oración**.
3. `vectorStore.add(...)` genera el **embedding** de cada fragmento y lo guarda en
   pgvector junto con su metadato `origen` (`faq` / `monolito`).
4. Antes de cargar se borran los fragmentos del mismo origen → no quedan tarifas viejas.

### Pregunta (RAG)
`ChatService.responder()`:

1. **Retrieval:** la pregunta se convierte en embedding y se buscan los 4
   fragmentos más parecidos (similitud coseno, umbral 0.35).
2. Si no hay ninguno relevante → responde "No tengo información…" **sin llamar al LLM**.
3. **Augmented:** arma el prompt `CONTEXTO [1]…[4] + PREGUNTA`.
4. **Generation:** el LLM responde siguiendo el *system prompt* (solo usar el
   contexto, no inventar montos, no dar datos personales).

### Lo exacto se calcula en código, no en el LLM
Un LLM predice texto, no hace cuentas. En una prueba real, `qwen2.5:3b` respondió
"(36 − 30) = 12 → Q76.00" con el ejemplo correcto (Q52.00) en su contexto.
Por eso `CalculadoraConsumo` intercepta las preguntas con una cantidad de m³
("1/2 paja y 36 m³", "gasté 40 metros cúbicos") y calcula con la tarifa vigente
del monolito, con la misma fórmula del cobro:
`monto_base + max(0, consumo − equivalencia_m3) × precio_m3_excedente`.
El LLM solo redacta respuestas sobre información, nunca montos calculados.

### Seguridad
- **Privacidad:** el RAG solo contiene datos públicos. Nunca clientes, lecturas ni pagos.
- **Guardia en código** (`DetectorConsultaCuenta`): si la pregunta pide datos de una
  cuenta (código de contador, "cuánto debo", "mi saldo"…), responde con un mensaje fijo
  que remite a la Consulta pública **sin llamar al LLM**. Una regla de seguridad no se
  le deja al LLM, porque un modelo pequeño no siempre obedece el system prompt.
- **Monolito intacto:** usuario MySQL con solo `SELECT` + conexión `readOnly`.
- **Rate limit por IP** (`RateLimitFilter`): usa `X-Forwarded-For`, que debe enviar el BFF.
- **API key del LLM** solo en el servidor (variables de entorno), nunca en el navegador.
- Errores con el schema `Error`; sin stack traces (`include-stacktrace: never`).

---

## 5. Datos del monolito en el RAG

El chatbot lee la BD del monolito con el usuario `chatbot_ro` (**solo `SELECT`
en 5 objetos públicos**) y convierte cada fila en oraciones para el RAG:

| Lector | Tabla del monolito | Fragmentos |
|---|---|---|
| `TarifasMonolitoReader` | `tarifas` + `pajas` (misma regla que la vista `tarifas_vigencia`) | 1 por paja activa + 1 resumen (incluye cambios programados) |
| `MetodosPagoMonolitoReader` | `metodos_pago` | 1 con las formas de pago activas |
| `SectoresMonolitoReader` | `sectores` | 1 con los sectores con servicio |

Por defecto se conecta al **MySQL de prueba** del compose (`db/`: estructura real
traducida de las 31 migraciones + datos ficticios). Al arrancar verás:

```
Ingesta completada: 27 fragmentos FAQ, 6 del monolito, ... ms
```

Para la **BD real** solo cambian `MONOLITO_DB_URL`, `MONOLITO_DB_USERNAME` y
`MONOLITO_DB_PASSWORD` en `.env`. El código no se toca.

### Demo en vivo: el chatbot sigue los datos del monolito

1. En el chat: *"¿cuánto cuesta la media paja?"* → **Q28.00**.
2. Simular que la oficina aprueba una tarifa nueva con vigencia desde hoy:
   ```powershell
   docker exec chatbot-mysql-monolito mysql -uroot -proot oficina_agua -e "INSERT INTO tarifas (paja_id, monto_base, precio_m3_excedente, vigente_desde, created_at, updated_at) VALUES (2, 32.00, 4.5, CURRENT_DATE, NOW(), NOW());"
   ```
3. Reingestar: Swagger → `POST /api/v1/ingest`.
4. Repetir la pregunta → **Q32.00**. El monolito no se modificó desde la Fase 2:
   el cambio se hizo "en el monolito" y el chatbot solo lo leyó.

## 6. Historial de conversaciones (DynamoDB)

Cada pregunta se guarda en **DynamoDB Local** (tabla `chatbot_conversaciones`):
pregunta, respuesta, **tipo** (`RAG`, `CALCULO`, `PRIVACIDAD`, `SIN_INFORMACION`),
fuentes, latencia, hash de la IP y fecha de expiración (TTL de 90 días).

- **Verlo en el demo:** http://localhost:8001 (dynamodb-admin) → tabla `chatbot_conversaciones`.
- Se guarda en **otro hilo**: el usuario no espera a DynamoDB, y si DynamoDB
  está caído el chat sigue funcionando (solo queda un aviso en el log).
- **Privacidad:** la IP no se guarda, solo un hash SHA-256 recortado.
- Para apagarlo: `HISTORIAL_ENABLED=false` en `.env`.

---

## 7. Usar el chatbot como LIBRERÍA en otro proyecto

`mvn package` produce dos jars en `chatbot-service/target/`:

| Jar | Para qué |
|---|---|
| `chatbot-rag-1.0.0.jar` | **Librería**: la que otro proyecto agrega en su `pom.xml` |
| `chatbot-rag-1.0.0-exec.jar` | **Aplicación**: se ejecuta sola (`java -jar`). La usa el `Dockerfile` |

### 7.1 Instalar la librería en el repositorio Maven local (`~/.m2`)

Desde `chatbot-service/` (sin instalar Maven, con Docker):

```powershell
docker run --rm -v "${PWD}:/app" -v "$env:USERPROFILE\.m2:/root/.m2" -w /app maven:3.9-eclipse-temurin-17 mvn -q install -DskipTests
```

Con Maven instalado basta `mvn install -DskipTests`. Cada integrante que vaya a
compilar el proyecto anfitrión debe hacer esto una vez (o cuando cambie la versión).

### 7.2 Agregar la dependencia en el proyecto anfitrión

```xml
<dependency>
    <groupId>gt.edu.umg.desarrolloweb</groupId>
    <artifactId>chatbot-rag</artifactId>
    <version>1.0.0</version>
</dependency>
```

Requisitos del anfitrión: **Spring Boot 3.5 + Java 17** (Spring MVC, no WebFlux).

**No hace falta `@ComponentScan` ni `@Import`**: la librería se auto-configura
(`META-INF/spring/...AutoConfiguration.imports`). Expone `POST /api/v1/chat` y
`POST /api/v1/ingest` dentro de la aplicación anfitriona.

### 7.3 Configuración mínima en el `application.yml` del anfitrión

Todo tiene valores por defecto (`chatbot-defaults.properties`); solo se cambia lo necesario:

```yaml
chatbot:
  vector-db:                       # Postgres + pgvector del chatbot (NO la BD del anfitrión)
    url: jdbc:postgresql://localhost:5433/chatbot
    username: chatbot
    password: chatbot
  monolito:                        # réplica del monolito, solo lectura
    enabled: true
    url: jdbc:mysql://HOST:3306/aqua_gest?allowPublicKeyRetrieval=true
    username: consultas_replica
    password: ${MONOLITO_DB_PASSWORD}
  historial:
    enabled: true
    endpoint: http://localhost:8000   # DynamoDB Local
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
```

La infraestructura sigue haciendo falta: **Postgres+pgvector, Ollama y DynamoDB Local**.
Se pueden levantar con este mismo compose:
`docker compose up -d postgres-vector ollama dynamodb-local`.

### 7.4 Diseño para convivir con el proyecto anfitrión

| Riesgo al ser librería | Cómo se evitó |
|---|---|
| Un `DataSource` propio apagaría la auto-configuración de la BD del anfitrión | Las conexiones van encapsuladas en `ChatbotBasesDeDatos`; no se publica ningún `DataSource` ni `JdbcTemplate` |
| El starter de pgvector usaría la BD del anfitrión | Se usa `spring-ai-pgvector-store` sin starter y el vector store se arma con la conexión propia |
| El `@RestControllerAdvice` atraparía los errores de todo el anfitrión | `assignableTypes = {ChatController, IngestaController}` |
| Un `application.yml` en el jar se mezclaría con el del anfitrión | La librería no trae `application.yml`: sus defaults tienen la prioridad más baja |
| Postgres o el monolito caídos tumbarían el arranque del anfitrión | Pools perezosos y esquema de pgvector inicializado por la ingesta, con manejo de errores |
| Choque de nombres de configuración | Todo bajo el prefijo `chatbot.` |

Para apagar el chatbot sin quitar la dependencia: `chatbot.enabled=false`.

> Si el proyecto anfitrión **no** usa base de datos propia, agregue
> `spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration`
> (la librería trae `spring-jdbc` y Spring Boot intentaría configurar una BD por defecto).

## 8. Estructura

```
feature2-chatbot/
├── docker-compose.yml                    # pgvector, ollama, mysql de prueba, dynamodb, chatbot, web
├── .env.example
├── db/                                   # MySQL de prueba (estructura + seed + usuario RO) — ver db/README.md
├── web-publica/                          # Next.js: página pública + BFF + widget
└── chatbot-service/                      # → chatbot-rag.jar (librería) + chatbot-rag-exec.jar (app)
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── resources/
        │   ├── chatbot-defaults.properties   # defaults de la librería (prioridad más baja)
        │   ├── application-chatbot-standalone.yml  # solo modo aplicación (Docker)
        │   ├── META-INF/                     # registro de la auto-configuración
        │   ├── static/openapi/chatbot-api.yaml   # EL CONTRATO
        │   └── conocimiento/*.md             # FAQ = fuente 1 del RAG
        └── java/
            ├── chatbotrag/standalone/ChatbotApplication.java  # main del modo aplicación (fuera
            │                                    #   de gt.edu.umg... para que el anfitrión no la escanee)
            └── gt/edu/umg/desarrolloweb/chatbot/
                ├── autoconfigure/        # ChatbotAutoConfiguration + defaults
                ├── config/               # propiedades, conexiones encapsuladas, pgvector
                ├── controller/           # implementan ChatApi / IngestaApi + errores
                ├── service/              # ChatService (RAG), IngestaService, guardias
                ├── conocimiento/         # FAQ, lectores del monolito, calculadora
                ├── historial/            # DynamoDB
                ├── ratelimit/            # 429 por IP
                └── exception/
```

## 9. Pendientes

- [x] Estructura real de la BD heredada → seed + lectores de tarifas, formas de pago y sectores
- [x] Next.js BFF: `/api/chat` + widget en la página pública
- [ ] Integrar la página de la Feature 1 en `web-publica`
- [x] Historial de pregunta/respuesta en DynamoDB
- [x] Empaquetado como librería Maven (`chatbot-rag`)
- [ ] Integrar este compose al `docker-compose.yml` único del equipo
