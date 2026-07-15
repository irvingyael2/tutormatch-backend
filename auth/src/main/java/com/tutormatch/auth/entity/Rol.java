package com.tutormatch.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "roles", schema = "schema_usuarios")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String nombre;

    // --- GETTERS Y SETTERS EXPLÍCITOS ---

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}