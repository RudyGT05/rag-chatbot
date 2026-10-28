-- Usuario de SOLO LECTURA para el chatbot, con MÍNIMO PRIVILEGIO:
-- solo puede hacer SELECT en las tablas PÚBLICAS que usa el RAG.
-- Ni siquiera puede leer clientes, lecturas, boletas ni pagos: aunque hubiera
-- un bug en el chatbot, esos datos le son inaccesibles desde la BD misma.
--
-- Se ejecuta de último (prefijo 99) para que las tablas ya existan.
-- En la BD REAL, alguien con permisos ejecuta estas mismas líneas una vez
-- (cambiando la contraseña).
CREATE USER IF NOT EXISTS 'chatbot_ro'@'%' IDENTIFIED BY 'chatbot_ro';
GRANT SELECT ON oficina_agua.tarifas_vigencia TO 'chatbot_ro'@'%';
GRANT SELECT ON oficina_agua.tarifas          TO 'chatbot_ro'@'%';
GRANT SELECT ON oficina_agua.pajas            TO 'chatbot_ro'@'%';
GRANT SELECT ON oficina_agua.metodos_pago     TO 'chatbot_ro'@'%';
GRANT SELECT ON oficina_agua.sectores         TO 'chatbot_ro'@'%';
FLUSH PRIVILEGES;
