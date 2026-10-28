-- =============================================================================
-- Estructura de la BD del MONOLITO (Laravel) — réplica para pruebas locales
-- -----------------------------------------------------------------------------
-- Traducción fiel a SQL (MySQL 8) de las 31 migraciones de database/migrations
-- del monolito heredado, en el mismo orden, con las convenciones de Laravel:
-- tipos, nombres de índices/llaves foráneas y columnas generadas.
--
-- Regla de oro: el monolito NO se toca. Este archivo NO se ejecuta contra la
-- BD real; solo crea una COPIA local para que la Fase 2 tenga con qué probar.
-- Si el equipo agrega una migración al monolito, hay que reflejarla aquí.
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 0001_01_01_000000_create_users_table (+ 2026_09_11 add_activo) ----------
CREATE TABLE users (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name              VARCHAR(255) NOT NULL,
  email             VARCHAR(255) NOT NULL,
  email_verified_at TIMESTAMP NULL,
  activo            TINYINT(1) NOT NULL DEFAULT 1,
  password          VARCHAR(255) NOT NULL,
  remember_token    VARCHAR(100) NULL,
  created_at        TIMESTAMP NULL,
  updated_at        TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY users_email_unique (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
  email      VARCHAR(255) NOT NULL,
  token      VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NULL,
  PRIMARY KEY (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sessions (
  id            VARCHAR(255) NOT NULL,
  user_id       BIGINT UNSIGNED NULL,
  ip_address    VARCHAR(45) NULL,
  user_agent    TEXT NULL,
  payload       LONGTEXT NOT NULL,
  last_activity INT NOT NULL,
  PRIMARY KEY (id),
  KEY sessions_user_id_index (user_id),
  KEY sessions_last_activity_index (last_activity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 0001_01_01_000001_create_cache_table ----------
CREATE TABLE cache (
  `key`      VARCHAR(255) NOT NULL,
  value      MEDIUMTEXT NOT NULL,
  expiration BIGINT NOT NULL,
  PRIMARY KEY (`key`),
  KEY cache_expiration_index (expiration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cache_locks (
  `key`      VARCHAR(255) NOT NULL,
  owner      VARCHAR(255) NOT NULL,
  expiration BIGINT NOT NULL,
  PRIMARY KEY (`key`),
  KEY cache_locks_expiration_index (expiration)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 0001_01_01_000002_create_jobs_table ----------
CREATE TABLE jobs (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  queue        VARCHAR(255) NOT NULL,
  payload      LONGTEXT NOT NULL,
  attempts     SMALLINT UNSIGNED NOT NULL,
  reserved_at  INT UNSIGNED NULL,
  available_at INT UNSIGNED NOT NULL,
  created_at   INT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  KEY jobs_queue_index (queue)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE job_batches (
  id             VARCHAR(255) NOT NULL,
  name           VARCHAR(255) NOT NULL,
  total_jobs     INT NOT NULL,
  pending_jobs   INT NOT NULL,
  failed_jobs    INT NOT NULL,
  failed_job_ids LONGTEXT NOT NULL,
  options        MEDIUMTEXT NULL,
  cancelled_at   INT NULL,
  created_at     INT NOT NULL,
  finished_at    INT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE failed_jobs (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  uuid       VARCHAR(255) NOT NULL,
  connection VARCHAR(255) NOT NULL,
  queue      VARCHAR(255) NOT NULL,
  payload    LONGTEXT NOT NULL,
  exception  LONGTEXT NOT NULL,
  failed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY failed_jobs_uuid_unique (uuid),
  KEY failed_jobs_connection_queue_failed_at_index (connection, queue, failed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_01_030006_create_permission_tables (spatie, sin teams) ----------
CREATE TABLE permissions (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name       VARCHAR(255) NOT NULL,
  guard_name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY permissions_name_guard_name_unique (name, guard_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name       VARCHAR(255) NOT NULL,
  guard_name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY roles_name_guard_name_unique (name, guard_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE model_has_permissions (
  permission_id BIGINT UNSIGNED NOT NULL,
  model_type    VARCHAR(255) NOT NULL,
  model_id      BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (permission_id, model_id, model_type),
  KEY model_has_permissions_model_id_model_type_index (model_id, model_type),
  CONSTRAINT model_has_permissions_permission_id_foreign FOREIGN KEY (permission_id)
    REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE model_has_roles (
  role_id    BIGINT UNSIGNED NOT NULL,
  model_type VARCHAR(255) NOT NULL,
  model_id   BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (role_id, model_id, model_type),
  KEY model_has_roles_model_id_model_type_index (model_id, model_type),
  CONSTRAINT model_has_roles_role_id_foreign FOREIGN KEY (role_id)
    REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_has_permissions (
  permission_id BIGINT UNSIGNED NOT NULL,
  role_id       BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (permission_id, role_id),
  KEY role_has_permissions_role_id_foreign (role_id),
  CONSTRAINT role_has_permissions_permission_id_foreign FOREIGN KEY (permission_id)
    REFERENCES permissions (id) ON DELETE CASCADE,
  CONSTRAINT role_has_permissions_role_id_foreign FOREIGN KEY (role_id)
    REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_01_030131_create_audits_table (+ 2026_09_11 llaves de texto) ----------
CREATE TABLE audits (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_type      VARCHAR(255) NULL,
  user_id        BIGINT UNSIGNED NULL,
  event          VARCHAR(255) NOT NULL,
  auditable_type VARCHAR(255) NOT NULL,
  auditable_id   VARCHAR(191) NOT NULL,
  old_values     JSON NULL,
  new_values     JSON NULL,
  url            TEXT NULL,
  ip_address     VARCHAR(45) NULL,
  user_agent     VARCHAR(1023) NULL,
  tags           VARCHAR(255) NULL,
  created_at     TIMESTAMP NULL,
  updated_at     TIMESTAMP NULL,
  PRIMARY KEY (id),
  KEY audits_auditable_type_auditable_id_index (auditable_type, auditable_id),
  KEY audits_user_id_user_type_index (user_id, user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000001_create_configuracion_table ----------
CREATE TABLE configuracion (
  clave           VARCHAR(50) NOT NULL,
  valor           TEXT NULL,
  descripcion     VARCHAR(255) NULL,
  actualizado_por BIGINT UNSIGNED NULL,
  created_at      TIMESTAMP NULL,
  updated_at      TIMESTAMP NULL,
  PRIMARY KEY (clave),
  KEY configuracion_actualizado_por_foreign (actualizado_por),
  CONSTRAINT configuracion_actualizado_por_foreign FOREIGN KEY (actualizado_por)
    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000002_create_sectores_table ----------
CREATE TABLE sectores (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(100) NOT NULL,
  descripcion VARCHAR(255) NULL,
  orden       SMALLINT NOT NULL DEFAULT 0,
  activo      TINYINT(1) NOT NULL DEFAULT 1,
  created_at  TIMESTAMP NULL,
  updated_at  TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY sectores_nombre_unique (nombre),
  KEY sectores_activo_orden_index (activo, orden)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000003_create_clientes_table (+ 2026_09_10 token_alta) ----------
CREATE TABLE clientes (
  id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  codigo                 VARCHAR(20) NOT NULL,
  token_alta             CHAR(36) NULL,
  nombre                 VARCHAR(150) NOT NULL,
  nit                    VARCHAR(20) NULL,
  dpi                    VARCHAR(20) NULL,
  telefono               VARCHAR(20) NULL,
  email                  VARCHAR(150) NULL,
  direccion_notificacion VARCHAR(255) NULL,
  estado                 ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
  deleted_at             TIMESTAMP NULL,
  created_at             TIMESTAMP NULL,
  updated_at             TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY clientes_codigo_unique (codigo),
  UNIQUE KEY clientes_token_alta_unique (token_alta),
  UNIQUE KEY clientes_nit_unique (nit),
  UNIQUE KEY clientes_dpi_unique (dpi),
  KEY clientes_estado_nombre_index (estado, nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000004_create_predios_table ----------
CREATE TABLE predios (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sector_id   BIGINT UNSIGNED NULL,
  aldea       VARCHAR(100) NULL,
  zona        VARCHAR(10) NULL,
  calle       VARCHAR(50) NULL,
  numero_casa VARCHAR(20) NULL,
  referencia  VARCHAR(255) NULL,
  latitud     DECIMAL(10,7) NULL,
  longitud    DECIMAL(10,7) NULL,
  deleted_at  TIMESTAMP NULL,
  created_at  TIMESTAMP NULL,
  updated_at  TIMESTAMP NULL,
  PRIMARY KEY (id),
  KEY predios_sector_id_aldea_index (sector_id, aldea),
  CONSTRAINT predios_sector_id_foreign FOREIGN KEY (sector_id)
    REFERENCES sectores (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000005_create_pajas_table ----------
CREATE TABLE pajas (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre          VARCHAR(50) NOT NULL,
  equivalencia_m3 DECIMAL(10,2) NOT NULL,
  activo          TINYINT(1) NOT NULL DEFAULT 1,
  created_at      TIMESTAMP NULL,
  updated_at      TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY pajas_nombre_unique (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000006_create_contadores_table ----------
CREATE TABLE contadores (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  predio_id         BIGINT UNSIGNED NOT NULL,
  cliente_id        BIGINT UNSIGNED NOT NULL,
  paja_id           BIGINT UNSIGNED NOT NULL,
  codigo            VARCHAR(30) NOT NULL,
  fecha_instalacion DATE NULL,
  estado            ENUM('activo','inactivo','dañado') NOT NULL DEFAULT 'activo',
  deleted_at        TIMESTAMP NULL,
  created_at        TIMESTAMP NULL,
  updated_at        TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY contadores_codigo_unique (codigo),
  KEY contadores_cliente_id_estado_index (cliente_id, estado),
  KEY contadores_predio_id_estado_index (predio_id, estado),
  KEY contadores_paja_id_foreign (paja_id),
  CONSTRAINT contadores_predio_id_foreign FOREIGN KEY (predio_id)
    REFERENCES predios (id) ON DELETE RESTRICT,
  CONSTRAINT contadores_cliente_id_foreign FOREIGN KEY (cliente_id)
    REFERENCES clientes (id) ON DELETE RESTRICT,
  CONSTRAINT contadores_paja_id_foreign FOREIGN KEY (paja_id)
    REFERENCES pajas (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000007_create_tarifas_table ----------
CREATE TABLE tarifas (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  paja_id             BIGINT UNSIGNED NOT NULL,
  monto_base          DECIMAL(10,2) NOT NULL,
  precio_m3_excedente DECIMAL(10,4) NOT NULL,
  vigente_desde       DATE NOT NULL,
  created_at          TIMESTAMP NULL,
  updated_at          TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY tarifas_paja_id_vigente_desde_unique (paja_id, vigente_desde),
  CONSTRAINT tarifas_paja_id_foreign FOREIGN KEY (paja_id)
    REFERENCES pajas (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000008_create_periodos_table ----------
CREATE TABLE periodos (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  anio         SMALLINT NOT NULL,
  mes          TINYINT NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin    DATE NOT NULL,
  cerrado_en   TIMESTAMP NULL,
  cerrado_por  BIGINT UNSIGNED NULL,
  created_at   TIMESTAMP NULL,
  updated_at   TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY periodos_anio_mes_unique (anio, mes),
  KEY periodos_cerrado_en_index (cerrado_en),
  KEY periodos_cerrado_por_foreign (cerrado_por),
  CONSTRAINT periodos_cerrado_por_foreign FOREIGN KEY (cerrado_por)
    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000009_create_lecturas_table ----------
CREATE TABLE lecturas (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  contador_id      BIGINT UNSIGNED NOT NULL,
  periodo_id       BIGINT UNSIGNED NOT NULL,
  usuario_id       BIGINT UNSIGNED NOT NULL,
  lectura_anterior DECIMAL(10,2) NOT NULL,
  lectura_actual   DECIMAL(10,2) NOT NULL,
  consumo_m3       DECIMAL(10,2) GENERATED ALWAYS AS (lectura_actual - lectura_anterior) STORED,
  fecha_lectura    DATE NOT NULL,
  observaciones    VARCHAR(255) NULL,
  created_at       TIMESTAMP NULL,
  updated_at       TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY lecturas_contador_id_periodo_id_unique (contador_id, periodo_id),
  KEY lecturas_periodo_id_index (periodo_id),
  KEY lecturas_usuario_id_foreign (usuario_id),
  CONSTRAINT lecturas_contador_id_foreign FOREIGN KEY (contador_id)
    REFERENCES contadores (id) ON DELETE RESTRICT,
  CONSTRAINT lecturas_periodo_id_foreign FOREIGN KEY (periodo_id)
    REFERENCES periodos (id) ON DELETE RESTRICT,
  CONSTRAINT lecturas_usuario_id_foreign FOREIGN KEY (usuario_id)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT lecturas_consumo_no_negativo CHECK (lectura_actual >= lectura_anterior)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000010_create_series_documento_table ----------
CREATE TABLE series_documento (
  id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tipo_documento     ENUM('boleta','recibo_pago') NOT NULL,
  codigo             VARCHAR(20) NOT NULL,
  prefijo            VARCHAR(10) NOT NULL DEFAULT '',
  separador          VARCHAR(5) NOT NULL DEFAULT '',
  incluye_anio       TINYINT(1) NOT NULL DEFAULT 0,
  longitud_numero    TINYINT NOT NULL DEFAULT 6,
  reinicia_cada_anio TINYINT(1) NOT NULL DEFAULT 0,
  ejercicio          SMALLINT NOT NULL,
  siguiente_numero   INT UNSIGNED NOT NULL DEFAULT 1,
  activa             TINYINT(1) NOT NULL DEFAULT 1,
  created_at         TIMESTAMP NULL,
  updated_at         TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY series_documento_tipo_documento_codigo_unique (tipo_documento, codigo),
  KEY series_documento_tipo_documento_activa_index (tipo_documento, activa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000011_create_boletas_table ----------
CREATE TABLE boletas (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  serie_id          BIGINT UNSIGNED NOT NULL,
  ejercicio         SMALLINT NOT NULL,
  numero            INT UNSIGNED NOT NULL,
  folio             VARCHAR(30) NOT NULL,
  cliente_id        BIGINT UNSIGNED NOT NULL,
  lectura_id        BIGINT UNSIGNED NOT NULL,
  tarifa_id         BIGINT UNSIGNED NOT NULL,
  periodo_id        BIGINT UNSIGNED NOT NULL,
  consumo_m3        DECIMAL(10,2) NOT NULL,
  monto_base        DECIMAL(10,2) NOT NULL,
  monto_excedente   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  monto             DECIMAL(10,2) NOT NULL,
  fecha_emision     DATE NOT NULL,
  fecha_vencimiento DATE NOT NULL,
  impresa_en        TIMESTAMP NULL,
  anulada_en        TIMESTAMP NULL,
  anulada_por       BIGINT UNSIGNED NULL,
  motivo_anulacion  VARCHAR(255) NULL,
  created_at        TIMESTAMP NULL,
  updated_at        TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY boletas_lectura_id_unique (lectura_id),
  UNIQUE KEY boletas_serie_id_ejercicio_numero_unique (serie_id, ejercicio, numero),
  UNIQUE KEY boletas_folio_unique (folio),
  KEY boletas_cliente_id_periodo_id_index (cliente_id, periodo_id),
  KEY boletas_anulada_en_fecha_vencimiento_index (anulada_en, fecha_vencimiento),
  KEY boletas_tarifa_id_foreign (tarifa_id),
  KEY boletas_periodo_id_foreign (periodo_id),
  KEY boletas_anulada_por_foreign (anulada_por),
  CONSTRAINT boletas_serie_id_foreign FOREIGN KEY (serie_id)
    REFERENCES series_documento (id) ON DELETE RESTRICT,
  CONSTRAINT boletas_cliente_id_foreign FOREIGN KEY (cliente_id)
    REFERENCES clientes (id) ON DELETE RESTRICT,
  CONSTRAINT boletas_lectura_id_foreign FOREIGN KEY (lectura_id)
    REFERENCES lecturas (id) ON DELETE RESTRICT,
  CONSTRAINT boletas_tarifa_id_foreign FOREIGN KEY (tarifa_id)
    REFERENCES tarifas (id) ON DELETE RESTRICT,
  CONSTRAINT boletas_periodo_id_foreign FOREIGN KEY (periodo_id)
    REFERENCES periodos (id) ON DELETE RESTRICT,
  CONSTRAINT boletas_anulada_por_foreign FOREIGN KEY (anulada_por)
    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000012_create_metodos_pago_table ----------
CREATE TABLE metodos_pago (
  id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  codigo              VARCHAR(20) NOT NULL,
  nombre              VARCHAR(50) NOT NULL,
  requiere_referencia TINYINT(1) NOT NULL DEFAULT 0,
  activo              TINYINT(1) NOT NULL DEFAULT 1,
  created_at          TIMESTAMP NULL,
  updated_at          TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY metodos_pago_codigo_unique (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000013_create_pagos_table ----------
CREATE TABLE pagos (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  serie_id       BIGINT UNSIGNED NOT NULL,
  ejercicio      SMALLINT NOT NULL,
  numero         INT UNSIGNED NOT NULL,
  folio          VARCHAR(30) NOT NULL,
  boleta_id      BIGINT UNSIGNED NOT NULL,
  metodo_pago_id BIGINT UNSIGNED NOT NULL,
  usuario_id     BIGINT UNSIGNED NOT NULL,
  monto          DECIMAL(10,2) NOT NULL,
  fecha_pago     DATE NOT NULL,
  referencia     VARCHAR(100) NULL,
  revertido_en   TIMESTAMP NULL,
  revertido_por  BIGINT UNSIGNED NULL,
  motivo_reverso VARCHAR(255) NULL,
  created_at     TIMESTAMP NULL,
  updated_at     TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY pagos_serie_id_ejercicio_numero_unique (serie_id, ejercicio, numero),
  UNIQUE KEY pagos_folio_unique (folio),
  KEY pagos_boleta_id_revertido_en_index (boleta_id, revertido_en),
  KEY pagos_fecha_pago_index (fecha_pago),
  KEY pagos_metodo_pago_id_foreign (metodo_pago_id),
  KEY pagos_usuario_id_foreign (usuario_id),
  KEY pagos_revertido_por_foreign (revertido_por),
  CONSTRAINT pagos_serie_id_foreign FOREIGN KEY (serie_id)
    REFERENCES series_documento (id) ON DELETE RESTRICT,
  CONSTRAINT pagos_boleta_id_foreign FOREIGN KEY (boleta_id)
    REFERENCES boletas (id) ON DELETE RESTRICT,
  CONSTRAINT pagos_metodo_pago_id_foreign FOREIGN KEY (metodo_pago_id)
    REFERENCES metodos_pago (id) ON DELETE RESTRICT,
  CONSTRAINT pagos_usuario_id_foreign FOREIGN KEY (usuario_id)
    REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT pagos_revertido_por_foreign FOREIGN KEY (revertido_por)
    REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000014_create_tipos_documento_table ----------
CREATE TABLE tipos_documento (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  codigo          VARCHAR(20) NOT NULL,
  nombre          VARCHAR(80) NOT NULL,
  respalda_predio TINYINT(1) NOT NULL DEFAULT 0,
  activo          TINYINT(1) NOT NULL DEFAULT 1,
  created_at      TIMESTAMP NULL,
  updated_at      TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY tipos_documento_codigo_unique (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000015_create_documentos_table (+ 2026_09_10 disco privado) ----------
CREATE TABLE documentos (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  cliente_id        BIGINT UNSIGNED NOT NULL,
  predio_id         BIGINT UNSIGNED NULL,
  tipo_documento_id BIGINT UNSIGNED NOT NULL,
  disco             VARCHAR(20) NOT NULL DEFAULT 'local',
  ruta              VARCHAR(500) NOT NULL,
  nombre_original   VARCHAR(255) NOT NULL,
  mime              VARCHAR(100) NOT NULL,
  tamano_bytes      INT UNSIGNED NOT NULL,
  hash_sha256       CHAR(64) NOT NULL,
  firmado           TINYINT(1) NOT NULL DEFAULT 0,
  fecha_firma       DATE NULL,
  subido_por        BIGINT UNSIGNED NOT NULL,
  created_at        TIMESTAMP NULL,
  updated_at        TIMESTAMP NULL,
  PRIMARY KEY (id),
  KEY documentos_cliente_id_tipo_documento_id_index (cliente_id, tipo_documento_id),
  KEY documentos_predio_id_index (predio_id),
  KEY documentos_tipo_documento_id_foreign (tipo_documento_id),
  KEY documentos_subido_por_foreign (subido_por),
  CONSTRAINT documentos_cliente_id_foreign FOREIGN KEY (cliente_id)
    REFERENCES clientes (id) ON DELETE CASCADE,
  CONSTRAINT documentos_predio_id_foreign FOREIGN KEY (predio_id)
    REFERENCES predios (id) ON DELETE CASCADE,
  CONSTRAINT documentos_tipo_documento_id_foreign FOREIGN KEY (tipo_documento_id)
    REFERENCES tipos_documento (id) ON DELETE RESTRICT,
  CONSTRAINT documentos_subido_por_foreign FOREIGN KEY (subido_por)
    REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000016_create_evidencias_lectura_table ----------
CREATE TABLE evidencias_lectura (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  lectura_id      BIGINT UNSIGNED NOT NULL,
  disco           VARCHAR(20) NOT NULL DEFAULT 'public',
  ruta            VARCHAR(500) NOT NULL,
  nombre_original VARCHAR(255) NOT NULL,
  mime            VARCHAR(100) NOT NULL,
  tamano_bytes    INT UNSIGNED NOT NULL,
  hash_sha256     CHAR(64) NOT NULL,
  descripcion     VARCHAR(255) NULL,
  subido_por      BIGINT UNSIGNED NOT NULL,
  created_at      TIMESTAMP NULL,
  updated_at      TIMESTAMP NULL,
  PRIMARY KEY (id),
  KEY evidencias_lectura_lectura_id_index (lectura_id),
  KEY evidencias_lectura_subido_por_foreign (subido_por),
  CONSTRAINT evidencias_lectura_lectura_id_foreign FOREIGN KEY (lectura_id)
    REFERENCES lecturas (id) ON DELETE CASCADE,
  CONSTRAINT evidencias_lectura_subido_por_foreign FOREIGN KEY (subido_por)
    REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_04_000017_create_tarifas_vigencia_view ----------
CREATE VIEW tarifas_vigencia AS
SELECT
    t.id,
    t.paja_id,
    t.monto_base,
    t.precio_m3_excedente,
    t.vigente_desde,
    (
        SELECT MIN(s.vigente_desde)
        FROM tarifas s
        WHERE s.paja_id = t.paja_id
          AND s.vigente_desde > t.vigente_desde
    ) AS proxima_vigencia,
    t.created_at,
    t.updated_at
FROM tarifas t;

-- ---------- 2026_09_09_000001_create_cliente_accesos_table (+ 2026_09_11 reotorgar) ----------
-- `vigente` vale 1 mientras el acceso no esté revocado y NULL cuando sí:
-- como MySQL permite varios NULL en un UNIQUE, un cliente puede tener varios
-- accesos revocados pero solo uno vigente.
CREATE TABLE cliente_accesos (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  cliente_id   BIGINT UNSIGNED NOT NULL,
  user_id      BIGINT UNSIGNED NOT NULL,
  otorgado_por BIGINT UNSIGNED NOT NULL,
  otorgado_en  TIMESTAMP NOT NULL,
  revocado_por BIGINT UNSIGNED NULL,
  revocado_en  TIMESTAMP NULL,
  created_at   TIMESTAMP NULL,
  updated_at   TIMESTAMP NULL,
  vigente      TINYINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN revocado_en IS NULL THEN 1 ELSE NULL END) VIRTUAL,
  PRIMARY KEY (id),
  UNIQUE KEY cliente_accesos_cliente_vigente_unique (cliente_id, vigente),
  UNIQUE KEY cliente_accesos_user_vigente_unique (user_id, vigente),
  KEY cliente_accesos_otorgado_por_foreign (otorgado_por),
  KEY cliente_accesos_revocado_por_foreign (revocado_por),
  CONSTRAINT cliente_accesos_cliente_id_foreign FOREIGN KEY (cliente_id) REFERENCES clientes (id),
  CONSTRAINT cliente_accesos_user_id_foreign FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT cliente_accesos_otorgado_por_foreign FOREIGN KEY (otorgado_por) REFERENCES users (id),
  CONSTRAINT cliente_accesos_revocado_por_foreign FOREIGN KEY (revocado_por) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- 2026_09_10_000001_create_correlativos_table ----------
CREATE TABLE correlativos (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  clave            VARCHAR(30) NOT NULL,
  prefijo          VARCHAR(10) NOT NULL DEFAULT '',
  longitud         TINYINT UNSIGNED NOT NULL DEFAULT 4,
  siguiente_numero INT UNSIGNED NOT NULL DEFAULT 1,
  created_at       TIMESTAMP NULL,
  updated_at       TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY correlativos_clave_unique (clave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La migración misma inserta estas dos filas
INSERT INTO correlativos (clave, prefijo, longitud, siguiente_numero, created_at, updated_at) VALUES
  ('cliente',  'CLI-', 4, 1, NOW(), NOW()),
  ('contador', 'CTR-', 4, 1, NOW(), NOW());

-- ---------- 2026_09_11_000004_create_lector_sectores_table ----------
CREATE TABLE lector_sectores (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL,
  sector_id  BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NULL,
  updated_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY lector_sectores_user_id_sector_id_unique (user_id, sector_id),
  KEY lector_sectores_sector_id_foreign (sector_id),
  CONSTRAINT lector_sectores_user_id_foreign FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT lector_sectores_sector_id_foreign FOREIGN KEY (sector_id)
    REFERENCES sectores (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------- Tabla de control de Laravel ----------
-- Registra las migraciones como ya ejecutadas, igual que en la BD real.
CREATE TABLE migrations (
  id        INT UNSIGNED NOT NULL AUTO_INCREMENT,
  migration VARCHAR(255) NOT NULL,
  batch     INT NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO migrations (migration, batch) VALUES
  ('0001_01_01_000000_create_users_table', 1),
  ('0001_01_01_000001_create_cache_table', 1),
  ('0001_01_01_000002_create_jobs_table', 1),
  ('2026_09_01_030006_create_permission_tables', 1),
  ('2026_09_01_030131_create_audits_table', 1),
  ('2026_09_04_000001_create_configuracion_table', 1),
  ('2026_09_04_000002_create_sectores_table', 1),
  ('2026_09_04_000003_create_clientes_table', 1),
  ('2026_09_04_000004_create_predios_table', 1),
  ('2026_09_04_000005_create_pajas_table', 1),
  ('2026_09_04_000006_create_contadores_table', 1),
  ('2026_09_04_000007_create_tarifas_table', 1),
  ('2026_09_04_000008_create_periodos_table', 1),
  ('2026_09_04_000009_create_lecturas_table', 1),
  ('2026_09_04_000010_create_series_documento_table', 1),
  ('2026_09_04_000011_create_boletas_table', 1),
  ('2026_09_04_000012_create_metodos_pago_table', 1),
  ('2026_09_04_000013_create_pagos_table', 1),
  ('2026_09_04_000014_create_tipos_documento_table', 1),
  ('2026_09_04_000015_create_documentos_table', 1),
  ('2026_09_04_000016_create_evidencias_lectura_table', 1),
  ('2026_09_04_000017_create_tarifas_vigencia_view', 1),
  ('2026_09_08_000001_corrige_equivalencia_de_pajas_a_m3', 2),
  ('2026_09_09_000001_create_cliente_accesos_table', 3),
  ('2026_09_10_000001_create_correlativos_table', 4),
  ('2026_09_10_000002_add_token_alta_to_clientes_table', 4),
  ('2026_09_10_000003_documentos_al_disco_privado', 4),
  ('2026_09_11_000001_add_activo_to_users_table', 5),
  ('2026_09_11_000002_permite_reotorgar_acceso_al_portal', 5),
  ('2026_09_11_000003_audits_admite_llaves_de_texto', 5),
  ('2026_09_11_000004_create_lector_sectores_table', 5);

SET FOREIGN_KEY_CHECKS = 1;
