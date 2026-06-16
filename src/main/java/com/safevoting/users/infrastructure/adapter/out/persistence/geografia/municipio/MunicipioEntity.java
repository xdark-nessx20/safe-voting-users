package com.safevoting.users.infrastructure.adapter.out.persistence.geografia.municipio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("municipio")
public class MunicipioEntity {

    @Id
    private UUID id;
    private String nombre;

    @Column("departamento_id")
    private UUID departamentoId;
}
