# Implementation Plan: Gestión de Usuarios

**Date**: 2026-06-13  
**Spec**: [002-gestion-usuarios.md](../spec/002-gestion-usuarios.md)

## Summary

Implementar las operaciones de administración de usuarios para el gestor electoral: modificar estado individual y masivo
de votantes, listar usuarios paginados por lugar de inscripción, y buscar usuarios por número de documento. Todas las
operaciones están restringidas por el `alcance_operacion` del gestor (NACIONAL | DEPARTAMENTAL | MUNICIPAL).

**Technical approach**: Extender el modelo de dominio con `alcance_operacion`, implementar casos de uso en
`application/usuario/` que validen alcance antes de cada operación, y exponer endpoints bajo el prefijo `/admin/users`.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x (WebFlux), Spring Data R2DBC, Flyway, jjwt, Lombok, MapStruct,
springdoc-openapi, Spring Boot Actuator, Bean Validation  
**Storage**: PostgreSQL 16 (via R2DBC)  
**Testing**: JUnit 5, Mockito, WebTestClient, Testcontainers  
**Target Platform**: Linux server (Docker)  
**Project Type**: Microservice REST reactivo — extensión del plan 001  
**Performance Goals**: Modificación individual < 1s, modificación masiva sobre 10k usuarios < 5s, búsqueda < 500ms  
**Constraints**: Transiciones de estado restringidas, gestor no puede modificar a otro gestor, alcance geográfico limita
visibilidad  
**Scale/Scope**: ~10k votantes, 3 gestores (nacional, departamental, municipal)

## Project Structure

### Documentation (this feature)

```text
docs/
├── plan/
│   └── 002-gestion-usuarios.md        # Este archivo
├── spec/
│   └── 002-gestion-usuarios.md
└── stakeholders.md
```

### Source Code (repository root — nuevo y modificado)

```text
src/main/java/com/safevoting/users/
│
├── domain/
│   ├── model/
│   │   ├── usuario/
│   │   │   ├── Usuario.java               # MODIFICAR: agregar alcanceOperacion
│   │   │   ├── GestorElectoral.java       # NUEVO: lógica de alcance
│   │   │   └── AlcanceOperacion.java      # NUEVO: NACIONAL, DEPARTAMENTAL, MUNICIPAL
│   │   └── exception/
│   │       ├── usuario/
│   │       │   ├── UsuarioNoEncontradoException.java
│   │       │   ├── AlcanceInsuficienteException.java
│   │       │   ├── TransicionEstadoInvalidaException.java
│   │       │   └── GestorNoModificableException.java
│   └── repository/
│       └── UsuarioRepository.java         # MODIFICAR: nuevos métodos de consulta
│
├── application/
│   └── usuario/
│       ├── CambiarEstadoIndividualUseCase.java
│       ├── CambiarEstadoMasivoPorAlcanceUseCase.java
│       ├── CambiarEstadoMasivoPorMunicipioUseCase.java
│       ├── ListarUsuariosUseCase.java
│       └── BuscarUsuarioPorDocumentoUseCase.java
│
└── infrastructure/
    ├── config/
    │   └── SecurityConfig.java            # MODIFICAR: restringir /api/v1/admin/** a rol GESTOR_ELECTORAL
    └── adapter/
        ├── in/
        │   └── rest/
        │       └── admin/
        │           ├── dto/
        │           │   ├── CambiarEstadoRequest.java
        │           │   ├── CambioMasivoRequest.java
        │           │   ├── UsuarioResponse.java
        │           │   └── PaginaResponse.java    # DTO genérico paginado
        │           ├── mapper/
        │           │   └── UsuarioDtoMapper.java  # MapStruct
        │           └── AdminController.java
        └── out/
            └── persistence/
                └── usuario/
                    └── UsuarioR2dbcRepository.java  # MODIFICAR: nuevas queries

src/main/resources/
└── db/migration/
    └── V2__agregar_alcance_operacion.sql

src/test/java/com/safevoting/users/
├── unit/
│   ├── domain/
│   │   └── model/usuario/
│   │       └── UsuarioAlcanceTest.java
│   └── application/
│       └── usuario/
│           ├── CambiarEstadoIndividualUseCaseTest.java
│           ├── CambiarEstadoMasivoPorAlcanceUseCaseTest.java
│           ├── CambiarEstadoMasivoPorMunicipioUseCaseTest.java
│           ├── ListarUsuariosUseCaseTest.java
│           └── BuscarUsuarioPorDocumentoUseCaseTest.java
└── integration/
    └── rest/
        └── admin/
            └── AdminControllerIntegrationTest.java
```

