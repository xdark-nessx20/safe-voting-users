package com.safevoting.users.application.usuario;

import java.util.List;

public record PaginaResultado<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {}
