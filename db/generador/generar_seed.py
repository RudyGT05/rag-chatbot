#!/usr/bin/env python3
"""
Genera db/02-seed-data.sql: datos FICTICIOS y coherentes para la BD de prueba
del monolito (estructura en db/01-estructura.sql).

    python generar_seed.py            → reescribe ../02-seed-data.sql

Es determinista (semilla fija): siempre produce el mismo archivo, así todo el
equipo prueba con los mismos datos.

Coherencia que respeta (igual que haría el monolito):
  - lecturas encadenadas: lectura_anterior = lectura_actual del mes previo
  - 1 boleta por lectura de período CERRADO, con la tarifa vigente al cierre
  - monto = monto_base + max(0, consumo − equivalencia_m3) × precio_m3_excedente
  - folios correlativos por serie, y series/correlativos con su siguiente número
  - pagos completos, un reverso con su pago corregido y clientes en mora

Ningún dato es real: nombres inventados, teléfonos 5550-xxxx, correos *.test,
sin NIT ni DPI.
"""
from __future__ import annotations

import random
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path

random.seed(2026)

HOY = date(2026, 10, 22)
CREADO = "2026-09-15 08:00:00"
# Contraseña de TODOS los usuarios de prueba: Prueba2026!  (bcrypt, cost 12)
PASSWORD_HASH = "$2y$12$WX7Xdwscs31awNCLMD/6IemNbWsPjk3dwDIDyVPBjdLpZrwGE4nw."
MODELO_USUARIO = "App\\\\Models\\\\User"  # → App\Models\User dentro del literal SQL

D = Decimal


def q2(x: Decimal) -> Decimal:
    return x.quantize(D("0.01"), rounding=ROUND_HALF_UP)


def sql(v) -> str:
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, Decimal)):
        return str(v)
    if isinstance(v, (date, datetime)):
        return f"'{v}'"
    if isinstance(v, Raw):
        return v.texto
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


@dataclass
class Raw:
    texto: str


def insert(tabla: str, columnas: list[str], filas: list[list]) -> str:
    if not filas:
        return f"-- {tabla}: sin filas\n"
    cuerpo = ",\n".join("  (" + ", ".join(sql(v) for v in f) + ")" for f in filas)
    return f"INSERT INTO {tabla} ({', '.join(columnas)}) VALUES\n{cuerpo};\n"


# ----------------------------------------------------------------------------
# Catálogos
# ----------------------------------------------------------------------------
USUARIOS = [  # id, nombre, email, rol
    (1, "Ana Administradora", "admin@oficina-agua.test", "Administrador"),
    (2, "Sofía Secretaria", "secretaria@oficina-agua.test", "Secretario"),
    (3, "Luis Lector (Norte)", "lector1@oficina-agua.test", "Operario"),
    (4, "Marta Lectora (Sur)", "lector2@oficina-agua.test", "Operario"),
    (5, "Portal — Cliente 1", "cliente1@correo.test", "Cliente"),
    (6, "Portal — Cliente 2", "cliente2@correo.test", "Cliente"),
]
ROLES = ["Administrador", "Secretario", "Operario", "Cliente"]

SECTORES = [  # id, nombre, descripcion, orden, activo, aldea, lector
    (1, "Aldea El Porvenir", "Casco de la aldea y alrededores de la escuela", 1, True, "El Porvenir", 3),
    (2, "Sector Centro", "Calles alrededor del parque central", 2, True, "Centro", 3),
    (3, "Caserío Los Pinos", "Camino a Los Pinos, después del puente", 3, True, "Los Pinos", 4),
    (4, "Sector La Laguna", "Ampliación en estudio; aún sin red", 4, False, "La Laguna", None),
]

PAJAS = [  # id, nombre, equivalencia_m3, activo
    (1, "1 paja", D("60.00"), True),
    (2, "1/2 paja", D("30.00"), True),
    (3, "1/4 paja", D("15.00"), True),
    (4, "2 pajas", D("120.00"), False),  # descontinuada: no debe salir en el RAG
]

