package com.safevoting.users.infrastructure.adapter.in.rest.common;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.exception.otp.OtpExpiradoException;
import com.safevoting.users.domain.exception.otp.OtpInvalidoException;
import com.safevoting.users.domain.exception.otp.ReintentosExcedidosException;
import com.safevoting.users.domain.exception.otp.TransicionEstadoOtpInvalidaException;
import com.safevoting.users.domain.exception.inscripcion.*;
import com.safevoting.users.domain.exception.usuario.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailDuplicado(EmailDuplicadoException ex) {
        var body = new ApiErrorResponse(409, "Conflicto", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(DocumentoDuplicadoException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentoDuplicado(DocumentoDuplicadoException ex) {
        var body = new ApiErrorResponse(409, "Conflicto", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(EmailNoRegistradoException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailNoRegistrado(EmailNoRegistradoException ex) {
        var body = new ApiErrorResponse(404, "No Encontrado", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UsuarioNoHabilitadoException.class)
    public ResponseEntity<ApiErrorResponse> handleUsuarioNoHabilitado(UsuarioNoHabilitadoException ex) {
        var body = new ApiErrorResponse(403, "Prohibido", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(OtpExpiradoException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpExpirado(OtpExpiradoException ex) {
        var body = new ApiErrorResponse(401, "No Autorizado", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(OtpInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpInvalido(OtpInvalidoException ex) {
        var body = new ApiErrorResponse(401, "No Autorizado", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(ReintentosExcedidosException.class)
    public ResponseEntity<ApiErrorResponse> handleReintentosExcedidos(ReintentosExcedidosException ex) {
        var body = new ApiErrorResponse(401, "No Autorizado", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(TransicionEstadoOtpInvalidaException.class)
    public ResponseEntity<ApiErrorResponse> handleTransicionEstadoOtpInvalida(TransicionEstadoOtpInvalidaException ex) {
        var body = new ApiErrorResponse(422, "Entidad no Procesable", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(MunicipioNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleMunicipioNoEncontrado(MunicipioNoEncontradoException ex) {
        var body = new ApiErrorResponse(404, "No Encontrado", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ApiErrorResponse> handleDatosInvalidos(DatosInvalidosException ex) {
        var body = new ApiErrorResponse(422, "Entidad no Procesable", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiErrorResponse> handleUsuarioInactivo(UsuarioInactivoException ex) {
        var body = new ApiErrorResponse(403, "Prohibido", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(RolInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleRolInvalido(RolInvalidoException ex) {
        var body = new ApiErrorResponse(403, "Prohibido", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AlcanceInsuficienteException.class)
    public ResponseEntity<ApiErrorResponse> handleAlcanceInsuficiente(AlcanceInsuficienteException ex) {
        var body = new ApiErrorResponse(403, "Prohibido", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<ApiErrorResponse> handleTransicionEstadoInvalida(TransicionEstadoInvalidaException ex) {
        var body = new ApiErrorResponse(422, "Entidad no Procesable", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(GestorNoModificableException.class)
    public ResponseEntity<ApiErrorResponse> handleGestorNoModificable(GestorNoModificableException ex) {
        var body = new ApiErrorResponse(403, "Prohibido", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        var body = new ApiErrorResponse(404, "No Encontrado", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> handleValidacion(WebExchangeBindException ex) {
        String mensaje = ex.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Error de validación");
        var body = new ApiErrorResponse(422, "Entidad no Procesable", mensaje, "VALIDACION");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(SolicitudDuplicadaException.class)
    public ResponseEntity<ApiErrorResponse> handleSolicitudDuplicada(SolicitudDuplicadaException ex) {
        var body = new ApiErrorResponse(409, "Conflicto", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(SolicitudYaProcesadaException.class)
    public ResponseEntity<ApiErrorResponse> handleSolicitudYaProcesada(SolicitudYaProcesadaException ex) {
        var body = new ApiErrorResponse(409, "Conflicto", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MismoMunicipioException.class)
    public ResponseEntity<ApiErrorResponse> handleMismoMunicipio(MismoMunicipioException ex) {
        var body = new ApiErrorResponse(422, "Entidad no Procesable", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(MotivoRequeridoException.class)
    public ResponseEntity<ApiErrorResponse> handleMotivoRequerido(MotivoRequeridoException ex) {
        var body = new ApiErrorResponse(422, "Entidad no Procesable", ex.getMessage(), ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(RuntimeException ex) {
        var body = new ApiErrorResponse(500, "Error Interno", "Ha ocurrido un error interno del servidor", "ERROR_INTERNO");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