**Structure Decision**: Misma arquitectura hexagonal del plan 001. El nuevo código extiende `Usuario` sin romper la capa
de dominio existente. Los casos de uso se organizan en `application/usuario/` por entidad de dominio. El controlador
`AdminController` y sus DTOs viven en `infrastructure/adapter/in/rest/admin/`. Las excepciones nuevas se agregan a
`domain/model/exception/usuario/`. Los tests replican la misma organización. Todas las rutas usan el prefijo
`/api/v1/admin/users/**`.

---

## Phase 1: Foundational — Extensión del modelo de dominio (Blocking Prerequisites)

**Purpose**: Extender el modelo de dominio y la base de datos para soportar `alcance_operacion` en gestores y las
transiciones de estado.

**⚠️ CRITICAL**: Ningún user story de administración puede comenzar antes de esta fase.

- [ ] T001 Crear migración Flyway `V2__agregar_alcance_operacion.sql`:
    - Agregar columna `alcance_operacion VARCHAR` a tabla `usuario` (nullable, solo para gestores).
    -
    `ALTER TABLE usuario ADD CONSTRAINT chk_alcance CHECK (alcance_operacion IN ('NACIONAL','DEPARTAMENTAL','MUNICIPAL') OR alcance_operacion IS NULL)`.
- [ ] T002 Crear enum `AlcanceOperacion` junto a su entidad en `domain/model/usuario/`:
    - `NACIONAL`, `DEPARTAMENTAL`, `MUNICIPAL`.
    - Método `cubre(Municipio municipioGestor, Municipio municipioObjetivo)` que retorna `boolean` según la jerarquía
      geográfica.
- [ ] T003 Modificar entidad `Usuario`:
    - Agregar campo `alcanceOperacion` (`AlcanceOperacion`, nullable).
    - Agregar `validateAlcance()` (privado): si `alcanceOperacion != null`, debe ser uno de los valores del enum.
    - `validateInfo()` ahora también invoca `validateAlcance()`.
    - Agregar método `esGestor()` → `return this.rol == Rol.GESTOR_ELECTORAL`.
    - Agregar método `getAlcanceEfectivo()`: si no es gestor, lanza `RolInvalidoException`.
- [ ] T004 Crear `GestorElectoral` en `domain/model/`:
    - No es una entidad separada, es una clase de utilidad que encapsula lógica de alcance para un `Usuario` con rol
      `GESTOR_ELECTORAL`.
    - Método estático `validarAlcance(Usuario gestor, Municipio municipioObjetivo)`: si el municipio está fuera del
      alcance → `AlcanceInsuficienteException`.
    - Método estático `validarNoEsGestor(Usuario objetivo)`: si el objetivo es gestor → `GestorNoModificableException`.
- [ ] T005 Crear excepciones de dominio nuevas:
    - `AlcanceInsuficienteException(String mensaje)` → errorCode `ALCANCE_INSUFICIENTE`.
    - `TransicionEstadoInvalidaException(EstadoUsuario origen, EstadoUsuario destino)` → errorCode
      `TRANSICION_INVALIDA`.
    - `GestorNoModificableException()` → errorCode `GESTOR_NO_MODIFICABLE`.
    - `UsuarioNoEncontradoException(String documento)` → errorCode `USUARIO_NO_ENCONTRADO`.
- [ ] T006 Modificar `UsuarioRepository` (puerto en `domain/repository/`):
    - Agregar `findByDocumento(DocumentoIdentidad documento)` → `Mono<Usuario>`.
    - Agregar `findByMunicipioId(UUID municipioId, Pageable pageable)` → `Flux<Usuario>`.
    - Agregar `countByMunicipioId(UUID municipioId)` → `Mono<Long>`.
    - Agregar `findByDepartamentoId(UUID departamentoId, Pageable pageable)` → `Flux<Usuario>`.
    - Agregar `updateEstadoBatch(UUID municipioId, EstadoUsuario nuevoEstado)` → `Mono<Long>` (retorna conteo de
      actualizados).
    - Agregar `updateEstadoBatchByDepartamento(UUID departamentoId, EstadoUsuario nuevoEstado)` → `Mono<Long>`.
    - Agregar `updateEstadoBatchNacional(EstadoUsuario nuevoEstado)` → `Mono<Long>`.