TARIFAS = [  # id, paja_id, monto_base, precio_m3_excedente, vigente_desde
    (1, 1, D("40.00"), D("3.5000"), date(2026, 1, 1)),
    (2, 2, D("25.00"), D("3.5000"), date(2026, 1, 1)),
    (3, 3, D("15.00"), D("3.5000"), date(2026, 1, 1)),
    (4, 4, D("75.00"), D("3.5000"), date(2026, 1, 1)),
    (5, 1, D("45.00"), D("4.0000"), date(2026, 7, 1)),
    (6, 2, D("28.00"), D("4.0000"), date(2026, 7, 1)),
    (7, 3, D("17.00"), D("4.0000"), date(2026, 7, 1)),
    (8, 1, D("50.00"), D("4.0000"), date(2027, 1, 1)),  # FUTURA: prueba "próxima vigencia"
]

METODOS_PAGO = [  # id, codigo, nombre, requiere_referencia, activo
    (1, "EFECTIVO", "Efectivo", False, True),
    (2, "TARJETA", "Tarjeta de débito o crédito", False, True),
    (3, "DEPOSITO", "Depósito bancario", True, True),
    (4, "CHEQUE", "Cheque", True, False),  # ya no se acepta
]

TIPOS_DOCUMENTO = [  # id, codigo, nombre, respalda_predio
    (1, "dpi", "Documento Personal de Identificación (DPI)", False),
    (2, "nit", "Constancia de NIT", False),
    (3, "escritura", "Escritura de propiedad", True),
    (4, "recibo_luz", "Recibo de luz del inmueble", True),
    (5, "contrato", "Contrato de servicio firmado", False),
]

NOMBRES = [
    "María Elena Pérez", "José Antonio López", "Rosa Amalia Hernández", "Carlos Enrique Morales",
    "Ana Lucía García", "Juan Pablo Ramírez", "Marta Julia Castillo", "Pedro Luis Méndez",
    "Gloria Esperanza Ruiz", "Miguel Ángel Orellana", "Silvia Patricia Monzón", "Óscar René Aguilar",
    "Teresa de Jesús Cruz", "Francisco Javier Ortiz", "Lidia Marisol Barrios", "Héctor Manuel Solís",
    "Carmen Rosa Vásquez", "Rubén Darío Estrada", "Elsa Margarita Pineda", "Mario Roberto Chávez",
    "Irma Yolanda Recinos", "Edgar Alfonso Galindo", "Norma Leticia Fajardo", "Raúl Alberto Cifuentes",
]
REFERENCIAS = [
    "frente a la tienda", "a la par de la iglesia", "esquina con portón verde", "casa de block sin repello",
    "atrás de la escuela", "cerca del molino", "casa con corredor amarillo", "frente al campo de fútbol",
]
CONSUMO_POR_PAJA = {1: (38, 70), 2: (16, 36), 3: (7, 20)}


