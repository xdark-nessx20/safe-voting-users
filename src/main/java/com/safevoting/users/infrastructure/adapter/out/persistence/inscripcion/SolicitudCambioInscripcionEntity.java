package com.safevoting.users.infrastructure.adapter.out.persistence.inscripcion;

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
@Table("solicitudes_cambio_inscripcion")
public class SolicitudCambioInscripcionEntity {

    @Id
    private UUID id;

    @Column("usuario_id")
    private UUID usuarioId;

    @Column("municipio_origen_id")
    private UUID municipioOrigenId;

    @Column("municipio_destino_id")
    private UUID municipioDestinoId;

    private String motivo;
    private String estado;

    @Column("motivo_rechazo")
    private String motivoRechazo;

    @Column("gestor_id")
    private UUID gestorId;

    @Column("fecha_solicitud")
    private Instant fechaSolicitud;

    @Column("fecha_resolucion")
    private Instant fechaResolucion;
}