- [ ] T007 Modificar `UsuarioR2dbcRepository` (adaptador out):
    - Implementar todos los nuevos métodos del puerto usando `R2dbcEntityTemplate` y SQL parametrizado.
    - `updateEstadoBatch`:
      `UPDATE usuario SET estado = :nuevoEstado WHERE municipio_id = :municipioId AND rol = 'VOTANTE' AND estado != :nuevoEstado`.
    - El `Pageable` se traduce a `LIMIT :size OFFSET :offset` manual (R2DBC no soporta `Page` nativamente).
- [ ] T008 Modificar `GlobalExceptionHandler`:
    - Agregar mapeos:
        - `AlcanceInsuficienteException` → 403
        - `TransicionEstadoInvalidaException` → 422
        - `GestorNoModificableException` → 403
        - `UsuarioNoEncontradoException` → 404
- [ ] T009 Modificar `SecurityConfig`:
    - Restringir `/admin/**` a `hasRole('GESTOR_ELECTORAL')`. → Ahora: `.pathMatchers("/api/v1/admin/**").hasRole("GESTOR_ELECTORAL")`.
    - Extraer `rol` del JWT y mapearlo a `SimpleGrantedAuthority("ROLE_GESTOR_ELECTORAL")` o
      `SimpleGrantedAuthority("ROLE_VOTANTE")`.
    - Agregar `SecurityWebFilterChain` con `.pathMatchers("/admin/**").hasRole("GESTOR_ELECTORAL")`.
- [ ] T010 Modificar `JwtProvider`:
    - `generateToken(email, rol)`: incluir claim `rol` en el JWT.
    - Método `extractRol(String token)` → `String`.

**Checkpoint**: Modelo de dominio extendido con alcance. Seguridad por roles implementada. Los gestores pueden acceder a
`/admin/**`, los votantes reciben 403.

---

## Phase 2: User Story 1 — Modificar estado de un usuario individual (Priority: P1)

**Goal**: Un gestor electoral cambia el estado de un usuario específico identificado por número de documento, siempre
que esté dentro de su alcance.

**Independent Test**: Autenticar gestor con alcance DEPARTAMENTAL, `PATCH /api/v1/admin/users/123456789/estado` con body
`{"estado": "HABILITADO"}` → 200. Usuario fuera de alcance → 403. Transición inválida → 422. Documento de otro gestor →
403.

### Tests for User Story 1

- [ ] T011 [P] [US1] Unit test `UsuarioAlcanceTest`: `GestorElectoral.validarAlcance()`: municipio dentro de alcance →
  OK. Fuera → `AlcanceInsuficienteException`. Gestor NACIONAL cubre todo. Gestor MUNICIPAL solo cubre su municipio.
- [ ] T012 [P] [US1] Unit test `CambiarEstadoIndividualUseCaseTest`: mock repos → cambio exitoso. Usuario fuera de
  alcance → `AlcanceInsuficienteException`. Transición inválida → `TransicionEstadoInvalidaException`. Objetivo es
  gestor → `GestorNoModificableException`. Documento no existe → `UsuarioNoEncontradoException`.

### Implementation for User Story 1

- [ ] T013 [P] [US1] Crear `CambiarEstadoRequest` DTO: `String estado` con `@NotBlank`, validación personalizada
  `@EstadoValido` (debe ser ACTIVO, HABILITADO o INACTIVO).
- [ ] T014 [P] [US1] Crear `UsuarioResponse` DTO: `id`, `nombre`, `email`, `documento`, `municipioNombre`,
  `departamentoNombre`, `estado`, `rol`.
- [ ] T015 [P] [US1] Crear `UsuarioDtoMapper` (MapStruct): `Usuario → UsuarioResponse`.
- [ ] T016 [US1] Crear `CambiarEstadoIndividualUseCase` en `application/usuario/`:
    - Inyecta `UsuarioRepository`, `MunicipioRepository`.
    - Método `ejecutar(String documentoObjetivo, EstadoUsuario nuevoEstado, Usuario gestor)`:
        1. Busca usuario objetivo por documento. Si no existe → `UsuarioNoEncontradoException`.
        2. `GestorElectoral.validarNoEsGestor(objetivo)`. Si es gestor → `GestorNoModificableException`.
        3. `GestorElectoral.validarAlcance(gestor, objetivo.getMunicipio())`.
        4. Validar transición: mapeo de transiciones permitidas. Si no es válida → `TransicionEstadoInvalidaException`.
        5. Aplicar cambio: `objetivo.setEstado(nuevoEstado)`, `objetivo.validateInfo()`.
        6. Persistir y retornar `Usuario` actualizado.
