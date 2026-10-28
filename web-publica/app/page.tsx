import Link from "next/link";

export default function Inicio() {
  return (
    <>
      <section className="portada">
        <div className="contenedor">
          <h1>Tu servicio de agua, claro y a la mano</h1>
          <p>
            Consulta el estado de cuenta de tu contador sin crear un usuario, o pregúntale a nuestro
            asistente sobre tarifas, pagos, trámites y qué hacer ante una fuga.
          </p>
          <div className="acciones">
            <Link href="/consulta" className="boton boton-primario">
              Consultar mi cuenta
            </Link>
            <a href="#ayuda" className="boton boton-secundario">
              Ver temas de ayuda
            </a>
          </div>
        </div>
      </section>

      <section className="seccion" id="ayuda">
        <div className="contenedor">
          <div className="tarjetas">
            <article className="tarjeta">
              <span className="etiqueta">Pagos</span>
              <h3>¿Dónde y cómo pagar?</h3>
              <p>Ventanilla de la oficina con efectivo, tarjeta o depósito bancario.</p>
            </article>
            <article className="tarjeta">
              <span className="etiqueta">Consumo</span>
              <h3>¿Cómo se calcula mi recibo?</h3>
              <p>Lectura actual menos la anterior, según la tarifa vigente de tu contador.</p>
            </article>
            <article className="tarjeta">
              <span className="etiqueta">Emergencias</span>
              <h3>¿Tienes una fuga?</h3>
              <p>Cierra la llave de paso junto al contador y repórtala si está en la vía pública.</p>
            </article>
          </div>
          <p style={{ marginTop: 28, color: "var(--tinta-suave)" }}>
            ¿Otra duda? Usa el asistente virtual en la esquina inferior derecha.
          </p>
        </div>
      </section>
    </>
  );
}
