# Chatbot — Cómo agregarlo a tu página Next.js

Esto agrega el botón flotante del chat a tu página. Funciona con **cualquier**
backend del chatbot: la librería dentro de tu Spring Boot, o el `chatbot-service`
corriendo aparte.

## 1. Copiar los archivos

La carpeta `web/` de esta entrega tiene la misma estructura que un proyecto
Next.js (App Router). Copia su contenido en la raíz de tu proyecto:

| Archivo | Qué es |
|---|---|
| `components/chat/ChatWidget.tsx` | El botón flotante y la ventana del chat |
| `components/chat/ChatWidget.module.css` | Sus estilos (trae sus propios colores; no toca los tuyos) |
| `app/api/chat/route.ts` | El BFF: recibe la pregunta del navegador y la reenvía al backend |
| `lib/servicios.ts` | La URL del backend del chatbot (solo en el servidor) |
| `lib/ip-cliente.ts` | Reenvía la IP real del usuario (para el límite de preguntas) |

Si ya tienes un `lib/servicios.ts`, no lo reemplaces: solo agrega la línea
`chatbot: process.env.CHATBOT_SERVICE_URL ?? "http://localhost:8082"`.

Los imports usan el alias `@/` (el que crea `create-next-app` por defecto). Si tu
proyecto no lo tiene, cámbialos por rutas relativas.

## 2. Instalar la dependencia

```powershell
npm install server-only
```

Hace fallar el build si algún componente del navegador importa `lib/servicios.ts`.
Así la dirección del backend nunca llega al navegador.

## 3. Mostrar el botón

En `app/layout.tsx`:

```tsx
import ChatWidget from "@/components/chat/ChatWidget";

// dentro de <body>, al final:
<ChatWidget />
```

## 4. Decirle dónde está el backend

En `.env.local` (o en el `environment` de tu servicio en docker-compose):

```
CHATBOT_SERVICE_URL=http://localhost:8082
```

| Si el chatbot corre... | `CHATBOT_SERVICE_URL` |
|---|---|
| Como `chatbot-service` aparte, en tu PC | `http://localhost:8082` |
| Como librería dentro de tu Spring Boot | La URL de tu Spring Boot, ej. `http://localhost:8080` |
| En Docker, en la misma red que el Next.js | El nombre del servicio, ej. `http://chatbot-service:8082` |

## 5. Probar

`npm run dev`, abre la página y pregunta "¿cuánto cuesta la media paja?".

Si alguien pregunta por el saldo de su cuenta, el chatbot no responde datos
personales: le indica que use la sección "Consulta pública" (Feature 1).

Probado con Next.js 16.4 y React 19.2.