- [ ] T017 [US1] Crear `AdminController` en `infrastructure/adapter/in/rest/admin/`:
    - `PATCH /api/v1/admin/users/{documento}/estado`: extrae gestor del `SecurityContext` (el `email` del JWT), busca
      `Usuario` gestor por email, invoca `CambiarEstadoIndividualUseCase.ejecutar()`, retorna `UsuarioResponse`. Wrap en
      `Mono<ResponseEntity<UsuarioResponse>>`.
    - Método helper privado `getGestorFromContext()`: extrae email del JWT → busca Usuario → valida que sea
      GESTOR_ELECTORAL.
- [ ] T018 [US1] Anotar `AdminController` con `@Tag(name = "Administración de Usuarios")` para Swagger.
- [ ] T019 [US1] Integration test `AdminControllerIntegrationTest`: autenticar gestor →
  `PATCH /api/v1/admin/users/{documento}/estado` → 200 con `UsuarioResponse`. Sin token → 401. Con token de votante →
  403. Con gestor fuera de alcance → 403.

**Checkpoint**: Modificación de estado individual funcional y protegida por alcance y transiciones.

---

## Phase 3: User Story 2 — Modificación masiva por alcance geográfico (Priority: P1)

**Goal**: Un gestor electoral cambia el estado de todos los usuarios cuyo lugar de inscripción corresponde a un
alcance (`NACIONAL`, `DEPARTAMENTAL`, `MUNICIPAL`), restringido por su propio alcance.

**Independent Test**: Gestor DEPARTAMENTAL modifica con `alcance=MUNICIPAL` + `municipioId=X` → usuarios de X cambian de
estado. Gestor con `alcance=MUNICIPAL` intenta `alcance=DEPARTAMENTAL` → 403. Sin usuarios coincidentes → conteo 0.

### Tests for User Story 2

- [ ] T020 [P] [US2] Unit test `CambiarEstadoMasivoPorAlcanceUseCaseTest`: gestor NACIONAL + alcance DEPARTAMENTAL →
  todos los usuarios del departamento cambian. Gestor DEPARTAMENTAL + alcance NACIONAL → `AlcanceInsuficienteException`.
  Sin usuarios → retorna conteo 0.
- [ ] T021 [P] [US2] Unit test `UsuarioR2dbcRepository` (updateEstadoBatch): verificar que SQL excluye usuarios con rol
  GESTOR_ELECTORAL.

### Implementation for User Story 2

- [ ] T022 [P] [US2] Crear `CambioMasivoRequest` DTO: `String alcance` (`NACIONAL`, `DEPARTAMENTAL`, `MUNICIPAL`),
  `UUID departamentoId` (requerido si alcance = DEPARTAMENTAL), `UUID municipioId` (requerido si alcance = MUNICIPAL),
  `String estado` con `@EstadoValido`. Bean Validation: lógica condicional según alcance.
- [ ] T023 [US2] Crear `CambiarEstadoMasivoPorAlcanceUseCase` en `application/usuario/`:
    - Inyecta `UsuarioRepository`, `MunicipioRepository`.
    - Método
      `ejecutar(AlcanceOperacion alcanceSolicitado, UUID departamentoId, UUID municipioId, EstadoUsuario nuevoEstado, Usuario gestor)`:
        1. Validar que `alcanceSolicitado` no exceda el `alcance_operacion` del gestor:
            - Si gestor es MUNICIPAL y alcanceSolicitado es DEPARTAMENTAL o NACIONAL → `AlcanceInsuficienteException`.
            - Si gestor es DEPARTAMENTAL y alcanceSolicitado es NACIONAL → `AlcanceInsuficienteException`.
        2. Si alcanceSolicitado es MUNICIPAL: validar municipioId está dentro del alcance del gestor. Ejecutar
           `updateEstadoBatch(municipioId, nuevoEstado)`.
        3. Si alcanceSolicitado es DEPARTAMENTAL: validar departamentoId está dentro del alcance del gestor. Ejecutar
           `updateEstadoBatchByDepartamento(departamentoId, nuevoEstado)`.
        4. Si alcanceSolicitado es NACIONAL: solo gestor NACIONAL. Ejecutar `updateEstadoBatchNacional(nuevoEstado)`.
        5. Retornar conteo de usuarios modificados (`Mono<Long>`).
