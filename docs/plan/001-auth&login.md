# Implementation Plan: Autenticación y Registro

**Date**: 2026-06-13  
**Spec**: [001-auth&login.md](../spec/001-auth&login.md)

## Summary

Implementar el registro de votantes y la autenticación mediante OTP enviado por correo electrónico para votantes y
gestores electorales. Solo usuarios en estado `HABILITADO` pueden autenticarse exitosamente. El OTP expira a los 5
minutos con máximo 3 intentos fallidos.

**Technical approach**: Arquitectura hexagonal con Spring Boot WebFlux, R2DBC + PostgreSQL, JWT para sesiones, y
entidades de dominio con validación interna mediante Builder + `validateInfo()`.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x (WebFlux), Spring Data R2DBC, Flyway, jjwt, Lombok, MapStruct,
springdoc-openapi (Swagger), Spring Boot Actuator, spring-boot-starter-mail, Bean Validation (jakarta.validation)  
**Storage**: PostgreSQL 16 (via R2DBC)  
**Testing**: JUnit 5, Mockito, WebTestClient, Testcontainers (PostgreSQL)  
**Target Platform**: Linux server (Docker)  
**Project Type**: Microservice REST reactivo  
**Performance Goals**: <500ms p95 en endpoints de auth, soportar 500 solicitudes OTP simultáneas  
**Constraints**: Email y documento únicos, OTP 6 caracteres, expira 5 min, 3 intentos máx.  
**Scale/Scope**: Módulo de usuarios del sistema Safe-Voting; ~10k votantes iniciales

## Project Structure

### Documentation (this feature)

```text
docs/
├── plan/
│   └── 001-auth&login.md              # Este archivo
├── spec/
│   └── 001-auth&login.md
└── stakeholders.md
```

### Source Code (repository root)

```text
src/main/java/com/safevoting/users/
│
├── domain/
│   ├── shared/                              # Tipos reutilizables con validación propia
│   │   ├── Email.java                       # Value object: valida formato email
│   │   └── DocumentoIdentidad.java          # Value object: valida formato numérico
│   ├── model/
│   │   ├── usuario/
│   │   │   ├── Usuario.java                 # Usa Email, DocumentoIdentidad
│   │   │   ├── EstadoUsuario.java           # ACTIVO, HABILITADO, INACTIVO
│   │   │   └── Rol.java                     # VOTANTE, GESTOR_ELECTORAL
│   │   ├── otp/
│   │   │   ├── Otp.java
│   │   │   └── EstadoOtp.java               # ACTIVO, USADO, INVALIDADO
│   │   ├── geografia/
│   │   │   ├── Municipio.java
│   │   │   └── Departamento.java
│   │   └── exception/
│   │       ├── usuario/
│   │       │   ├── EmailDuplicadoException.java
│   │       │   ├── DocumentoDuplicadoException.java
│   │       │   ├── EmailNoRegistradoException.java
│   │       │   └── UsuarioNoHabilitadoException.java
│   │       ├── otp/
│   │       │   ├── OtpExpiradoException.java
│   │       │   ├── OtpInvalidoException.java
│   │       │   └── ReintentosExcedidosException.java
│   │       ├── geografia/
│   │       │   └── MunicipioNoEncontradoException.java
│   │       └── comun/
│   │           ├── DomainException.java      # Base
│   │           ├── DatosInvalidosException.java
│   │           └── RolInvalidoException.java
│   └── repository/                           # Puertos (interfaces)
│       ├── UsuarioRepository.java
│       ├── OtpRepository.java
│       ├── MunicipioRepository.java
│       ├── DepartamentoRepository.java
│       └── EmailSender.java
│
├── application/
│   └── auth/
│       ├── RegisterVotanteUseCase.java
│       ├── RequestOtpUseCase.java
│       └── VerifyOtpUseCase.java
│
└── infrastructure/
    ├── config/
    │   ├── AppConfig.java
    │   ├── CorsConfig.java
    │   ├── SecurityConfig.java
    │   ├── JwtProvider.java
    │   ├── JwtFilter.java
    │   └── WebFluxConfig.java
    └── adapter/
        ├── in/
        │   ├── rest/
        │   │   ├── auth/
        │   │   │   ├── dto/
        │   │   │   │   ├── RegisterRequest.java
        │   │   │   │   ├── OtpRequest.java
        │   │   │   │   ├── OtpVerifyRequest.java
        │   │   │   │   └── AuthResponse.java
        │   │   │   ├── mapper/
        │   │   │   │   └── AuthDtoMapper.java    # MapStruct
        │   │   │   └── AuthController.java
        │   │   └── common/
        │   │       ├── ApiErrorResponse.java
        │   │       └── GlobalExceptionHandler.java
        │   └── scheduler/
        │       └── OtpExpirationScheduler.java
        └── out/
            ├── persistence/
            │   ├── usuario/
            │   │   └── UsuarioR2dbcRepository.java
            │   ├── otp/
            │   │   └── OtpR2dbcRepository.java
            │   └── geografia/
            │       ├── MunicipioR2dbcRepository.java
            │       └── DepartamentoR2dbcRepository.java
            └── email/
                └── EmailSenderAdapter.java

src/main/resources/
├── application.yml
├── application-dev.yml
└── db/migration/
    └── V1__crear_tablas_iniciales.sql

src/test/java/com/safevoting/users/
├── unit/
│   ├── domain/
│   │   ├── shared/
│   │   │   ├── EmailTest.java
│   │   │   └── DocumentoIdentidadTest.java
│   │   ├── model/
│   │   │   ├── usuario/
│   │   │   │   └── UsuarioTest.java
│   │   │   └── otp/
│   │   │       └── OtpTest.java
│   └── application/
│       └── auth/
│           ├── RegisterVotanteUseCaseTest.java
│           ├── RequestOtpUseCaseTest.java
│           └── VerifyOtpUseCaseTest.java
└── integration/
    └── rest/
        └── auth/
            ├── AuthControllerIntegrationTest.java
            └── OtpFlowIntegrationTest.java

docker-compose.yml
Dockerfile
build.gradle.kts
settings.gradle.kts
```

