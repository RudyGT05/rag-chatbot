import "server-only";

/**
 * URLs de los microservicios Java. Este archivo SOLO se ejecuta en el servidor
 * de Next.js ("server-only" hace fallar el build si algún componente del
 * navegador lo importa). Así el navegador nunca conoce estas direcciones:
 * ese es el papel del BFF (Backend-for-Frontend).
 *
 * Para integrar la Feature 1, agregar aquí CONSULTA_SERVICE_URL.
 */
export const servicios = {
  chatbot: process.env.CHATBOT_SERVICE_URL ?? "http://localhost:8082",
  // consulta: process.env.CONSULTA_SERVICE_URL ?? "http://localhost:8081",
} as const;