- [ ] T024 [US2] Agregar endpoint a `AdminController`:
    - `PATCH /api/v1/admin/users/estado/masivo`: recibe `@Valid @RequestBody CambioMasivoRequest`, invoca
      `CambiarEstadoMasivoPorAlcanceUseCase.ejecutar()`, retorna `200` con `{"usuariosModificados": N}`.
- [ ] T025 [US2] Integration test: gestor NACIONAL modifica masivamente por departamento. Verificar conteo y que
  usuarios en DB cambiaron.

**Checkpoint**: Modificación masiva por alcance funcional. Todos los niveles de alcance operan correctamente.

---

## Phase 4: User Story 3 — Modificación masiva por municipio de inscripción (Priority: P1)

**Goal**: Similar a US2 pero filtrando directamente por `municipio_id`. El gestor especifica el municipio exacto.

**Independent Test**: Gestor DEPARTAMENTAL →
`PATCH /api/v1/admin/users/estado/masivo/municipio/{municipioId}?estado=INACTIVO` → usuarios de ese municipio cambian.
Municipio fuera de alcance → 403.

### Tests for User Story 3

- [ ] T026 [P] [US3] Unit test `CambiarEstadoMasivoPorMunicipioUseCaseTest`: municipio dentro de alcance → cambio
  exitoso. Municipio fuera de alcance → `AlcanceInsuficienteException`. Municipio inexistente →
  `MunicipioNoEncontradoException`.

### Implementation for User Story 3

- [ ] T027 [US3] Crear `CambiarEstadoMasivoPorMunicipioUseCase` en `application/usuario/`:
    - Inyecta `UsuarioRepository`, `MunicipioRepository`.
    - Método `ejecutar(UUID municipioId, EstadoUsuario nuevoEstado, Usuario gestor)`:
        1. Busca municipio. Si no existe → `MunicipioNoEncontradoException`.
        2. `GestorElectoral.validarAlcance(gestor, municipio)`.
        3. `updateEstadoBatch(municipioId, nuevoEstado)`.
        4. Retorna conteo.
- [ ] T028 [US3] Agregar endpoint a `AdminController`:
    - `PATCH /api/v1/admin/users/estado/masivo/municipio/{municipioId}`: query param `estado`, invoca use case. Retorna
      `200` con conteo.
- [ ] T029 [US3] Integration test: flujo completo con municipio dentro y fuera del alcance.

**Checkpoint**: Modificación masiva por municipio funcional.

---

## Phase 5: User Story 4 — Listar usuarios por lugar de inscripción (Priority: P2)

**Goal**: Gestor electoral consulta listado paginado de usuarios filtrado por municipio de inscripción.

**Independent Test**: `GET /api/v1/admin/users?municipioId=X&page=0&size=20` → devuelve lista paginada. Municipio fuera de
alcance → 403. Página sin resultados → lista vacía.

### Tests for User Story 4

- [ ] T030 [P] [US4] Unit test `ListarUsuariosUseCaseTest`: municipio en alcance → retorna lista paginada. Municipio
  fuera de alcance → `AlcanceInsuficienteException`. Paginación con offset correcto.
- [ ] T031 [P] [US4] Unit test `PaginaResponse`: estructura genérica con `contenido`, `pagina`, `tamano`,
  `totalElementos`, `totalPaginas`.

### Implementation for User Story 4

- [ ] T032 [P] [US4] Crear `PaginaResponse<T>` DTO genérico: `List<T> contenido`, `int pagina`, `int tamano`,
  `long totalElementos`, `int totalPaginas`.
- [ ] T033 [US4] Crear `ListarUsuariosUseCase` en `application/usuario/`:
    - Inyecta `UsuarioRepository`, `MunicipioRepository`.
    - Método `ejecutar(UUID municipioId, int pagina, int tamano, Usuario gestor)`:
        1. Busca municipio. Si no existe → `MunicipioNoEncontradoException`.
        2. `GestorElectoral.validarAlcance(gestor, municipio)`.
        3. `findByMunicipioId(municipioId, pageable)` + `countByMunicipioId(municipioId)`.
        4. Mapea cada `Usuario` a `UsuarioResponse`.
        5. Retorna `PaginaResponse<UsuarioResponse>`.
- [ ] T034 [US4] Agregar endpoint a `AdminController`:
    - `GET /api/v1/admin/users`: query params `municipioId` (requerido), `page` (default 0), `size` (default 20, max 100).
- [ ] T035 [US4] Integration test: listar usuarios paginados, verificar estructura de `PaginaResponse`.

