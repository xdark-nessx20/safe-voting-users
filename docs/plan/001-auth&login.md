# Implementation Plan: Autenticación y Registro

**Date**: 2026-06-13  
**Spec**: [001-auth&login.md](../spec/001-auth&login.md)
**Last updated**: 2026-06-16

## Summary

Implementar el registro de votantes y la autenticación mediante OTP enviado por correo electrónico para votantes y
gestores electorales. Solo usuarios en estado `HABILITADO` pueden autenticarse exitosamente. El OTP expira a los 5
minutos con máximo 3 intentos fallidos.

**Technical approach**: Arquitectura hexagonal con Spring Boot WebFlux, Spring Data R2DBC + DatabaseClient, PostgreSQL,
JWT para sesiones, y entidades de dominio con validación mediante patrón Builder + `validateInfo()`.
Las excepciones extienden `RuntimeException` directamente. La capa de aplicación no tiene anotaciones Spring
(beans declarados en `BeanConfiguration`). La persistencia separa Entity, Mapper, ReactiveRepository y Adapter.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3.x (WebFlux), Spring Data R2DBC, Flyway, jjwt, Lombok, MapStruct,
springdoc-openapi (Swagger), Spring Boot Actuator, spring-boot-starter-mail, Bean Validation (jakarta.validation)  
**Storage**: PostgreSQL 16 (via R2DBC + DatabaseClient)  
**Testing**: JUnit 5, Mockito, WebTestClient  
**Target Platform**: Linux server (Docker)  
**Project Type**: Microservice REST reactivo  
**Performance Goals**: <500ms p95 en endpoints de auth, soportar 500 solicitudes OTP simultáneas  
**Constraints**: Email y documento únicos, OTP 6 caracteres alfanuméricos mayúsculas, expira 5 min, 3 intentos máx.  
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
│   ├── shared/                              # Value objects con validación propia
│   │   ├── Email.java                       # Patrón regex: ^[^@]+@[^@]+\.[^@]+$
│   │   ├── DocumentoIdentidad.java          # Patrón regex: \d{5,15}
│   │   └── Phone.java                       # Patrón regex: \d{7,15} (nullable)
│   ├── model/
│   │   ├── usuario/
│   │   │   ├── Usuario.java                 # Builder + AllArgs/NoArgs + esActivo/Habilitado/Inactivo
│   │   │   ├── EstadoUsuario.java           # ACTIVO, HABILITADO, INACTIVO
│   │   │   └── Rol.java                     # VOTANTE, GESTOR_ELECTORAL
│   │   ├── otp/
│   │   │   ├── Otp.java                     # Patrón [A-Z0-9]{6}, esActivo/Usado/Invalidado
│   │   │   └── EstadoOtp.java               # ACTIVO, USADO, INVALIDADO
│   │   └── geografia/
│   │       ├── Municipio.java
│   │       └── Departamento.java
│   ├── exception/
│   │   ├── common/
│   │   │   └── DatosInvalidosException.java
│   │   ├── usuario/
│   │   │   ├── EmailDuplicadoException.java
│   │   │   ├── DocumentoDuplicadoException.java
│   │   │   ├── EmailNoRegistradoException.java
│   │   │   ├── UsuarioNoHabilitadoException.java
│   │   │   ├── UsuarioInactivoException.java
│   │   │   └── RolInvalidoException.java
│   │   ├── otp/
│   │   │   ├── OtpExpiradoException.java
│   │   │   ├── OtpInvalidoException.java
│   │   │   ├── ReintentosExcedidosException.java
│   │   │   └── TransicionEstadoOtpInvalidaException.java
│   │   └── geografia/
│   │       └── MunicipioNoEncontradoException.java
│   └── repository/                           # Puertos (interfaces sin anotaciones Spring)
│       ├── UsuarioRepository.java
│       ├── OtpRepository.java
│       ├── MunicipioRepository.java
│       ├── DepartamentoRepository.java
│       ├── EmailSender.java
│       └── TokenService.java                 # Puerto para generación de tokens
│
├── application/
│   └── auth/
│       ├── AuthResult.java                   # DTO de aplicación (no infrastructure)
│       ├── RegisterVotanteUseCase.java       # Sin @Service; bean en BeanConfiguration
│       ├── RequestOtpUseCase.java
│       └── VerifyOtpUseCase.java             # Usa TokenService (puerto), no JwtProvider
│
└── infrastructure/
    ├── config/
    │   ├── BeanConfiguration.java            # Declara use cases, EmailSenderAdapter como @Bean
    │   ├── AppConfig.java
    │   ├── CorsConfig.java
    │   ├── SecurityConfig.java
    │   ├── JwtProvider.java                  # implements TokenService + validarToken/extraerEmail
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
        │   │   │   │   ├── AuthResponse.java
        │   │   │   │   └── MessageResponse.java   # Respuesta genérica { mensaje }
        │   │   │   ├── mapper/
        │   │   │   │   └── AuthDtoMapper.java      # MapStruct + toUsuarioParaRegistro
        │   │   │   └── AuthController.java
        │   │   └── common/
        │   │       ├── ApiErrorResponse.java
        │   │       └── GlobalExceptionHandler.java
        │   └── scheduler/
        │       └── OtpExpirationScheduler.java
        └── out/
            ├── persistence/
            │   ├── usuario/
            │   │   ├── UsuarioEntity.java              # @Table("usuario"), campos planos
            │   │   ├── UsuarioPersistenceMapper.java    # MapStruct: Entity ↔ Domain
            │   │   ├── UsuarioReactiveRepository.java  # extends ReactiveCrudRepository
            │   │   └── UsuarioRepositoryAdapter.java   # implements UsuarioRepository
            │   ├── otp/
            │   │   ├── OtpEntity.java
            │   │   ├── OtpPersistenceMapper.java       # MapStruct
            │   │   ├── OtpReactiveRepository.java
            │   │   └── OtpRepositoryAdapter.java
            │   └── geografia/
            │       ├── DepartamentoEntity.java
            │       ├── DepartamentoPersistenceMapper.java  # MapStruct
            │       ├── DepartamentoReactiveRepository.java
            │       ├── DepartamentoRepositoryAdapter.java
            │       ├── MunicipioEntity.java
            │       ├── MunicipioPersistenceMapper.java     # MapStruct
            │       ├── MunicipioReactiveRepository.java
            │       └── MunicipioRepositoryAdapter.java
            └── email/
                └── EmailSenderAdapter.java

