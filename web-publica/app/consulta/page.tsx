/**
 * ⚠ LUGAR RESERVADO PARA LA FEATURE 1 (Consulta pública con captcha).
 *
 * El equipo de la Feature 1 reemplaza este archivo con su formulario
 * (código de contador + captcha) y agrega su ruta BFF en
 * app/api/consulta/[meterCode]/route.ts, siguiendo el mismo patrón que
 * app/api/chat/route.ts (lib/servicios.ts + lib/ip-cliente.ts).
 */
export default function ConsultaPublica() {
  return (
    <section className="seccion">
      <div className="contenedor">
        <h2>Consulta pública</h2>
        <div className="pendiente">
          <p>
            <strong>Próximamente:</strong> consulta el estado de cuenta de tu contador con su código.
          </p>
        </div>
      </div>
    </section>
  );
}
