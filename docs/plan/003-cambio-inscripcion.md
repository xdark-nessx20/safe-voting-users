# Implementation Plan: Cambio de Lugar de Inscripción

**Date**: 2026-06-13  
**Spec**: [003-cambio-inscripcion.md](../spec/003-cambio-inscripcion.md)

## Summary

Implementar el flujo de cambio de lugar de inscripción: los votantes solicitan el cambio a un nuevo municipio con un
motivo, y los gestores electorales listan, aceptan o rechazan dichas solicitudes. La aceptación actualiza el municipio
de inscripción del votante. Todo el flujo respeta el `alcance_operacion` del gestor sobre el municipio destino de la
solicitud.

**Technical approach**: Nueva entidad de dominio `SolicitudCambioInscripcion` con su propio ciclo de vida. Casos de uso
en `application/inscripcion/`. Endpoints REST bajo `/users/inscripcion` (votante) y `/admin/inscripcion` (gestor).
Control de concurrencia para evitar doble procesamiento.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x (WebFlux), Spring Data R2DBC, Flyway, jjwt, Lombok, MapStruct,
springdoc-openapi, Spring Boot Actuator, Bean Validation  
**Storage**: PostgreSQL 16 (via R2DBC)  
**Testing**: JUnit 5, Mockito, WebTestClient, Testcontainers  
**Target Platform**: Linux server (Docker)  
**Project Type**: Microservice REST reactivo — extensión de los planes 001 y 002  
**Performance Goals**: Creación de solicitud <2s, aceptación/rechazo <1s, listado paginado <500ms  
**Constraints**: Solo una solicitud pendiente por votante, municipio destino ≠ municipio actual, cancelación automática
al suspender usuario  
**Scale/Scope**: ~10k votantes, ~500 solicitudes pendientes en período pre-electoral

## Project Structure

### Documentation (this feature)

```text
docs/
├── plan/
│   └── 003-cambio-inscripcion.md      # Este archivo
├── spec/
│   └── 003-cambio-inscripcion.md
└── stakeholders.md
```

### Source Code (repository root — nuevo y modificado)

```text
src/main/java/com/safevoting/users/
│
├── domain/
│   ├── model/
│   │   ├── inscripcion/
│   │   │   ├── SolicitudCambioInscripcion.java  # NUEVO
│   │   │   └── EstadoSolicitud.java             # NUEVO: PENDIENTE, ACEPTADA, RECHAZADA, CANCELADA
│   │   └── exception/
│   │       └── inscripcion/
│   │           ├── SolicitudDuplicadaException.java
│   │           ├── SolicitudYaProcesadaException.java
│   │           ├── MismoMunicipioException.java
│   │           └── MotivoRequeridoException.java
│   └── repository/
│       └── SolicitudCambioInscripcionRepository.java  # NUEVO (puerto)
│
├── application/
│   └── inscripcion/
│       ├── SolicitarCambioInscripcionUseCase.java
│       ├── ListarSolicitudesPendientesUseCase.java
│       ├── AceptarSolicitudInscripcionUseCase.java
│       └── RechazarSolicitudInscripcionUseCase.java
│
└── infrastructure/
    ├── config/
    │   └── SecurityConfig.java                        # MODIFICAR: agregar rutas /api/v1/users/inscripcion/**
    └── adapter/
        ├── in/
        │   └── rest/
        │       ├── inscripcion/
        │       │   ├── dto/
        │       │   │   ├── SolicitarCambioRequest.java
        │       │   │   └── SolicitudCambioResponse.java
        │       │   ├── mapper/
        │       │   │   └── SolicitudCambioDtoMapper.java  # MapStruct
        │       │   └── InscripcionController.java
        │       └── admin/
        │           ├── dto/
        │           │   └── RechazarSolicitudRequest.java
        │           └── AdminInscripcionController.java
        └── out/
            └── persistence/
                ├── inscripcion/
                │   └── SolicitudCambioInscripcionR2dbcRepository.java  # NUEVO
                └── usuario/
                    └── UsuarioR2dbcRepository.java  # MODIFICAR: cancelar solicitudes al suspender

src/main/resources/
└── db/migration/
    └── V3__crear_solicitud_cambio_inscripcion.sql

src/test/java/com/safevoting/users/
├── unit/
│   ├── domain/
│   │   └── model/inscripcion/
│   │       └── SolicitudCambioInscripcionTest.java
│   └── application/
│       └── inscripcion/
│           ├── SolicitarCambioInscripcionUseCaseTest.java
│           ├── ListarSolicitudesPendientesUseCaseTest.java
│           ├── AceptarSolicitudInscripcionUseCaseTest.java
│           └── RechazarSolicitudInscripcionUseCaseTest.java
└── integration/
    └── rest/
        ├── inscripcion/
        │   └── InscripcionControllerIntegrationTest.java
        └── admin/
            └── AdminInscripcionControllerIntegrationTest.java
```