**Structure Decision**: Arquitectura hexagonal con tres módulos de capa. `domain/shared/` contiene value objects
(`Email`, `DocumentoIdentidad`) con validación encapsulada, reduciendo la carga de las entidades y haciéndolos
reutilizables en otras entidades. Las excepciones se agrupan por entidad de dominio (`usuario/`, `otp/`, `geografia/`,
`comun/`). Los adaptadores `in/rest/` y `out/persistence/` se organizan por feature para identificar rápidamente
qué maneja cada archivo. Los tests replican la misma jerarquía de carpetas. Todas las rutas REST usan el prefijo
`/api/v1/`.

## Convenciones de código limpio

- **SOLID**: una responsabilidad por clase. `domain/` define contratos, `application/` orquesta, `infrastructure/`
  adapta.
- **Nombres en español** alineados al dominio electoral colombiano.
- **Métodos cortos** (~15 líneas máx.), un solo nivel de abstracción por método.
- **Sin magic numbers**: constantes en enums o clases dedicadas.
- **Sin comentarios redundantes**: el código se explica solo; comentar solo el "por qué" de decisiones no obvias.
- **Tests Given/When/Then**: nombres de método descriptivos en español.
- **Entidades de dominio**: patrón Builder (Lombok `@Builder`), validación post-construcción via `validateInfo()`.
  Mutadores puntuales reutilizan validadores privados.
- **Excepciones**: solo excepciones de dominio en `domain/model/exception/`. Nunca usar `IllegalArgumentException`,
  `IllegalStateException` ni excepciones nativas de Java.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicializar el proyecto, tooling y entorno de desarrollo local.

