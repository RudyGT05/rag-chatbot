import "server-only";

/**
 * IP real del usuario que hizo la petición al BFF.
 *
 * Los microservicios aplican rate limit por IP. Pero las peticiones les llegan
 * desde ESTE servidor, no desde el navegador; si no reenviáramos la IP real,
 * todos los usuarios compartirían el mismo límite. Por eso el BFF la manda en
 * el header X-Forwarded-For.
 *
 * Lo usan la Feature 2 (chat) y lo usará la Feature 1 (consulta pública).
 */
export function ipCliente(request: Request): string | null {
  const xff = request.headers.get("x-forwarded-for");
  if (xff) {
    return xff.split(",")[0].trim();
  }
  return request.headers.get("x-real-ip");
}