**Structure Decision**: Misma arquitectura hexagonal. `SolicitudCambioInscripcion` es una entidad de dominio
independiente en `domain/model/inscripcion/`. Las excepciones nuevas van en `domain/model/exception/inscripcion/`.
Los controladores se separan por stakeholder: `InscripcionController` en `rest/inscripcion/` (votante) y
`AdminInscripcionController` en `rest/admin/` (gestor). Los tests replican la misma organización por feature.
Todas las rutas usan el prefijo `/api/v1/`.

---

## Phase 1: Foundational — Nueva entidad y modelo de dominio (Blocking Prerequisites)

**Purpose**: Crear la tabla, entidad, enumerados, excepciones y repositorio para `SolicitudCambioInscripcion`.

**⚠️ CRITICAL**: Ningún user story de cambio de inscripción puede comenzar antes de esta fase.

- [ ] T001 Crear migración Flyway `V3__crear_solicitud_cambio_inscripcion.sql`:
    - Tabla `solicitud_cambio_inscripcion`:
        - `id UUID PRIMARY KEY`
        - `usuario_id UUID NOT NULL REFERENCES usuario(id)`
        - `municipio_origen_id UUID NOT NULL REFERENCES municipio(id)`
        - `municipio_destino_id UUID NOT NULL REFERENCES municipio(id)`
        - `motivo TEXT NOT NULL`
        - `estado VARCHAR NOT NULL DEFAULT 'PENDIENTE'`
        - `motivo_rechazo TEXT`
        - `gestor_id UUID REFERENCES usuario(id)`
        - `fecha_solicitud TIMESTAMP NOT NULL DEFAULT NOW()`
        - `fecha_resolucion TIMESTAMP`
    - `CONSTRAINT chk_estado_solicitud CHECK (estado IN ('PENDIENTE','ACEPTADA','RECHAZADA','CANCELADA'))`
    - Índice en `(usuario_id, estado)` para búsqueda rápida de solicitud pendiente por usuario.
    - Índice en `(estado, municipio_destino_id)` para listado de pendientes por alcance.
