package com.safevoting.users.infrastructure.adapter.in.rest.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        int status,
        String error,
        String mensaje,
        String errorCode,
        Instant timestamp
) {
    public ApiErrorResponse(int status, String error, String mensaje, String errorCode) {
        this(status, error, mensaje, errorCode, Instant.now());
    }
}
