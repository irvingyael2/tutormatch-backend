package com.tutormatch.ms_core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity que mapea schema_core.inscripciones.
 * Se usa en ms-core para consultar cuántos alumnos están inscritos a una sesión
 * (necesario para HU-10: mostrar contador, y HU-11: bloquear fecha si hay inscritos).
 */
@Entity
@Table(name = "inscripciones", schema = "schema_core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sesion_id")
    private UUID sesionId;

    // alumno_id se guarda pero ms-core no necesita cruzarlo con ms-usuarios
    @Column(name = "alumno_id", nullable = false)
    private UUID alumnoId;

    /** Estado: CONFIRMADA | CANCELADA */
    @Column(name = "estado", length = 50)
    private String estado;

    @Column(name = "fecha_inscripcion")
    private LocalDateTime fechaInscripcion;
}
