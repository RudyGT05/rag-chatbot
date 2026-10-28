# web-publica — Página pública + BFF (Next.js)

Es el **único** servicio que ve el navegador. Hace dos papeles:

1. **Página pública** (React): inicio, consulta pública (Feature 1) y el widget del chat (Feature 2).
2. **BFF** (*Backend-for-Frontend*): rutas `app/api/*` que corren en el servidor y llaman
   a los microservicios Java. El navegador nunca conoce sus URLs ni ninguna API key.

```
Navegador ──► /api/chat (route.ts, servidor Next) ──► chatbot-service:8082 /api/v1/chat
          ──► /api/consulta/... (Feature 1)      ──► consulta-service (pendiente)
```

## Correr en desarrollo (sin Docker)

```bash
cd web-publica
npm install
copy .env.example .env.local      # en PowerShell; en Linux/Mac: cp
npm run dev
```

Abre http://localhost:3000. Necesita `chatbot-service` corriendo en el 8082
(`docker compose up -d` en la carpeta raíz).

## Estructura

```
app/
├── layout.tsx              # encabezado, pie y <ChatWidget/> (aparece en todas las páginas)
├── page.tsx                # inicio
├── consulta/page.tsx       # ⚠ RESERVADO para la Feature 1
└── api/chat/route.ts       # BFF de la Feature 2
components/chat/            # widget del chat (corre en el navegador)
lib/
├── servicios.ts            # URLs de los microservicios (solo servidor)
└── ip-cliente.ts           # IP real del usuario para el rate limit
```

## Qué hace el BFF `/api/chat`

| Caso | Respuesta al navegador |
|---|---|
| Pregunta válida | 200 con `answer` y `sources` (mismo contrato que el microservicio) |
| Pregunta de menos de 3 o más de 500 caracteres, o JSON roto | 400 `VALIDATION_ERROR`, sin molestar al microservicio |
| Rate limit del microservicio | 429 y reenvía `Retry-After` |
| LLM caído | 503 `LLM_UNAVAILABLE` |
| `chatbot-service` apagado | 503 `CHATBOT_UNREACHABLE` |
| Tardó más de 90 s | 504 `CHATBOT_TIMEOUT` |
| `POST /api/v1/ingest` | 404: el BFF no lo expone |

Además reenvía la IP real del usuario en `X-Forwarded-For`. Sin eso, todos los
usuarios compartirían el mismo rate limit, porque al microservicio todas las
peticiones le llegan desde el servidor de Next.js.

## Integrar la Feature 1 (consulta pública con captcha)

1. **Página:** reemplazar `app/consulta/page.tsx` con el formulario (código de
   contador + captcha). Si usa hooks (`useState`), va en un componente con `"use client"`.
2. **BFF:** crear `app/api/consulta/[meterCode]/route.ts` copiando el patrón de
   `app/api/chat/route.ts`:
   - validar el captcha **aquí en el servidor** (la clave secreta del captcha
     va en una variable de entorno sin `NEXT_PUBLIC_`),
   - llamar a `${servicios.consulta}/api/v1/public/accounts/{meterCode}`,
   - reenviar `X-Forwarded-For` con `ipCliente(request)`.
3. **`lib/servicios.ts`:** descomentar la línea `consulta`.
4. **`docker-compose.yml`:** agregar `CONSULTA_SERVICE_URL: http://consulta-service:8081`
   al servicio `web-publica`.

No hace falta tocar nada del chat: el widget vive en el layout y aparece solo
en la página de consulta.
