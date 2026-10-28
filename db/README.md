# db/ — MySQL de prueba del monolito

El contenedor `mysql-monolito` ejecuta estos scripts **la primera vez** que se
crea su volumen, en orden alfabético:

| Archivo | Qué es |
|---|---|
| `01-estructura.sql` | Traducción a SQL de las **31 migraciones Laravel** del monolito heredado (34 tablas + vista `tarifas_vigencia`) |
| `02-seed-data.sql` | Datos **ficticios** y coherentes. Lo genera `generador/generar_seed.py` |
| `99-usuario-chatbot.sql` | Usuario `chatbot_ro` con **mínimo privilegio**: `SELECT` solo en tarifas, pajas, métodos de pago y sectores |

> **Regla de oro:** esto es una **copia** para probar. La BD real del monolito
> no se toca; en ella solo se ejecuta `99-usuario-chatbot.sql` (con otra contraseña).

## Qué datos trae (lo que pueden usar las 4 features)

| Tabla | Filas | Notas útiles para probar |
|---|---|---|
| users | 6 | admin, secretaria, 2 lectores, 2 clientes del portal. Contraseña: `Prueba2026!` |
| roles | 4 | Administrador, Secretario, Operario, Cliente |
| sectores | 4 | 3 activos + "Sector La Laguna" inactivo |
| pajas | 4 | 1 paja (60 m³), 1/2 (30), 1/4 (15) + "2 pajas" descontinuada |
| tarifas | 8 | Cambio de tarifa el 2026-07-01 y una **tarifa futura** de 1 paja el 2027-01-01 |
| clientes / contadores | 24 / 26 | Códigos `CLI-0001…`, `CTR-0001…`. 2 clientes inactivos, `CTR-0009` dañado |
| periodos | 6 | Mayo–septiembre 2026 cerrados, octubre abierto |
| lecturas | 135 | Encadenadas; algunas con consumo alto y excedente |
| boletas | 119 | Folios `B-2026-000001…`. 1 anulada |
| pagos | 104 | Folios `R-2026-000001…`. 1 pago revertido |
| metodos_pago | 4 | Efectivo, Tarjeta, Depósito (pide referencia) + Cheque inactivo |

Casos para el demo de la **Feature 1** (consulta pública):
- Clientes **en mora** desde julio: `CLI-0005`, `CLI-0014`, `CLI-0019`
- Cliente con **pago revertido** en agosto: `CLI-0003`
- Cliente con **boleta anulada** en junio: `CLI-0007`

## Comandos útiles

```powershell
# Entrar a MySQL de prueba
docker exec -it chatbot-mysql-monolito mysql -uroot -proot oficina_agua

# Volver a crear la BD desde cero (si cambias algún .sql)
docker compose stop mysql-monolito
docker compose rm -f mysql-monolito
docker volume rm feature2-chatbot_mysql-monolito-data
docker compose up -d mysql-monolito

# Regenerar los datos de prueba
python db/generador/generar_seed.py
```

## Si el monolito agrega una migración

Reflejar el cambio en `01-estructura.sql` (y en el generador si es una tabla
nueva con datos), y recrear el volumen.
