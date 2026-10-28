import type { Metadata } from "next";
import Link from "next/link";
import ChatWidget from "@/components/chat/ChatWidget";
import "./globals.css";

export const metadata: Metadata = {
  title: "Oficina del Agua",
  description: "Consulta tu estado de cuenta y resuelve tus dudas sobre el servicio de agua.",
};

function Gota() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 2.5C8.5 7.2 5.5 10.6 5.5 14.5a6.5 6.5 0 0 0 13 0c0-3.9-3-7.3-6.5-12z" fill="#7cc4e0" />
      <path d="M9.2 15.2a3 3 0 0 0 3 2.9" stroke="#06283d" strokeWidth="1.6" fill="none" strokeLinecap="round" />
    </svg>
  );
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body>
        <header className="encabezado">
          <div className="contenedor">
            <Link href="/" className="marca">
              <Gota />
              Oficina del Agua
            </Link>
            <nav className="nav" aria-label="Principal">
              <Link href="/">Inicio</Link>
              {/* Feature 1: la página /consulta la reemplaza el equipo de la consulta pública */}
              <Link href="/consulta">Consulta pública</Link>
            </nav>
          </div>
        </header>

        <main>{children}</main>

        <footer className="pie">
          <div className="contenedor">Oficina del Agua · Proyecto académico UMG · Fase 2</div>
        </footer>

        {/* Feature 2: el chat se monta en el layout, así aparece en TODAS las páginas */}
        <ChatWidget />
      </body>
    </html>
  );
}
