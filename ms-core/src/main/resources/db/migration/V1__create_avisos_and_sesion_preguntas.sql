CREATE SCHEMA IF NOT EXISTS schema_core;

CREATE TABLE IF NOT EXISTS schema_core.avisos (
    id UUID PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    mensaje TEXT NOT NULL,
    creado_en TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_core.sesion_preguntas (
    id UUID PRIMARY KEY,
    sesion_id UUID NOT NULL,
    alumno_id UUID NOT NULL,
    alumno_nombre VARCHAR(255) NOT NULL,
    pregunta TEXT NOT NULL,
    respuesta TEXT,
    creado_en TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    respondido_en TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE IF EXISTS schema_core.sesion_preguntas
    ADD CONSTRAINT fk_sesion_preguntas_sesion
    FOREIGN KEY (sesion_id)
    REFERENCES schema_core.sesiones (id);
