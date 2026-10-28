"use client";

import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import styles from "./ChatWidget.module.css";

/**
 * Widget flotante del chatbot (Feature 2).
 *
 * Corre en el NAVEGADOR ("use client"). Solo conoce la ruta /api/chat de
 * nuestro propio Next.js; nunca la URL del microservicio ni ninguna API key.
 */

type Fuente = { title: string; origin: string; snippet: string; score?: number };

type Mensaje =
  | { id: number; rol: "usuario"; texto: string }
  | { id: number; rol: "asistente"; texto: string; fuentes: Fuente[] }
  | { id: number; rol: "error"; texto: string };

const MAX = 500;

const SUGERENCIAS = [
  "¿Dónde puedo pagar?",
  "¿Qué hago si tengo una fuga?",
  "¿Cómo se calcula mi consumo?",
  "¿Cuál es el horario de atención?",
];

const BIENVENIDA =
  "¡Hola! Soy el asistente de la Oficina del Agua. Puedo ayudarte con tarifas, pagos, trámites y qué hacer ante una fuga.";

let siguienteId = 1;

/** Traduce la respuesta de error del BFF a un mensaje para el usuario. */
function mensajeDeError(status: number, cuerpo: { message?: string; details?: string[] } | null, retryAfter: string | null) {
  if (status === 429) {
    const seg = retryAfter ? ` Espera ${retryAfter} segundos.` : "";
    return `Hiciste muchas preguntas seguidas.${seg}`;
  }
  if (status === 400) {
    return cuerpo?.message ?? "La pregunta no es válida.";
  }
  if (status === 504) {
    return "El asistente tardó demasiado en responder. Intenta de nuevo.";
  }
  return "El asistente no está disponible en este momento. Intenta más tarde.";
}

export default function ChatWidget() {
  const [abierto, setAbierto] = useState(false);
  const [mensajes, setMensajes] = useState<Mensaje[]>([]);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);

  const listaRef = useRef<HTMLDivElement>(null);
  const entradaRef = useRef<HTMLTextAreaElement>(null);

  // Bajar al último mensaje cada vez que llega uno nuevo
  useEffect(() => {
    listaRef.current?.scrollTo({ top: listaRef.current.scrollHeight, behavior: "smooth" });
  }, [mensajes, enviando]);

  // Al abrir, poner el cursor en la caja de texto
  useEffect(() => {
    if (abierto) entradaRef.current?.focus();
  }, [abierto]);

  async function preguntar(pregunta: string) {
    const limpia = pregunta.trim();
    if (limpia.length < 3 || enviando) return;

    setMensajes((m) => [...m, { id: siguienteId++, rol: "usuario", texto: limpia }]);
    setTexto("");
    setEnviando(true);

    try {
      const res = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: limpia }),
      });
      const cuerpo = await res.json().catch(() => null);

      if (res.ok && cuerpo?.answer) {
        setMensajes((m) => [
          ...m,
          { id: siguienteId++, rol: "asistente", texto: cuerpo.answer, fuentes: cuerpo.sources ?? [] },
        ]);
      } else {
        setMensajes((m) => [
          ...m,
          { id: siguienteId++, rol: "error", texto: mensajeDeError(res.status, cuerpo, res.headers.get("Retry-After")) },
        ]);
      }
    } catch {
      setMensajes((m) => [
        ...m,
        { id: siguienteId++, rol: "error", texto: "No hay conexión. Revisa tu internet e intenta de nuevo." },
      ]);
    } finally {
      setEnviando(false);
      entradaRef.current?.focus();
    }
  }

  function alEnviar(e: FormEvent) {
    e.preventDefault();
    void preguntar(texto);
  }

  // Enter envía, Shift+Enter hace salto de línea
  function alTeclear(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      void preguntar(texto);
    }
    if (e.key === "Escape") setAbierto(false);
  }

  const muyCorta = texto.trim().length > 0 && texto.trim().length < 3;

  return (
    <>
      {abierto && (
        <section className={styles.panel} role="dialog" aria-label="Asistente virtual de la Oficina del Agua">
          <header className={styles.cabecera}>
            <div>
              <strong>Asistente virtual</strong>
              <span className={styles.subtitulo}>Respuestas basadas en información oficial</span>
            </div>
            <button className={styles.cerrar} onClick={() => setAbierto(false)} aria-label="Cerrar chat">
              ×
            </button>
          </header>

          <div className={styles.lista} ref={listaRef} aria-live="polite">
            <div className={`${styles.burbuja} ${styles.asistente}`}>{BIENVENIDA}</div>

            {mensajes.length === 0 && (
              <div className={styles.sugerencias}>
                {SUGERENCIAS.map((s) => (
                  <button key={s} className={styles.chip} onClick={() => void preguntar(s)}>
                    {s}
                  </button>
                ))}
              </div>
            )}

            {mensajes.map((m) => (
              <div key={m.id} className={`${styles.burbuja} ${styles[m.rol]}`}>
                {m.texto}
                {m.rol === "asistente" && m.fuentes.length > 0 && (
                  <details className={styles.fuentes}>
                    <summary>
                      Basado en {m.fuentes.length} {m.fuentes.length === 1 ? "fuente" : "fuentes"}
                    </summary>
                    <ul>
                      {m.fuentes.map((f, i) => (
                        <li key={i}>{f.title}</li>
                      ))}
                    </ul>
                  </details>
                )}
              </div>
            ))}

            {enviando && (
              <div className={`${styles.burbuja} ${styles.asistente} ${styles.escribiendo}`} aria-label="Escribiendo">
                <span />
                <span />
                <span />
              </div>
            )}
          </div>

          <form className={styles.formulario} onSubmit={alEnviar}>
            <textarea
              ref={entradaRef}
              value={texto}
              onChange={(e) => setTexto(e.target.value.slice(0, MAX))}
              onKeyDown={alTeclear}
              placeholder="Escribe tu pregunta…"
              rows={2}
              maxLength={MAX}
              disabled={enviando}
              aria-label="Tu pregunta"
            />
            <div className={styles.pieFormulario}>
              <span className={muyCorta ? styles.aviso : styles.contador}>
                {muyCorta ? "Mínimo 3 caracteres" : `${texto.length}/${MAX}`}
              </span>
              <button type="submit" className={styles.enviar} disabled={enviando || texto.trim().length < 3}>
                Enviar
              </button>
            </div>
          </form>
        </section>
      )}

      <button
        className={styles.lanzador}
        onClick={() => setAbierto((a) => !a)}
        aria-label={abierto ? "Cerrar asistente" : "Abrir asistente virtual"}
        aria-expanded={abierto}
      >
        {abierto ? (
          <span className={styles.icono}>×</span>
        ) : (
          <svg width="26" height="26" viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v8a2.5 2.5 0 0 1-2.5 2.5H10l-4.2 3.6c-.5.4-1.3.1-1.3-.6V16A2.5 2.5 0 0 1 4 13.5z"
              fill="currentColor"
            />
          </svg>
        )}
      </button>
    </>
  );
}
