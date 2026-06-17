package com.safevoting.users.domain.model.inscripcion;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import com.safevoting.users.domain.exception.inscripcion.MismoMunicipioException;
import com.safevoting.users.domain.exception.inscripcion.SolicitudYaProcesadaException;
import com.safevoting.users.domain.model.geografia.Municipio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCambioInscripcion {

    private UUID id;
    private UUID usuarioId;
    private Municipio municipioOrigen;
    private Municipio municipioDestino;
    private String motivo;

    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    private String motivoRechazo;
    private UUID gestorId;

    @Builder.Default
    private Instant fechaSolicitud = Instant.now();

    private Instant fechaResolucion;

    public void validateInfo() {
        validateMotivo();
        if (municipioDestino == null) {
            throw new DatosInvalidosException("El municipio destino no puede ser nulo");
        }
        if (municipioOrigen == null) {
            throw new DatosInvalidosException("El municipio origen no puede ser nulo");
        }
        validateMunicipiosDiferentes();
    }

    public boolean esPendiente() {
        return EstadoSolicitud.PENDIENTE.equals(estado);
    }

    public boolean esAceptada() {
        return EstadoSolicitud.ACEPTADA.equals(estado);
    }

    public boolean esRechazada() {
        return EstadoSolicitud.RECHAZADA.equals(estado);
    }

    public boolean esCancelada() {
        return EstadoSolicitud.CANCELADA.equals(estado);
    }

    public void setGestor(UUID gestorId) {
        this.gestorId = gestorId;
        this.fechaResolucion = Instant.now();
    }

    private boolean noEsPendiente(){
        return !EstadoSolicitud.PENDIENTE.equals(estado);
    }

    public void aceptar() {
        if (noEsPendiente()) {
            throw new SolicitudYaProcesadaException(id);
        }
        this.estado = EstadoSolicitud.ACEPTADA;
    }

    public void rechazar(String motivoRechazo) {
        if (noEsPendiente()) {
            throw new SolicitudYaProcesadaException(id);
        }
        this.motivoRechazo = motivoRechazo;
        validateMotivo();
        this.estado = EstadoSolicitud.RECHAZADA;
    }

    public void cancelar() {
        if (noEsPendiente())
            throw new SolicitudYaProcesadaException(this.id);

        this.estado = EstadoSolicitud.CANCELADA;
    }

    private void validateMotivo() {
        if (motivo == null || motivo.isBlank() || motivo.length() < 10) {
            throw new DatosInvalidosException("El motivo debe tener al menos 10 caracteres");
        }
    }

    private void validateMunicipiosDiferentes() {
        if (municipioOrigen != null && municipioDestino != null
                && municipioOrigen.getId().equals(municipioDestino.getId())) {
            throw new MismoMunicipioException();
        }
    }
}
