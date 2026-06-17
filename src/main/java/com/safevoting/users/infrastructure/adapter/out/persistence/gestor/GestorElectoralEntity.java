package com.safevoting.users.infrastructure.adapter.out.persistence.gestor;

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
@Table("gestores_electorales")
public class GestorElectoralEntity {

    @Id
    private UUID id;

    @Column("usuario_id")
    private UUID usuarioId;

    @Column("alcance_operacion")
    private String alcanceOperacion;
}