- [ ] T001 Crear proyecto Gradle con Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`), configurar plugin `java`,
  `org.springframework.boot`, `io.spring.dependency-management`
- [ ] T002 Agregar dependencias: `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql`,
  `flyway-core`, `flyway-database-postgresql`, `spring-boot-starter-mail`, `reactor-core`, `jjwt-api`, `jjwt-impl`,
  `jjwt-jackson`, `lombok`, `mapstruct`, `springdoc-openapi-starter-webflux-ui`, `spring-boot-starter-actuator`,
  `spring-boot-starter-validation`
- [ ] T003 Agregar dependencias de test: `spring-boot-starter-test`, `reactor-test`, `testcontainers`,
  `testcontainers-postgresql`, `mockito-core`
- [ ] T004 Configurar `application.yml` con perfil `dev` (PostgreSQL local), Flyway habilitado, Swagger habilitado
- [ ] T005 Configurar `application-dev.yml` con conexión R2DBC a `localhost:5432/safevoting_users`, credenciales dev,
  mail config apuntando a Mailpit
- [ ] T006 Crear `docker-compose.yml` con servicios `postgres` (16, puerto 5432, volumen `pgdata`, DB
  `safevoting_users`) y `mailpit` (puertos 1025 SMTP + 8025 UI)
- [ ] T007 Crear `Dockerfile` multistage (build con Gradle + runtime con `eclipse-temurin:21-jre-alpine`)
- [ ] T008 Crear `src/main/resources/db/migration/` para Flyway
- [ ] T009 Crear paquete base `com.safevoting.users` con clase `UsersApplication.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestructura base que DEBE estar completa antes de cualquier user story.

**⚠️ CRITICAL**: Ningún caso de uso puede implementarse antes de esta fase.

- [ ] T010 Crear migración Flyway `V1__crear_tablas_iniciales.sql`:
    - Tabla `departamento` (id UUID PK DEFAULT gen_random_uuid(), nombre VARCHAR NOT NULL UNIQUE)
    - Tabla `municipio` (id UUID PK DEFAULT gen_random_uuid(), nombre VARCHAR NOT NULL, departamento_id UUID FK NOT NULL)
    - Tabla `usuario` (id UUID PK DEFAULT gen_random_uuid(), nombre VARCHAR NOT NULL, email VARCHAR UNIQUE NOT NULL,
      telefono VARCHAR, documento VARCHAR UNIQUE NOT NULL, municipio_id UUID FK NOT NULL, rol VARCHAR NOT NULL,
      estado VARCHAR NOT NULL, created_at TIMESTAMP DEFAULT NOW)
    - Tabla `otp` (id UUID PK DEFAULT gen_random_uuid(), email VARCHAR NOT NULL, codigo VARCHAR(6) NOT NULL,
      expiracion TIMESTAMP NOT NULL, intentos INTEGER DEFAULT 0, estado VARCHAR NOT NULL)
    - Seed data: departamentos y municipios de Colombia con UUIDs fijos generados
- [ ] T010.5 Crear value objects en `domain/shared/` (sin anotaciones de framework):
    - `Email` con `@Builder`, campo `valor` (String). `validateInfo()`: valida no nulo, contiene `@`, formato RFC 5322
      simplificado. Lanza `DatosInvalidosException` si falla. Método `getValor()`.
    - `DocumentoIdentidad` con `@Builder`, campo `valor` (String). `validateInfo()`: valida no nulo, solo dígitos,
      longitud entre 5 y 15. Lanza `DatosInvalidosException` si falla. Método `getValor()`.
- [ ] T011 Crear entidades de dominio (sin anotaciones de framework), usando value objects de `domain/shared/`:
    - `domain/model/geografia/Departamento`: `@Builder`, campos `id` (UUID), `nombre`. `validateInfo()` valida `nombre` no vacío.
    - `domain/model/geografia/Municipio`: `@Builder`, campos `id` (UUID), `nombre`, `departamento`. `validateInfo()` valida
      `nombre` no vacío y `departamento` no nulo.
    - `domain/model/usuario/Usuario`: `@Builder`, campos `id`, `nombre`, `email` (tipo `Email`), `telefono`,
      `documento` (tipo `DocumentoIdentidad`), `municipio`, `rol`, `estado`, `createdAt`.
      Métodos: `validateInfo()` (invoca `email.validateInfo()`, `documento.validateInfo()`, `validateNombre()`).
      Los validadores privados `validateEmail()` y `validateDocumento()` delegan a los value objects.
      Mutador `habilitar()` → cambia estado a `HABILITADO` + `validateInfo()`.
      Mutador `suspender()` → cambia a `INACTIVO` + `validateInfo()`.
    - `domain/model/otp/Otp`: `@Builder`, campos `id`, `email` (tipo `Email`), `codigo`, `expiracion`, `intentos`,
      `estado` (activo | usado | invalidado). Métodos: `validateInfo()`, `incrementarIntento()`
      (si llega a 3 → `invalidar()`), `marcarUsado()`, `invalidar()`. `esValido()` → true si ACTIVO + no expirado.
    - Constantes en `Otp`: `CODIGO_LONGITUD = 6`, `MAXIMO_INTENTOS = 3`, `TIEMPO_EXPIRACION_MINUTOS = 5`