- [ ] T002 Crear enum `EstadoSolicitud` junto a su entidad en `domain/model/inscripcion/`:
    - `PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `CANCELADA`.
    - Método `esPendiente()` → `this == PENDIENTE`.
- [ ] T003 Crear entidad `SolicitudCambioInscripcion` en `domain/model/`:
    - `@Builder`, campos: `id` (UUID), `usuarioId` (UUID), `municipioOrigen` (Municipio), `municipioDestino` (
      Municipio), `motivo` (String), `estado` (EstadoSolicitud, default PENDIENTE), `motivoRechazo` (String, nullable),
      `gestorId` (UUID, nullable), `fechaSolicitud` (Instant), `fechaResolucion` (Instant, nullable).
    - Métodos de validación privados:
        - `validateMotivo()`: no vacío, longitud mínima 10 caracteres. Lanza `DatosInvalidosException` si falla.
        - `validateMunicipiosDiferentes()`: `municipioOrigen.getId() != municipioDestino.getId()`. Si son iguales →
          `MismoMunicipioException`.
        - `validateMunicipioDestino()`: no nulo. Si es nulo → `MunicipioNoEncontradoException`.
    - Método público `validateInfo()`: invoca `validateMotivo()`, `validateMunicipiosDiferentes()`,
      `validateMunicipioDestino()`.
    - Métodos de mutación:
        - `aceptar(UUID gestorId)`: valida `esPendiente()`. Si no → `SolicitudYaProcesadaException`. Asigna
          `estado = ACEPTADA`, `gestorId`, `fechaResolucion = Instant.now()`. Invoca `validateInfo()`.
        - `rechazar(UUID gestorId, String motivoRechazo)`: valida `esPendiente()`. Si no →
          `SolicitudYaProcesadaException`. Valida `motivoRechazo` no vacío → `MotivoRequeridoException`. Asigna
          `estado = RECHAZADA`, `motivoRechazo`, `gestorId`, `fechaResolucion = Instant.now()`. Invoca `validateInfo()`.
        - `cancelar(String motivo)`: asigna `estado = CANCELADA`, `motivoRechazo = motivo`,
          `fechaResolucion = Instant.now()`.
- [ ] T004 Crear excepciones de dominio nuevas:
    - `SolicitudDuplicadaException(UUID usuarioId)` → errorCode `SOLICITUD_DUPLICADA`.
    - `SolicitudYaProcesadaException(UUID solicitudId)` → errorCode `SOLICITUD_YA_PROCESADA`.
    - `MismoMunicipioException()` → errorCode `MISMO_MUNICIPIO`.
    - `MotivoRequeridoException()` → errorCode `MOTIVO_REQUERIDO`.
- [ ] T005 Crear puerto `SolicitudCambioInscripcionRepository` en `domain/repository/`:
    - `save(SolicitudCambioInscripcion)` → `Mono<SolicitudCambioInscripcion>`.
    - `findById(UUID id)` → `Mono<SolicitudCambioInscripcion>`.
    - `findPendienteByUsuarioId(UUID usuarioId)` → `Mono<SolicitudCambioInscripcion>`.
    - `findPendientesByMunicipioDestinoId(UUID municipioId, long offset, int limit)` →
      `Flux<SolicitudCambioInscripcion>`.
    - `countPendientesByMunicipioDestinoId(UUID municipioId)` → `Mono<Long>`.
    - `findPendientesByDepartamentoDestinoId(UUID departamentoId, long offset, int limit)` →
      `Flux<SolicitudCambioInscripcion>`.
    - `countPendientesByDepartamentoDestinoId(UUID departamentoId)` → `Mono<Long>`.
    - `findAllPendientes(long offset, int limit)` → `Flux<SolicitudCambioInscripcion>`.
    - `countAllPendientes()` → `Mono<Long>`.
    - `cancelarPendientesPorUsuarioId(UUID usuarioId, String motivo)` → `Mono<Long>`.
    - `cancelarPendientesPorMunicipioDestinoId(UUID municipioId, String motivo)` → `Mono<Long>`.
    - `update(SolicitudCambioInscripcion)` → `Mono<SolicitudCambioInscripcion>`.
- [ ] T006 Crear adaptador `SolicitudCambioInscripcionR2dbcRepository` en `infrastructure/adapter/out/persistence/`:
    - Implementa el puerto usando `R2dbcEntityTemplate`. Mapea filas ↔ entidad manualmente.
    - `findPendienteByUsuarioId`:
      `SELECT * FROM solicitud_cambio_inscripcion WHERE usuario_id = :usuarioId AND estado = 'PENDIENTE'`.
    - `findPendientesByMunicipioDestinoId`: filtrar por `municipio_destino_id = :municipioId AND estado = 'PENDIENTE'`.
    - `findPendientesByDepartamentoDestinoId`: JOIN con `municipio` para filtrar por `departamento_id`.
    - `cancelarPendientesPorMunicipioDestinoId`:
      `UPDATE solicitud_cambio_inscripcion SET estado = 'CANCELADA', motivo_rechazo = :motivo, fecha_resolucion = NOW() WHERE municipio_destino_id = :municipioId AND estado = 'PENDIENTE'`.
- [ ] T007 Modificar `GlobalExceptionHandler`:
    - Agregar mapeos:
        - `SolicitudDuplicadaException` → 409
        - `SolicitudYaProcesadaException` → 409
        - `MismoMunicipioException` → 422
        - `MotivoRequeridoException` → 422
- [ ] T008 Modificar `SecurityConfig`:
    - `/api/v1/users/inscripcion/**` requiere autenticación (cualquier rol).
    - `/api/v1/admin/inscripcion/**` requiere `ROLE_GESTOR_ELECTORAL` (ya configurado en plan 002 para
      `/api/v1/admin/**`; verificar que aplique).
- [ ] T009 Modificar `UsuarioR2dbcRepository` (o el caso de uso de suspensión del plan 002):
    - Al suspender un usuario (`estado = INACTIVO`), invocar
      `SolicitudCambioInscripcionRepository.cancelarPendientesPorUsuarioId(usuarioId, "Usuario suspendido")` en la misma
      transacción.
    - Esto asegura FR-010 del spec: cancelación automática al cambiar a INACTIVO.

**Checkpoint**: Modelo de `SolicitudCambioInscripcion` completo. Repositorio funcional. Cancelación automática
integrada.

---

## Phase 2: User Story 1 — Solicitar cambio de inscripción (Priority: P1)

**Goal**: Un votante autenticado solicita cambiar su lugar de inscripción a un nuevo municipio. La solicitud queda
PENDIENTE.

**Independent Test**: Autenticar votante → `POST /api/v1/users/inscripcion/solicitar` con municipio destino y motivo → 201. Ya
tiene solicitud pendiente → 409. Municipio destino = municipio actual → 422. Municipio destino no existe → 404.

### Tests for User Story 1

- [ ] T010 [P] [US1] Unit test `SolicitudCambioInscripcionTest`: construir con Builder + `validateInfo()` → OK.
  Municipio origen = destino → `MismoMunicipioException`. Motivo vacío → `DatosInvalidosException`. Motivo < 10
  caracteres → `DatosInvalidosException`.
- [ ] T011 [P] [US1] Unit test `SolicitarCambioInscripcionUseCaseTest`: mock repos → solicitud creada exitosamente. Ya
  existe pendiente → `SolicitudDuplicadaException`. Municipio destino no existe → `MunicipioNoEncontradoException`.
- [ ] T012 [P] [US1] Integration test `InscripcionControllerIntegrationTest`: `POST /api/v1/users/inscripcion/solicitar` → 201
  con id de solicitud. Solicitud duplicada → 409.

### Implementation for User Story 1

- [ ] T013 [P] [US1] Crear `SolicitarCambioRequest` DTO: `UUID municipioDestinoId` (`@NotNull`), `String motivo` (
  `@NotBlank`, `@Size(min = 10, max = 500)`).
- [ ] T014 [P] [US1] Crear `SolicitudCambioResponse` DTO: `UUID id`, `String municipioOrigenNombre`,
  `String municipioDestinoNombre`, `String motivo`, `String estado`, `String motivoRechazo`,
  `LocalDateTime fechaSolicitud`, `LocalDateTime fechaResolucion`.
- [ ] T015 [P] [US1] Crear `SolicitudCambioDtoMapper` (MapStruct):
  `SolicitarCambioRequest → SolicitudCambioInscripcion` (mapeo parcial, el resto lo completa el use case).
  `SolicitudCambioInscripcion → SolicitudCambioResponse`.
- [ ] T016 [US1] Crear `SolicitarCambioInscripcionUseCase` en `application/inscripcion/`:
    - Inyecta `SolicitudCambioInscripcionRepository`, `UsuarioRepository`, `MunicipioRepository`.
    - Método `ejecutar(UUID municipioDestinoId, String motivo, Usuario votante)`:
        1. Busca solicitud pendiente del usuario. Si existe → `SolicitudDuplicadaException`.
        2. Busca municipio destino. Si no existe → `MunicipioNoEncontradoException`.
        3. Construye `SolicitudCambioInscripcion` con Builder:
            - `municipioOrigen = votante.getMunicipio()`
            - `municipioDestino = municipioDestino`
            - `motivo = motivo`
            - Validar `solicitud.validateInfo()` (incluye `validateMunicipiosDiferentes()`).
        4. Asigna `id = UUID.randomUUID()`, `estado = PENDIENTE`, `usuarioId = votante.getId()`,
           `fechaSolicitud = Instant.now()`.
        5. Persiste y retorna `SolicitudCambioInscripcion`.
- [ ] T017 [US1] Crear `InscripcionController` en `infrastructure/adapter/in/rest/inscripcion/`:
    - `POST /api/v1/users/inscripcion/solicitar`: recibe `@Valid @RequestBody SolicitarCambioRequest`, extrae votante del
      `SecurityContext` (mismo helper que en AdminController), invoca use case, retorna `201` con
      `SolicitudCambioResponse`.
    - Anotar con `@Tag(name = "Cambio de Inscripción")`.
- [ ] T018 [US1] Integration test: flujo completo de solicitud.

**Checkpoint**: Votante puede crear solicitudes de cambio de inscripción. Validaciones de dominio aplicadas.

---

## Phase 3: User Story 2 — Listar solicitudes pendientes (Priority: P1)

**Goal**: Gestor electoral lista solicitudes PENDIENTES cuyo municipio destino esté dentro de su `alcance_operacion`.
Listado paginado.

**Independent Test**: Gestor con alcance DEPARTAMENTAL → `GET /api/v1/admin/inscripcion/solicitudes?page=0&size=20` → lista de
solicitudes de su departamento. Paginación correcta.

### Tests for User Story 2

- [ ] T019 [P] [US2] Unit test `ListarSolicitudesPendientesUseCaseTest`: gestor NACIONAL → todas las pendientes. Gestor
  DEPARTAMENTAL → solo las de su departamento. Gestor MUNICIPAL → solo las de su municipio. Paginación con offset
  correcto. Sin resultados → total cero.
- [ ] T020 [P] [US2] Integration test `AdminInscripcionControllerIntegrationTest`: listar solicitudes como gestor con
  distintos alcances.

### Implementation for User Story 2

- [ ] T021 [US2] Crear `ListarSolicitudesPendientesUseCase` en `application/inscripcion/`:
    - Inyecta `SolicitudCambioInscripcionRepository`, `MunicipioRepository`.
    - Método `ejecutar(int pagina, int tamano, Usuario gestor)`:
        1. Obtener `AlcanceOperacion alcanceGestor = gestor.getAlcanceOperacion()`.
        2. Según alcance:
            - `NACIONAL`: `findAllPendientes(offset, limit)` + `countAllPendientes()`.
            - `DEPARTAMENTAL`: obtener `departamentoId` del municipio del gestor.
              `findPendientesByDepartamentoDestinoId(departamentoId, offset, limit)` + count.
            - `MUNICIPAL`: obtener `municipioId` del gestor.
              `findPendientesByMunicipioDestinoId(municipioId, offset, limit)` + count.
        3. Mapear cada `SolicitudCambioInscripcion` a `SolicitudCambioResponse` incluyendo nombre del votante (buscar
           usuario por `usuarioId`).
        4. Retornar `PaginaResponse<SolicitudCambioResponse>`.
- [ ] T022 [US2] Crear `AdminInscripcionController` en `infrastructure/adapter/in/rest/admin/`:
    - `GET /api/v1/admin/inscripcion/solicitudes`: query params `page` (default 0), `size` (default 20, max 50).
    - Anotar con `@Tag(name = "Administración de Cambios de Inscripción")`.
- [ ] T023 [US2] Agregar al DTO `SolicitudCambioResponse` los campos `String nombreVotante`, `String documentoVotante`
  para que el gestor vea quién solicita.
- [ ] T024 [US2] Integration test: verificar que el listado incluye datos del votante y está filtrado por alcance
  correctamente.

**Checkpoint**: Gestores pueden ver solicitudes pendientes filtradas automáticamente por su alcance.

---

## Phase 4: User Story 3 — Aceptar solicitud de cambio (Priority: P1)

**Goal**: Gestor electoral acepta una solicitud PENDIENTE. El municipio del votante se actualiza al municipio destino.
La solicitud pasa a ACEPTADA.

**Independent Test**: Solicitud PENDIENTE en alcance del gestor → `POST /api/v1/admin/inscripcion/{id}/aceptar` → 200,
municipio del votante cambia. Solicitud ya procesada → 409. Solicitud fuera de alcance → 403. Solicitud inexistente →
404.

### Tests for User Story 3

- [ ] T025 [P] [US3] Unit test `AceptarSolicitudInscripcionUseCaseTest`: solicitud pendiente → aceptada, municipio
  actualizado. Solicitud fuera de alcance → `AlcanceInsuficienteException`. Solicitud no pendiente →
  `SolicitudYaProcesadaException`. Concurrencia: dos gestores aceptan simultáneamente → uno gana, otro recibe 409.
- [ ] T026 [P] [US3] Unit test de integridad transaccional: verificar que `aceptar()` actualiza municipio del usuario y
  solicitud en la misma operación lógica.

### Implementation for User Story 3

- [ ] T027 [US3] Crear `AceptarSolicitudInscripcionUseCase` en `application/inscripcion/`:
    - Inyecta `SolicitudCambioInscripcionRepository`, `UsuarioRepository`, `MunicipioRepository`.
    - Método `ejecutar(UUID solicitudId, Usuario gestor)`:
        1. Busca solicitud por id. Si no existe → `SolicitudYaProcesadaException` (404 semántico).
        2. Si `!solicitud.getEstado().esPendiente()` → `SolicitudYaProcesadaException`.
        3. `GestorElectoral.validarAlcance(gestor, solicitud.getMunicipioDestino())`. Si fuera de alcance →
           `AlcanceInsuficienteException`.
        4. Busca usuario votante por `solicitud.getUsuarioId()`.
        5. Valida que el municipio actual del usuario == `municipioOrigen` de la solicitud. Si no coincide (otra
           solicitud fue aceptada antes) → `SolicitudYaProcesadaException`.
        6. `solicitud.aceptar(gestor.getId())` → aplica estado ACEPTADA.
        7. Actualiza municipio del usuario: `votante.setMunicipio(solicitud.getMunicipioDestino())`,
           `votante.validateInfo()`.
        8. Persiste ambos en secuencia: `solicitudRepository.update(solicitud).then(usuarioRepository.save(votante))`.
        9. Retorna `SolicitudCambioResponse`.
    - **Control de concurrencia**: usar `UPDATE ... WHERE id = :id AND estado = 'PENDIENTE'` en el repositorio para que
      solo una actualización tenga éxito. Si `update` retorna 0 filas afectadas → `SolicitudYaProcesadaException`.
- [ ] T028 [US3] Modificar `SolicitudCambioInscripcionR2dbcRepository`:
    - Método `update(SolicitudCambioInscripcion)` debe ser atómico:
      `UPDATE solicitud_cambio_inscripcion SET estado = :estado, gestor_id = :gestorId, fecha_resolucion = :fechaResolucion WHERE id = :id AND estado = 'PENDIENTE'`.
      Retorna `Mono<Long>` (filas afectadas). Si es 0, el use case lanza `SolicitudYaProcesadaException`.
- [ ] T029 [US3] Agregar endpoint a `AdminInscripcionController`:
    - `POST /api/v1/admin/inscripcion/{id}/aceptar`: path variable UUID, invoca use case, retorna 200 con
      `SolicitudCambioResponse`.
- [ ] T030 [US3] Integration test: flujo aceptar → verificar municipio actualizado, verificar concurrencia con dos
  requests simultáneos.

**Checkpoint**: Aceptación de solicitudes funcional con control de concurrencia.

---

## Phase 5: User Story 4 — Rechazar solicitud de cambio (Priority: P1)

**Goal**: Gestor electoral rechaza una solicitud PENDIENTE con un motivo obligatorio. El municipio del votante no
cambia. La solicitud pasa a RECHAZADA.

**Independent Test**: Solicitud PENDIENTE → `POST /api/v1/admin/inscripcion/{id}/rechazar` con motivo → 200. Sin motivo → 422.
Solicitud ya procesada → 409. Fuera de alcance → 403.

### Tests for User Story 4

- [ ] T031 [P] [US4] Unit test `RechazarSolicitudInscripcionUseCaseTest`: rechazo exitoso con motivo. Motivo vacío →
  `MotivoRequeridoException`. Solicitud no pendiente → `SolicitudYaProcesadaException`. Fuera de alcance →
  `AlcanceInsuficienteException`.
- [ ] T032 [P] [US4] Unit test: verificar que el municipio del votante no cambia tras el rechazo.

### Implementation for User Story 4

- [ ] T033 [P] [US4] Crear `RechazarSolicitudRequest` DTO: `String motivoRechazo` con `@NotBlank` y
  `@Size(min = 10, max = 500)`.
- [ ] T034 [US4] Crear `RechazarSolicitudInscripcionUseCase` en `application/inscripcion/`:
    - Inyecta `SolicitudCambioInscripcionRepository`.
    - Método `ejecutar(UUID solicitudId, String motivoRechazo, Usuario gestor)`:
        1. Busca solicitud por id. Si no existe → `SolicitudYaProcesadaException`.
        2. Si `!solicitud.getEstado().esPendiente()` → `SolicitudYaProcesadaException`.
        3. `GestorElectoral.validarAlcance(gestor, solicitud.getMunicipioDestino())`.
        4. Si `motivoRechazo` es null o blank → `MotivoRequeridoException`.
        5. `solicitud.rechazar(gestor.getId(), motivoRechazo)` → valida internamente que el motivo no esté vacío.
        6. Persiste con `solicitudRepository.update(solicitud)`.
        7. Retorna `SolicitudCambioResponse`.
    - Mismo mecanismo de control de concurrencia que US3: `UPDATE WHERE id = :id AND estado = 'PENDIENTE'`.
- [ ] T035 [US4] Agregar endpoint a `AdminInscripcionController`:
    - `POST /api/v1/admin/inscripcion/{id}/rechazar`: path variable UUID, recibe
      `@Valid @RequestBody RechazarSolicitudRequest`, invoca use case, retorna 200 con `SolicitudCambioResponse`.
- [ ] T036 [US4] Integration test: flujo rechazo → verificar que municipio del votante no cambia.

**Checkpoint**: Rechazo de solicitudes funcional con motivo obligatorio y control de concurrencia.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de que los planes 001 y 002 (Foundational + seguridad JWT + roles) estén
  completos. BLOQUEA todos los user stories de este plan.
- **US1 — Solicitar cambio (Phase 2)**: Depende de Phase 1.
- **US2 — Listar pendientes (Phase 3)**: Depende de Phase 1. Puede iniciarse en paralelo con US1.
- **US3 — Aceptar (Phase 4)**: Depende de Phase 1. Puede iniciarse en paralelo con US1 y US2.
- **US4 — Rechazar (Phase 5)**: Depende de Phase 1. Puede iniciarse en paralelo.

### User Story Dependencies

- **US1 (P1)**: Solo depende de Foundational.
- **US2 (P1)**: Solo depende de Foundational. Independiente de US1.
- **US3 (P1)**: Solo depende de Foundational. Usa `update` atómico del repositorio.
- **US4 (P1)**: Solo depende de Foundational. Lógica similar a US3.
- Todos los user stories pueden implementarse en paralelo una vez completada la Phase 1.

### Within Each User Story

- Repositorio y entidad primero (Phase 1 cubre esto para todos).
- DTOs y mapper junto con el controlador.
- Caso de uso antes que controlador.
- Tests unitarios de dominio y caso de uso, luego tests de integración del endpoint.

---

## Notes

- **Control de concurrencia**: el método `update` del repositorio usa `UPDATE ... WHERE estado = 'PENDIENTE'` como
  mecanismo de locking optimista. Si dos gestores intentan procesar la misma solicitud, solo uno afecta filas. El otro
  recibe `SolicitudYaProcesadaException`.
- **Cancelación automática**: cuando un usuario se suspende (cambia a `INACTIVO`) desde el plan 002, se debe invocar
  `SolicitudCambioInscripcionRepository.cancelarPendientesPorUsuarioId()`. Esto debe ejecutarse en la misma transacción.
  Si R2DBC no soporta transacciones multi-repositorio fácilmente, usar `@Transactional` de Spring (requiere
  `EnableTransactionManagement` y un `TransactionManager` reactivo).
- **Eliminación de municipio**: si un municipio se elimina de la jerarquía geográfica (caso infrecuente pero posible),
  se debe invocar `cancelarPendientesPorMunicipioDestinoId(municipioId, "Municipio eliminado del sistema")`. Esto podría
  ser un evento de dominio en el futuro.
- El `SolicitudCambioResponse` incluye `nombreVotante` y `documentoVotante`. En US2 (listar), se necesita un join o una
  consulta adicional para obtenerlos. Considerar una vista o un DTO proyectado desde el repositorio si el rendimiento es
  crítico.
- La validación `validateMotivo()` en `SolicitudCambioInscripcion` exige mínimo 10 caracteres.
  `MotivoRequeridoException` en el rechazo solo verifica no vacío. Son dos validaciones distintas: una para el motivo de
  solicitud (min 10) y otra para el motivo de rechazo (no vacío).
