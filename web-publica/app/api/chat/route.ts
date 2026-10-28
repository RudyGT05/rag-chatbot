import { NextResponse } from "next/server";
import { ipCliente } from "@/lib/ip-cliente";
import { servicios } from "@/lib/servicios";

/**
 * BFF del chatbot:  navegador → POST /api/chat (aquí) → chatbot-service
 *
 * ¿Por qué no llamar al microservicio directo desde el navegador?
 *  1. La URL del microservicio y cualquier API key quedan solo en el servidor.
 *  2. Aquí se valida y se limita lo que el navegador puede pedir: solo
 *     preguntas, nunca POST /api/v1/ingest u otros endpoints internos.
 *  3. Se reenvía la IP real para el rate limit del microservicio.
 */

export const dynamic = "force-dynamic"; // nunca cachear respuestas del chat

const MIN = 3;
const MAX = 500;
const TIMEOUT_MS = 90_000; // Ollama sin GPU puede tardar en la primera pregunta

type ErrorBody = { code: string; message: string; details?: string[] };

function error(status: number, code: string, message: string, details: string[] = []) {
  return NextResponse.json<ErrorBody>({ code, message, details }, { status });
}

export async function POST(request: Request) {
  // ---- 1. Validar lo que manda el navegador (misma regla que el contrato) ----
  let question: unknown;
  try {
    ({ question } = await request.json());
  } catch {
    return error(400, "VALIDATION_ERROR", "El cuerpo de la solicitud no es un JSON válido");
  }
  if (typeof question !== "string" || question.trim().length < MIN || question.trim().length > MAX) {
    return error(400, "VALIDATION_ERROR", `La pregunta debe tener entre ${MIN} y ${MAX} caracteres`);
  }

  // ---- 2. Reenviar al microservicio ----
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  const ip = ipCliente(request);
  if (ip) headers["X-Forwarded-For"] = ip;

  let respuesta: Response;
  try {
    respuesta = await fetch(`${servicios.chatbot}/api/v1/chat`, {
      method: "POST",
      headers,
      body: JSON.stringify({ question: question.trim() }),
      signal: AbortSignal.timeout(TIMEOUT_MS),
      cache: "no-store",
    });
  } catch (e) {
    const timeout = e instanceof Error && e.name === "TimeoutError";
    console.error("[BFF /api/chat] chatbot-service no respondió:", e);
    return error(
      timeout ? 504 : 503,
      timeout ? "CHATBOT_TIMEOUT" : "CHATBOT_UNREACHABLE",
      "El asistente no está disponible en este momento.",
    );
  }

  // ---- 3. Devolver la respuesta tal cual (mismo contrato), con Retry-After si viene ----
  const cuerpo = await respuesta.text();
  const salida = new NextResponse(cuerpo, {
    status: respuesta.status,
    headers: { "Content-Type": "application/json" },
  });
  const retryAfter = respuesta.headers.get("Retry-After");
  if (retryAfter) salida.headers.set("Retry-After", retryAfter);
  return salida;
}