- [ ] T012 Crear enums junto a sus entidades en `domain/model/`:
    - `usuario/EstadoUsuario`: `ACTIVO`, `HABILITADO`, `INACTIVO`
    - `usuario/Rol`: `VOTANTE`, `GESTOR_ELECTORAL`
    - `otp/EstadoOtp`: `ACTIVO`, `USADO`, `INVALIDADO`
- [ ] T013 Crear excepciones de dominio en `domain/model/exception/` organizadas por entidad:
    - `usuario/`: `EmailDuplicadoException`, `DocumentoDuplicadoException`, `EmailNoRegistradoException`,
      `UsuarioNoHabilitadoException`
    - `otp/`: `OtpExpiradoException`, `OtpInvalidoException`, `ReintentosExcedidosException`
    - `geografia/`: `MunicipioNoEncontradoException`
    - `comun/`: `DomainException` (base con `errorCode` String), `DatosInvalidosException`, `RolInvalidoException`
- [ ] T014 Crear puertos en `domain/repository/` (interfaces sin anotaciones):
    - `UsuarioRepository`: `findByEmail(Email)`, `findByDocumento(DocumentoIdentidad)`, `save(Usuario)`
    - `OtpRepository`: `save(Otp)`, `findByEmailAndEstado(Email, EstadoOtp)`, `update(Otp)`
    - `MunicipioRepository`: `findById(UUID)`, `findAll()`
    - `DepartamentoRepository`: `findById(UUID)`, `findAll()`
    - `EmailSender`: `enviarOtp(Email email, String codigo)` → retorna `Mono<Void>`
- [ ] T015 Crear adaptadores de persistencia en `infrastructure/adapter/out/persistence/` por entidad:
    - `usuario/UsuarioR2dbcRepository`: implementa `UsuarioRepository` con `R2dbcEntityTemplate`.
      Mapea `Email` ↔ `email VARCHAR`, `DocumentoIdentidad` ↔ `documento VARCHAR`.
    - `otp/OtpR2dbcRepository`: implementa `OtpRepository` con `R2dbcEntityTemplate`.
    - `geografia/MunicipioR2dbcRepository`: implementa `MunicipioRepository`.
    - `geografia/DepartamentoR2dbcRepository`: implementa `DepartamentoRepository`.
- [ ] T016 Crear `EmailSenderAdapter` en `infrastructure/adapter/out/email/` implementando `EmailSender`:
    - Recibe `Email` (value object), extrae `email.getValor()` para el envío.
    - Usa `spring-boot-starter-mail` envuelto en `Mono.fromRunnable` para no bloquear el event loop.
    - Envía correo desde `noreply@safevoting.com` con subject "Código de verificación Safe-Voting".
- [ ] T017 Crear configuración de seguridad en `infrastructure/config/`:
    - `JwtProvider`: generar token con `sub=email`, `rol`, `exp` (15 min access). Validar y extraer claims.
    - `JwtFilter`: extraer token del header `Authorization: Bearer <token>`, validar, poblar `SecurityContext`.
    - `SecurityConfig`: `SecurityWebFilterChain` con rutas públicas (`/api/v1/auth/**`,
      `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`) y el resto autenticadas.
      Deshabilitar CSRF y form login.
- [ ] T018 Crear configuración de infraestructura en `infrastructure/config/`:
    - `AppConfig`: beans de servicio (si aplica).
    - `CorsConfig`: permitir origins `http://localhost:3000` (frontend dev), métodos GET/POST/PATCH/DELETE.
    - `WebFluxConfig`: configurar `WebFluxConfigurer` si se necesita path matching.
    - `FlywayConfig`: habilitar migraciones automáticas al iniciar.