**Checkpoint**: Listado de usuarios paginado y filtrado por municipio funcional.

---

## Phase 6: User Story 5 — Buscar usuario por número de cédula (Priority: P2)

**Goal**: Gestor busca un usuario específico por número de documento, restringido a su alcance. No revela existencia de
usuarios fuera de alcance ni de otros gestores.

**Independent Test**: `GET /api/v1/admin/users/123456789` → 200 con UsuarioResponse. Usuario fuera de alcance → 404. Usuario es
gestor → 404. Documento no existe → 404.

### Tests for User Story 5

- [ ] T036 [P] [US5] Unit test `BuscarUsuarioPorDocumentoUseCaseTest`: documento existe y dentro de alcance → retorna
  UsuarioResponse. Fuera de alcance → `UsuarioNoEncontradoException` (para no revelar existencia). Objetivo es gestor →
  `UsuarioNoEncontradoException`. Documento no existe → `UsuarioNoEncontradoException`.

### Implementation for User Story 5

- [ ] T037 [US5] Crear `BuscarUsuarioPorDocumentoUseCase` en `application/usuario/`:
    - Inyecta `UsuarioRepository`.
    - Método `ejecutar(String documento, Usuario gestor)`:
        1. Busca usuario por documento. Si no existe → `UsuarioNoEncontradoException`.
        2. Si `objetivo.esGestor()` → `UsuarioNoEncontradoException` (no revelar).
        3. `GestorElectoral.validarAlcance(gestor, objetivo.getMunicipio())`. Si fuera de alcance →
           `UsuarioNoEncontradoException` (no revelar).
        4. Retorna `UsuarioResponse`.
- [ ] T038 [US5] Agregar endpoint a `AdminController`:
    - `GET /api/v1/admin/users/{documento}`: path variable documento, invoca use case, retorna 200 con `UsuarioResponse`
      o 404.
- [ ] T039 [US5] Integration test: búsqueda de usuario existente, no existente, y fuera de alcance (todos → 404 para no
  filtrar información).

**Checkpoint**: Búsqueda por documento funcional con ocultación de información fuera de alcance.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 1)**: Depende de que el plan 001 (Foundational) esté completo. BLOQUEA todos los user stories de
  este plan.
- **US1 (Phase 2)**: Depende de Phase 1.
- **US2 (Phase 3)**: Depende de Phase 1. Puede iniciarse en paralelo con US1.
- **US3 (Phase 4)**: Depende de Phase 1. Puede iniciarse en paralelo con US1 y US2.
- **US4 (Phase 5)**: Depende de Phase 1. Paralelizable.
- **US5 (Phase 6)**: Depende de Phase 1. Paralelizable.

### User Story Dependencies

- **US1 (P1)**: Solo depende de Foundational.
- **US2 (P1)**: Solo depende de Foundational. Independiente de US1.
- **US3 (P1)**: Solo depende de Foundational. Similar a US2.
- **US4 (P2)**: Solo depende de Foundational.
- **US5 (P2)**: Solo depende de Foundational.
- Todos los user stories pueden implementarse en paralelo.

### Within Each User Story

- Repositorio extendido primero (nuevos métodos en puerto + adapter).
- Caso de uso antes que controlador.
- DTOs y mappers junto con el controlador.
- Test unitario de caso de uso, luego test de integración del endpoint.

---

## Notes

- La búsqueda por documento y el listado por municipio **no deben revelar** la existencia de usuarios fuera del alcance
  del gestor. Esto se logra lanzando `UsuarioNoEncontradoException` (404) en lugar de `AlcanceInsuficienteException` (
  403) en estos endpoints de lectura.
- Las operaciones masivas (`updateEstadoBatch`) deben excluir usuarios con rol `GESTOR_ELECTORAL` en la cláusula WHERE
  del SQL.
- `Pageable` de Spring Data no es reactivo-compatible con R2DBC. Se implementa manualmente con `LIMIT` y `OFFSET`.
  Considerar migrar a `spring-data-r2dbc` con `Page` si se añade soporte en versiones futuras.
- El `AdminController.helper getGestorFromContext()` se reutiliza en todos los endpoints admin. Si crece en complejidad,
  extraer a un `GestorContextService` en `application/`.
- La validación de alcance `cubre()` del enum `AlcanceOperacion` compara departamento_id del municipio del gestor con el
  departamento_id del municipio objetivo para alcance DEPARTAMENTAL. Para MUNICIPAL, compara `municipio_id`
  directamente. Para NACIONAL, siempre true.