def main() -> None:
    out: list[str] = [
        "-- =============================================================================",
        "-- Datos FICTICIOS para la BD de prueba del monolito.",
        "-- Generado por db/generador/generar_seed.py — no editar a mano: regenerar.",
        "-- Usuarios de prueba: contraseña Prueba2026! (solo para desarrollo local).",
        "-- =============================================================================",
        "SET NAMES utf8mb4;",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "",
    ]

    # ---- usuarios y roles ----
    out.append(insert("users",
        ["id", "name", "email", "email_verified_at", "activo", "password", "created_at", "updated_at"],
        [[i, n, e, CREADO, True, PASSWORD_HASH, CREADO, CREADO] for i, n, e, _ in USUARIOS]))
    out.append(insert("roles", ["id", "name", "guard_name", "created_at", "updated_at"],
        [[i + 1, r, "web", CREADO, CREADO] for i, r in enumerate(ROLES)]))
    out.append(insert("model_has_roles", ["role_id", "model_type", "model_id"],
        [[ROLES.index(r) + 1, Raw(f"'{MODELO_USUARIO}'"), i] for i, _, _, r in USUARIOS]))

    # ---- sectores y lectores ----
    out.append(insert("sectores", ["id", "nombre", "descripcion", "orden", "activo", "created_at", "updated_at"],
        [[s[0], s[1], s[2], s[3], s[4], CREADO, CREADO] for s in SECTORES]))
    out.append(insert("lector_sectores", ["user_id", "sector_id", "created_at", "updated_at"],
        [[s[6], s[0], CREADO, CREADO] for s in SECTORES if s[6]]))

    # ---- catálogos ----
    out.append(insert("pajas", ["id", "nombre", "equivalencia_m3", "activo", "created_at", "updated_at"],
        [[*p, CREADO, CREADO] for p in PAJAS]))
    out.append(insert("tarifas",
        ["id", "paja_id", "monto_base", "precio_m3_excedente", "vigente_desde", "created_at", "updated_at"],
        [[*t, CREADO, CREADO] for t in TARIFAS]))
    out.append(insert("metodos_pago",
        ["id", "codigo", "nombre", "requiere_referencia", "activo", "created_at", "updated_at"],
        [[*m, CREADO, CREADO] for m in METODOS_PAGO]))
    out.append(insert("tipos_documento",
        ["id", "codigo", "nombre", "respalda_predio", "activo", "created_at", "updated_at"],
        [[*t, True, CREADO, CREADO] for t in TIPOS_DOCUMENTO]))

    # ---- clientes, predios, contadores ----
    activos = [s for s in SECTORES if s[4]]
    clientes, predios, contadores = [], [], []
    inactivos = {23, 24}                 # clientes dados de baja: sin lecturas
    con_dos_contadores = {4, 11}         # clientes con un segundo predio/contador
    for cid, nombre in enumerate(NOMBRES, start=1):
        sector = activos[(cid - 1) % len(activos)]
        estado = "inactivo" if cid in inactivos else "activo"
        clientes.append([cid, f"CLI-{cid:04d}", nombre, f"5550-{1000 + cid * 37:04d}",
                         f"cliente{cid:02d}@correo.test", f"{sector[5]}, casa {cid}-{10 + cid}",
                         estado, CREADO, CREADO])
        for extra in ([0, 1] if cid in con_dos_contadores else [0]):
            pid = len(predios) + 1
            predios.append([pid, sector[0], sector[5], "0", f"{1 + (pid % 5)}a. calle",
                            f"{cid}-{10 + pid}", random.choice(REFERENCIAS), CREADO, CREADO])
            paja = random.choices([1, 2, 3], weights=[2, 5, 2])[0]
            ctr = len(contadores) + 1
            if cid in inactivos:
                estado_ctr = "inactivo"
            elif ctr == 9:
                estado_ctr = "dañado"    # se dañó en septiembre: sin lectura ese mes
            else:
                estado_ctr = "activo"
            contadores.append({"id": ctr, "predio": pid, "cliente": cid, "paja": paja,
                               "codigo": f"CTR-{ctr:04d}", "estado": estado_ctr, "sector": sector[0],
                               "lector": sector[6],
                               "instalado": date(2026, 1 + (ctr % 4), 5 + (ctr % 20))})

    out.append(insert("clientes",
        ["id", "codigo", "nombre", "telefono", "email", "direccion_notificacion", "estado",
         "created_at", "updated_at"], clientes))
    out.append(insert("predios",
        ["id", "sector_id", "aldea", "zona", "calle", "numero_casa", "referencia", "created_at", "updated_at"],
        predios))
    out.append(insert("contadores",
        ["id", "predio_id", "cliente_id", "paja_id", "codigo", "fecha_instalacion", "estado",
         "created_at", "updated_at"],
        [[c["id"], c["predio"], c["cliente"], c["paja"], c["codigo"], c["instalado"], c["estado"],
          CREADO, CREADO] for c in contadores]))

    # ---- acceso al portal de 2 clientes ----
    out.append(insert("cliente_accesos",
        ["cliente_id", "user_id", "otorgado_por", "otorgado_en", "created_at", "updated_at"],
        [[1, 5, 2, "2026-09-20 10:00:00", "2026-09-20 10:00:00", "2026-09-20 10:00:00"],
         [2, 6, 2, "2026-09-20 10:05:00", "2026-09-20 10:05:00", "2026-09-20 10:05:00"]]))

    # ---- períodos: mayo–septiembre cerrados, octubre abierto ----
    periodos = []
    for pid, mes in enumerate(range(5, 11), start=1):
        inicio = date(2026, mes, 1)
        fin = (date(2026, mes + 1, 1) if mes < 12 else date(2027, 1, 1)) - timedelta(days=1)
        cerrado = mes < 10
        cerrado_en = f"{fin + timedelta(days=3)} 17:00:00" if cerrado else None
        periodos.append({"id": pid, "mes": mes, "inicio": inicio, "fin": fin, "cerrado": cerrado,
                         "cerrado_en": cerrado_en})
    out.append(insert("periodos",
        ["id", "anio", "mes", "fecha_inicio", "fecha_fin", "cerrado_en", "cerrado_por", "created_at", "updated_at"],
        [[p["id"], 2026, p["mes"], p["inicio"], p["fin"], p["cerrado_en"], 1 if p["cerrado"] else None,
          CREADO, CREADO] for p in periodos]))

    # ---- lecturas encadenadas ----
    lecturas = []
    for c in contadores:
        if c["estado"] == "inactivo":
            continue
        actual = D(random.randint(150, 900))
        for p in periodos:
            if c["estado"] == "dañado" and p["mes"] >= 9:
                break
            # octubre está en curso: solo el lector del Norte ya pasó
            if not p["cerrado"] and c["lector"] != 3:
                continue
            lo, hi = CONSUMO_POR_PAJA[c["paja"]]
            consumo = D(random.randint(lo, hi)) + D(random.randint(0, 99)) / 100
            if random.random() < 0.12:  # mes con consumo alto (fuga, visitas…)
                consumo += D(random.randint(15, 30))
            anterior, actual = actual, actual + consumo
            fecha = p["fin"] - timedelta(days=random.randint(1, 5)) if p["cerrado"] \
                else date(2026, 10, random.randint(19, 21))
            obs = "Consumo alto; se recomendó revisar fugas" if consumo > hi + 10 else None
            if c["estado"] == "dañado" and p["mes"] == 8:
                obs = "Vidrio del contador quebrado; se reportó para cambio"
            lecturas.append({"id": len(lecturas) + 1, "contador": c, "periodo": p, "anterior": anterior,
                             "actual": actual, "consumo": consumo, "fecha": fecha, "obs": obs})
    out.append(insert("lecturas",
        ["id", "contador_id", "periodo_id", "usuario_id", "lectura_anterior", "lectura_actual",
         "fecha_lectura", "observaciones", "created_at", "updated_at"],
        [[l["id"], l["contador"]["id"], l["periodo"]["id"], l["contador"]["lector"], q2(l["anterior"]),
          q2(l["actual"]), l["fecha"], l["obs"], f"{l['fecha']} 12:00:00", f"{l['fecha']} 12:00:00"]
         for l in lecturas]))

    # ---- series de documentos ----
    def folio(prefijo: str, n: int) -> str:
        return f"{prefijo}-2026-{n:06d}"

    # ---- boletas (solo períodos cerrados) ----
    def tarifa_vigente(paja: int, al: date):
        candidatas = [t for t in TARIFAS if t[1] == paja and t[4] <= al]
        return max(candidatas, key=lambda t: t[4])

    equivalencia = {p[0]: p[2] for p in PAJAS}
    boletas = []
    for l in lecturas:
        p = l["periodo"]
        if not p["cerrado"]:
            continue
        c = l["contador"]
        t = tarifa_vigente(c["paja"], p["fin"])
        consumo = q2(l["consumo"])
        exced = q2(max(D(0), consumo - equivalencia[c["paja"]]) * t[3])
        emision = p["fin"] + timedelta(days=2)
        b = {"id": len(boletas) + 1, "lectura": l, "cliente": c["cliente"], "tarifa": t, "periodo": p,
             "consumo": consumo, "base": t[2], "exced": exced, "monto": t[2] + exced,
             "emision": emision, "vence": emision + timedelta(days=20), "anulada": False}
        b["folio"] = folio("B", b["id"])
        boletas.append(b)
    # una boleta anulada (error de emisión)
    anulada = next(b for b in boletas if b["periodo"]["mes"] == 6 and b["cliente"] == 7)
    anulada["anulada"] = True

    out.append(insert("boletas",
        ["id", "serie_id", "ejercicio", "numero", "folio", "cliente_id", "lectura_id", "tarifa_id", "periodo_id",
         "consumo_m3", "monto_base", "monto_excedente", "monto", "fecha_emision", "fecha_vencimiento",
         "impresa_en", "anulada_en", "anulada_por", "motivo_anulacion", "created_at", "updated_at"],
        [[b["id"], 1, 2026, b["id"], b["folio"], b["cliente"], b["lectura"]["id"], b["tarifa"][0],
          b["periodo"]["id"], b["consumo"], b["base"], b["exced"], b["monto"], b["emision"], b["vence"],
          f"{b['emision']} 09:00:00",
          f"{b['emision'] + timedelta(days=1)} 11:30:00" if b["anulada"] else None,
          1 if b["anulada"] else None,
          "Emitida por error: el contador estaba en revisión" if b["anulada"] else None,
          f"{b['emision']} 08:00:00", f"{b['emision']} 08:00:00"] for b in boletas]))

    # ---- pagos ----
    en_mora = {5, 14, 19}  # no pagan desde julio
    pagos = []

    def pagar(b, fecha, metodo, revertido=None):
        n = len(pagos) + 1
        ref = f"DEP-{random.randint(100000, 999999)}" if metodo == 3 else None
        pagos.append([n, 2, 2026, n, folio("R", n), b["id"], metodo, 2, b["monto"], fecha, ref,
                      revertido[0] if revertido else None, 1 if revertido else None,
                      revertido[1] if revertido else None, f"{fecha} 10:00:00", f"{fecha} 10:00:00"])

    for b in boletas:
        if b["anulada"]:
            continue
        mes = b["periodo"]["mes"]
        if b["cliente"] in en_mora and mes >= 7:
            continue
        if mes == 9 and random.random() < 0.35:  # septiembre: aún pendientes
            continue
        fecha = b["emision"] + timedelta(days=random.randint(1, 18))
        if fecha > HOY:
            continue
        metodo = random.choices([1, 2, 3], weights=[7, 2, 1])[0]
        if b["cliente"] == 3 and mes == 8:
            # pago aplicado por error y revertido; luego el pago correcto
            pagar(b, fecha, 1, revertido=(f"{fecha} 16:00:00", "Monto cobrado de más; se registró de nuevo"))
            pagar(b, fecha + timedelta(days=1), 1)
            continue
        pagar(b, fecha, metodo)

    out.append(insert("pagos",
        ["id", "serie_id", "ejercicio", "numero", "folio", "boleta_id", "metodo_pago_id", "usuario_id", "monto",
         "fecha_pago", "referencia", "revertido_en", "revertido_por", "motivo_reverso", "created_at", "updated_at"],
        pagos))

    out.append(insert("series_documento",
        ["id", "tipo_documento", "codigo", "prefijo", "separador", "incluye_anio", "longitud_numero",
         "reinicia_cada_anio", "ejercicio", "siguiente_numero", "activa", "created_at", "updated_at"],
        [[1, "boleta", "BOL", "B", "-", True, 6, True, 2026, len(boletas) + 1, True, CREADO, CREADO],
         [2, "recibo_pago", "REC", "R", "-", True, 6, True, 2026, len(pagos) + 1, True, CREADO, CREADO]]))

    out.append(f"UPDATE correlativos SET siguiente_numero = {len(clientes) + 1} WHERE clave = 'cliente';")
    out.append(f"UPDATE correlativos SET siguiente_numero = {len(contadores) + 1} WHERE clave = 'contador';")
    out.append("")
    out.append("SET FOREIGN_KEY_CHECKS = 1;")

    destino = Path(__file__).resolve().parent.parent / "02-seed-data.sql"
    destino.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"{destino.name}: {len(clientes)} clientes, {len(contadores)} contadores, {len(lecturas)} lecturas, "
          f"{len(boletas)} boletas ({sum(b['anulada'] for b in boletas)} anulada), {len(pagos)} pagos")


if __name__ == "__main__":
    main()