- [ ] T019 Crear `GlobalExceptionHandler` en `infrastructure/adapter/in/rest/common/`:
    - `@RestControllerAdvice` con métodos `@ExceptionHandler` para cada excepción de dominio.
    - Mapeo `DomainException → HttpStatus`:
        - `EmailDuplicadoException` → 409
        - `DocumentoDuplicadoException` → 409
        - `EmailNoRegistradoException` → 404
        - `UsuarioNoHabilitadoException` → 403
        - `OtpExpiradoException` → 401
        - `OtpInvalidoException` → 401
        - `ReintentosExcedidosException` → 401
        - `MunicipioNoEncontradoException` → 404
        - `DatosInvalidosException` → 422
        - `RolInvalidoException` → 403
    - Respuesta unificada `ApiErrorResponse`: `int status`, `String error`, `String mensaje`, `String errorCode`,
      `Instant timestamp`.
    - Handler genérico para `RuntimeException` → 500 con mensaje genérico (no filtrar detalles internos).
- [ ] T020 Crear `ApiErrorResponse` record en `infrastructure/adapter/in/rest/common/`.

**Checkpoint**: Foundation ready — la app arranca con `./gradlew bootRun`, Swagger visible en `/swagger-ui.html`,
migraciones ejecutadas, endpoints de auth expuestos como públicos.

---

## Phase 3: User Story 1 — Registro de Votante (Priority: P1)

**Goal**: Un ciudadano se registra con sus datos y obtiene una cuenta en estado `ACTIVO`.

**Independent Test**: Enviar POST a `/api/v1/auth/register` con datos válidos → 201 con confirmación. Enviar email duplicado →
409. Enviar documento duplicado → 409.

### Tests for User Story 1

- [ ] T021 [P] [US1] Unit test `UsuarioTest`: construir Usuario con Builder → `validateInfo()` exitoso. Construir con
  email inválido → `DatosInvalidosException`. Construir con documento no numérico → `DatosInvalidosException`.
- [ ] T022 [P] [US1] Unit test `RegisterVotanteUseCaseTest`: mock repos → registro exitoso retorna Usuario con estado
  ACTIVO. Email ya existe → `EmailDuplicadoException`. Documento ya existe → `DocumentoDuplicadoException`. Municipio no
  encontrado → `MunicipioNoEncontradoException`.
- [ ] T023 [P] [US1] Integration test `AuthControllerIntegrationTest`: `POST /api/v1/auth/register` con body válido → 201.
  Mismo email → 409. Mismo documento → 409. Body sin nombre → 422. Body con municipio inexistente → 404.

### Implementation for User Story 1

- [ ] T024 [P] [US1] Crear `RegisterRequest` (DTO) en `infrastructure/adapter/in/rest/auth/dto/`: `nombre`, `email`,
  `telefono`, `documento`, `municipioId` (UUID) con validación Bean Validation (`@NotBlank`, `@Email`, `@NotNull`).
- [ ] T025 [P] [US1] Crear `AuthDtoMapper` (MapStruct) en `infrastructure/adapter/in/rest/auth/mapper/`:
  `RegisterRequest → Usuario` (solo mapeo de campos, no lógica).
- [ ] T026 [US1] Crear `RegisterVotanteUseCase` en `application/auth/`:
    - Inyecta `UsuarioRepository`, `MunicipioRepository`.
    - Método `registrar(Usuario usuario)`: valida unicidad de email y documento (lanza excepciones de dominio si se
      violan), verifica municipio existe, asigna `rol = VOTANTE`, `estado = ACTIVO`, `createdAt = Instant.now()`, invoca
      `usuario.validateInfo()`, persiste, retorna `Usuario`.
- [ ] T027 [US1] Crear `AuthController` en `infrastructure/adapter/in/rest/auth/`:
    - `POST /api/v1/auth/register`: recibe `@Valid @RequestBody RegisterRequest`, mapea a Usuario via `AuthDtoMapper`,
      invoca `RegisterVotanteUseCase.registrar()`, retorna `201` con mensaje de confirmación. Wrap en
      `Mono<ResponseEntity<?>>`.
- [ ] T028 [US1] Anotar `AuthController` con `@Tag(name = "Autenticación")` para Swagger.
- [ ] T029 [US1] Verificar que `GlobalExceptionHandler` captura las excepciones de este caso de uso y retorna los HTTP
  status correctos.

**Checkpoint**: Registro de votante 100% funcional. `POST /api/v1/auth/register` crea usuario ACTIVO, valida unicidad,
municipio, y datos obligatorios.

---

## Phase 4: User Story 2 — Login de Votante con OTP (Priority: P1)

