package com.safevoting.users.infrastructure.adapter.out.persistence.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("usuarios")
public class UsuarioEntity {

    @Id
    private UUID id;
    private String nombre;
    private String email;
    private String telefono;
    private String documento;

    @Column("municipio_id")
    private UUID municipioId;
    private String rol;
    private String estado;

    @Column("created_at")
    private Instant createdAt;
}
