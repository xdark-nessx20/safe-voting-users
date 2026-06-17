package com.safevoting.users.infrastructure.adapter.out.persistence.geografia.departamento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("departamentos")
public class DepartamentoEntity {

    @Id
    private UUID id;
    private String nombre;
}