**Goal**: Un votante HABILITADO solicita OTP, lo recibe por correo, y se autentica obteniendo un JWT.

**Independent Test**: Crear usuario HABILITADO → `POST /api/v1/auth/otp/request` → recibe correo → `POST /api/v1/auth/otp/verify` →
obtiene JWT. Login con usuario ACTIVO → 403. OTP incorrecto → 401. OTP expirado → 401. 3 intentos fallidos → OTP
invalidado.

### Tests for User Story 2

- [ ] T030 [P] [US2] Unit test `OtpTest`: construir OTP con Builder → `validateInfo()` exitoso. `incrementarIntento()` 3
  veces → estado INVALIDADO. `esValido()` con fecha expirada → false.
- [ ] T031 [P] [US2] Unit test `RequestOtpUseCaseTest`: email existe → genera OTP, envía correo, retorna confirmación.
  Email no existe → retorna confirmación genérica (sin revelar). Usuario INACTIVO → retorna confirmación genérica.
- [ ] T032 [P] [US2] Unit test `VerifyOtpUseCaseTest`: OTP correcto + usuario HABILITADO → retorna JWT. Usuario ACTIVO →
  `UsuarioNoHabilitadoException`. OTP expirado → `OtpExpiradoException`. OTP incorrecto → `OtpInvalidoException`. 3er
  intento fallido → `ReintentosExcedidosException`.
- [ ] T033 [P] [US2] Integration test `OtpFlowIntegrationTest` (Testcontainers): flujo completo request → verify con
  Mailpit mock. Verificar que el JWT contiene rol VOTANTE.

### Implementation for User Story 2

- [ ] T034 [P] [US2] Crear `OtpRequest` DTO: `email` con `@NotBlank @Email`.
- [ ] T035 [P] [US2] Crear `OtpVerifyRequest` DTO: `email`, `codigo` ambos `@NotBlank`.
- [ ] T036 [P] [US2] Crear `AuthResponse` DTO: `token`, `email`, `rol`.
- [ ] T037 [US2] Crear `RequestOtpUseCase` en `application/auth/`:
    - Inyecta `UsuarioRepository`, `OtpRepository`, `EmailSender`.
    - Método `solicitarOtp(String email)`: busca usuario por email. Si no existe o está INACTIVO → retorna confirmación
      genérica (sin revelar nada). Si existe: genera código aleatorio 6 dígitos (`ThreadLocalRandom`), calcula
      expiración (`Instant.now().plus(5, MINUTOS)`), invalida OTP activo previo, persiste nuevo OTP, envía correo vía
      `EmailSender`. Siempre retorna `Mono.just("Si el email está registrado, recibirás un código.")`.
- [ ] T038 [US2] Crear `VerifyOtpUseCase` en `application/auth/`:
    - Inyecta `UsuarioRepository`, `OtpRepository`, `JwtProvider`.
    - Método `verificarOtp(String email, String codigo)`:
        1. Busca OTP activo para email. Si no existe → `OtpInvalidoException`.
        2. Verifica `otp.esValido()`. Si expirado → `OtpExpiradoException`.
        3. Compara código. Si no coincide → `otp.incrementarIntento()` + persistir. Si intentos == 3 →
           `otp.invalidar()` + persistir + `ReintentosExcedidosException`. Si coincide:
            - Busca usuario. Si estado != HABILITADO → `UsuarioNoHabilitadoException`.
            - `otp.marcarUsado()` + persistir.
            - Genera JWT con `email`, `rol`, `exp` (15 min).
            - Retorna `AuthResponse`.
- [ ] T039 [US2] Agregar endpoints a `AuthController`:
    - `POST /api/v1/auth/otp/request`: recibe `OtpRequest`, invoca `RequestOtpUseCase.solicitarOtp()`, retorna 200 con
      mensaje genérico.
    - `POST /api/v1/auth/otp/verify`: recibe `OtpVerifyRequest`, invoca `VerifyOtpUseCase.verificarOtp()`, retorna 200
      con `AuthResponse`.
- [ ] T040 [US2] Crear `OtpExpirationScheduler` en `infrastructure/adapter/in/scheduler/`:
    - `@Scheduled(fixedRate = 60000)` (cada 1 min).
    - Barre OTPs ACTIVOS con expiración pasada y los marca INVALIDADO en batch.
    - Log de cuántos OTPs fueron invalidados.