src/main/resources/
├── application.yml
├── application-dev.yml
└── db/migration/
    ├── V1__crear_tablas_iniciales.sql        # DDL: CREATE TABLE + índices
    └── V2__seed_data_municipios_departamentos.sql  # Seed sin UUIDs hardcodeados, CTE

src/test/java/com/safevoting/users/
├── unit/
│   ├── domain/
│   │   ├── shared/
│   │   │   ├── EmailTest.java
│   │   │   └── DocumentoIdentidadTest.java
│   │   └── model/
│   │       ├── usuario/
│   │       │   └── UsuarioTest.java
│   │       └── otp/
│   │           └── OtpTest.java
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

**Structure Decision**: Arquitectura hexagonal estricta. `domain/` sin anotaciones de framework. `application/` sin
`@Service` — beans declarados en `BeanConfiguration`. Persistencia con 4 capas por entidad: Entity (tabla DB),
Mapper (Row/Domain), ReactiveRepository (Spring Data R2DBC), Adapter (implementa puerto del dominio).
Las excepciones extienden `RuntimeException` directamente (sin base común). Rutas REST: registro en `/register`,
login en `/login/request-otp` y `/login/verify-otp`.

## Convenciones de código limpio

- **SOLID**: una responsabilidad por clase. `domain/` define contratos, `application/` orquesta, `infrastructure/`
  adapta.
- **Arquitectura hexagonal**: la capa de aplicación no importa nada de `infrastructure/`. Los puertos
  (`UsuarioRepository`, `TokenService`, `EmailSender`) viven en `domain/repository/`.
- **Sin `@Service` en application**: los use cases son POJOs con `@RequiredArgsConstructor` (Lombok).
  `BeanConfiguration` los declara como `@Bean`.
- **Persistencia en 4 capas**: Entity (tabla) → ReactiveRepository (Spring Data) → Mapper (conversión) → Adapter
  (implementa puerto dominio). El Adapter usa `DatabaseClient` para queries con JOINs que el ReactiveRepository
  no puede resolver.
- **Nombres en español** alineados al dominio electoral colombiano.
- **Métodos cortos** (~15 líneas máx.), un solo nivel de abstracción por método.
- **Sin magic numbers**: constantes en enums o en la propia clase de dominio.
- **Sin comentarios redundantes**: el código se explica solo.
- **Tests Given/When/Then**: nombres de método descriptivos en español.
- **Entidades de dominio**: Lombok `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor`. Validación con
  `validateInfo()` llamada explícitamente. Sin builders estáticos custom.
  Métodos boolean de estado (`esActivo()`, `esHabilitado()`, etc.). Transiciones de estado con guardas.
- **Excepciones**: solo excepciones de dominio en `domain/exception/`. Extienden `RuntimeException` directamente,
  cada una con su `errorCode`. Sin `DomainException` base.
- **Value objects** con `@Builder` sobre el constructor (no sobre la clase), validación en el constructor.
- **Reactivo sin if en flatMap**: usar `filter` + `switchIfEmpty` + `Mono.defer` para branching reactivo.

## REST API Endpoints

| Método | Ruta | Request | Response |
|--------|------|---------|----------|
| POST | `/api/v1/auth/register` | `RegisterRequest` | `MessageResponse` (201) |
| POST | `/api/v1/auth/login/request-otp` | `OtpRequest` | `MessageResponse` (200) |
| POST | `/api/v1/auth/login/verify-otp` | `OtpVerifyRequest` | `AuthResponse` (200) |