**Checkpoint**: Login OTP de votante 100% funcional. Flujo request → verify con JWT, expiración, reintentos, y estado
HABILITADO exigido.

---

## Phase 5: User Story 3 — Login de Gestor Electoral con OTP (Priority: P2)

**Goal**: Un gestor electoral HABILITADO se autentica con el mismo flujo OTP que el votante, obteniendo un JWT con rol
`GESTOR_ELECTORAL`. Los gestores no pueden registrarse por el endpoint público.

**Independent Test**: Crear gestor manualmente en DB → `POST /api/v1/auth/otp/request` → verify → JWT contiene
`rol=GESTOR_ELECTORAL`. Intentar `POST /api/v1/auth/register` con datos de gestor → endpoint solo asigna VOTANTE (ya cubierto
en US1).

### Tests for User Story 3

- [ ] T041 [P] [US3] Unit test `VerifyOtpUseCaseTest`: gestor HABILITADO → retorna JWT con rol GESTOR_ELECTORAL. Gestor
  ACTIVO → `UsuarioNoHabilitadoException`.
- [ ] T042 [P] [US3] Integration test: gestor completa flujo OTP exitosamente. JWT permite acceder a endpoints admin.

### Implementation for User Story 3

- [ ] T043 [US3] Verificar que `VerifyOtpUseCase` no requiere cambios: el JWT incluye el `rol` del usuario, sea VOTANTE
  o GESTOR_ELECTORAL.
- [ ] T044 [US3] Verificar que `SecurityConfig` no requiere cambios: la autorización por rol se implementará en specs
  posteriores. Por ahora, cualquier JWT válido accede.
- [ ] T045 [US3] Agregar test de integración que verifique que el endpoint de registro (`POST /api/v1/auth/register`)
  rechaza intentos de crear gestores (el endpoint fuerza `rol = VOTANTE`).

**Checkpoint**: Ambos roles (VOTANTE y GESTOR_ELECTORAL) pueden autenticarse vía OTP y obtener JWT con el rol correcto.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — ejecutar inmediatamente.
- **Foundational (Phase 2)**: Depende de Phase 1 — BLOQUEA todos los user stories.
- **US1 — Registro (Phase 3)**: Depende de Phase 2. Independiente de US2 y US3.
- **US2 — Login OTP Votante (Phase 4)**: Depende de Phase 2. Puede iniciarse en paralelo con US1.
- **US3 — Login OTP Gestor (Phase 5)**: Depende de Phase 4 (reutiliza VerifyOtpUseCase). Sin cambios estructurales.

### User Story Dependencies

- **US1 (P1)**: Solo depende de Foundational.
- **US2 (P1)**: Solo depende de Foundational. Puede implementarse en paralelo con US1.
- **US3 (P2)**: Depende de US2 completado (reutiliza los mismos casos de uso).

### Within Each User Story

- Entidades de dominio antes que casos de uso.
- Casos de uso antes que controladores.
- Tests unitarios de dominio primero, luego tests de caso de uso, luego tests de integración.
- DTOs y mappers se crean junto con el controlador.

---

## Notes

- Las entidades `Usuario` y `Otp` usan `@Builder` de Lombok. `validateInfo()` se invoca manualmente después de construir
  o mutar. No usar `@Builder.Default` con lógica de validación.
- `EmailSender.enviarOtp()` es un puerto de dominio. Su implementación en infraestructura envuelve `JavaMailSender` en
  `Mono.fromRunnable` para mantener el flujo reactivo.
- `OtpExpirationScheduler` usa scheduling reactivo; considerar `Flux.interval` en lugar de `@Scheduled` si se quiere
  consistencia con WebFlux. Tarea T040 puede ajustarse en implementación.
- La migración Flyway V1 incluye seed data de departamentos y municipios. Si el volumen es grande, considerar un archivo
  CSV o data SQL aparte.
- Los JWT no incluyen refresh tokens en esta fase. Se pueden agregar en un spec futuro si se requiere.
- Todos los nombres de clases, métodos y variables están en español. Las excepciones técnicas de Spring/Reactor se
  capturan en el `GlobalExceptionHandler` y se traducen a mensajes de error en español.
