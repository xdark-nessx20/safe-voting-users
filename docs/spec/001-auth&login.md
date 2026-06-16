# Feature Specification: Autenticación y Registro

**Created**: 2026-06-13  
**Stakeholders**: Votante, Gestor Electoral

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registro de Votante (Priority: P1)

Un ciudadano se registra en el sistema proporcionando sus datos de identidad para obtener una cuenta que le permita
posteriormente participar en procesos electorales.

**Why this priority**: Sin registro no existe base de votantes. Es el punto de entrada obligatorio para cualquier otra
funcionalidad del sistema.

**Independent Test**: Puede probarse completamente invocando el endpoint de registro con datos válidos y verificando que
el usuario se crea en estado `ACTIVO` con los atributos correctos.

**Acceptance Scenarios**:

1. **Registro exitoso**
    - **Given** el email y número de documento no existen en el sistema
    - **When** el votante envía nombre completo, email, teléfono, número de documento y municipio de inscripción válidos
    - **Then** el sistema crea el usuario con estado `ACTIVO` y rol `VOTANTE`, y retorna confirmación

2. **Email duplicado**
    - **Given** ya existe un usuario registrado con el email `juan@example.com`
    - **When** otro ciudadano intenta registrarse con el mismo email
    - **Then** el sistema rechaza el registro con error 409 indicando que el email ya está en uso

3. **Documento duplicado**
    - **Given** ya existe un usuario registrado con el documento `123456789`
    - **When** otro ciudadano intenta registrarse con el mismo número de documento
    - **Then** el sistema rechaza el registro con error 409 indicando que el documento ya está registrado

4. **Municipio inexistente**
    - **Given** el municipio proporcionado no existe en el sistema
    - **When** el votante envía la solicitud de registro
    - **Then** el sistema rechaza el registro con error 404 indicando que el municipio no es válido

5. **Campos obligatorios faltantes**
    - **Given** el votante omite uno o más campos obligatorios (nombre, email, documento, municipio)
    - **When** envía la solicitud de registro
    - **Then** el sistema rechaza el registro con error 422 detallando los campos faltantes

---

### User Story 2 - Login de Votante con OTP (Priority: P1)

Un votante en estado `HABILITADO` se autentica mediante un código OTP de un solo uso enviado a su correo electrónico.

**Why this priority**: La autenticación es el mecanismo que habilita la participación del votante en una jornada
electoral. Sin login funcional, el registro no tiene valor.

**Independent Test**: Puede probarse creando un usuario `HABILITADO`, solicitando el OTP, verificando la recepción
simulada del código, y validando que el login retorna un token de sesión.

**Acceptance Scenarios**:

1. **Solicitud de OTP exitosa**
    - **Given** existe un usuario registrado con email `juan@example.com`
    - **When** el votante solicita un OTP para ese email
    - **Then** el sistema genera un código de 6 dígitos con expiración de 5 minutos, lo envía al correo, y retorna
      confirmación de envío

2. **Login exitoso con OTP válido**
    - **Given** el usuario está en estado `HABILITADO` y tiene un OTP activo no expirado
    - **When** el votante envía el email y el código OTP correcto
    - **Then** el sistema autentica al usuario, invalida el OTP usado, y retorna un token de sesión

3. **Login rechazado por estado no HABILITADO**
    - **Given** el usuario está en estado `ACTIVO` y tiene un OTP activo
    - **When** el votante envía el email y el código OTP correcto
    - **Then** el sistema rechaza el login con error 403 indicando que el usuario no está habilitado para votar

4. **Login con usuario INACTIVO**
    - **Given** el usuario está en estado `INACTIVO`
    - **When** el votante intenta solicitar OTP o hacer login
    - **Then** el sistema rechaza la operación con error 403 indicando que la cuenta está suspendida

5. **OTP expirado**
    - **Given** el OTP fue generado hace más de 5 minutos
    - **When** el votante intenta usarlo para autenticarse
    - **Then** el sistema rechaza el login con error 401 indicando que el código ha expirado

6. **OTP incorrecto**
    - **Given** existe un OTP activo `123456`
    - **When** el votante envía un código incorrecto
    - **Then** el sistema incrementa el contador de intentos fallidos y retorna error 401

7. **Reintentos excedidos**
    - **Given** el OTP ha acumulado 3 intentos fallidos
    - **When** el votante intenta usar cualquier código para ese OTP
    - **Then** el sistema invalida el OTP y retorna error 401. El usuario debe solicitar un nuevo OTP.

8. **Email no registrado al solicitar OTP**
    - **Given** el email `noexiste@example.com` no pertenece a ningún usuario
    - **When** se solicita un OTP para ese email
    - **Then** el sistema retorna confirmación genérica de envío (sin revelar si el email existe o no, por seguridad)

---

### User Story 3 - Login de Gestor Electoral con OTP (Priority: P2)

Un gestor electoral se autentica mediante el mismo mecanismo de OTP por correo. Los gestores son creados exclusivamente
por un usuario administrador del sistema y no pueden auto-registrarse.

**Why this priority**: El gestor electoral es necesario para administrar usuarios, pero su funcionalidad puede
implementarse después de que el flujo base de OTP esté probado con votantes.

**Independent Test**: Puede probarse creando un gestor directamente en base de datos, solicitando OTP con su email, y
verificando que el login retorna token de sesión con rol `GESTOR_ELECTORAL`.

**Acceptance Scenarios**:

1. **Login exitoso de gestor**
    - **Given** existe un gestor electoral en estado `HABILITADO`
    - **When** solicita OTP e ingresa el código correcto
    - **Then** el sistema autentica al gestor y retorna un token de sesión con rol `GESTOR_ELECTORAL`

2. **Registro de gestor rechazado**
    - **Given** cualquier persona intenta usar el endpoint de registro público
    - **When** envía datos con intención de crear una cuenta de gestor
    - **Then** el sistema rechaza la solicitud. El endpoint de registro público solo crea usuarios con rol `VOTANTE`

---

### Edge Cases

- ¿Qué ocurre si un usuario solicita un nuevo OTP cuando ya tiene uno activo?  
  → El OTP anterior se invalida y se genera
  uno nuevo.
- ¿Qué ocurre si el servicio de envío de correo falla al generar el OTP?  
  → El sistema retorna error 502 indicando fallo
  en el servicio de notificaciones y no persiste el OTP.
- ¿Qué ocurre si un usuario intenta registrar un email con formato inválido?  
  → El sistema rechaza con error 422.
- ¿Qué ocurre si un usuario intenta registrar un número de documento con formato no numérico?  
  → El sistema rechaza con error 422.
- ¿Qué ocurre si dos solicitudes simultáneas de OTP llegan para el mismo email?  
  → Se genera un único OTP; la segunda
  solicitud invalida la primera y crea una nueva.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir el registro de votantes con nombre completo, email, teléfono, número de documento
  de identidad y municipio de inscripción.
- **FR-002**: El sistema DEBE validar que el email y el número de documento sean únicos en el sistema.
- **FR-003**: El sistema DEBE crear todo nuevo votante con estado `ACTIVO` y rol `VOTANTE`.
- **FR-004**: El sistema DEBE generar un código OTP numérico de 6 dígitos por cada solicitud de autenticación.
- **FR-005**: El sistema DEBE enviar el código OTP al correo electrónico del usuario.
- **FR-006**: El sistema DEBE establecer una expiración de 5 minutos para cada OTP generado.
- **FR-007**: El sistema DEBE invalidar el OTP tras 3 intentos fallidos consecutivos.
- **FR-008**: El sistema DEBE rechazar el login de cualquier usuario que no esté en estado `HABILITADO`.
- **FR-009**: El sistema DEBE invalidar el OTP inmediatamente después de un login exitoso.
- **FR-010**: El sistema NO DEBE revelar si un email existe o no al solicitar un OTP (respuesta genérica por seguridad).
- **FR-011**: El sistema DEBE restringir el endpoint de registro público para que solo cree usuarios con rol `VOTANTE`.
  Los gestores electorales solo pueden ser creados por un administrador.
- **FR-012**: El sistema DEBE validar que el municipio de inscripción exista en la jerarquía geográfica antes de crear
  el usuario.

### Key Entities

1. **Usuario**: Identidad del ciudadano en el sistema.
    - Atributos: nombre completo, email (único), teléfono, número de
      documento de identidad (único), municipio de inscripción (FK a Municipio), rol (`VOTANTE` | `GESTOR_ELECTORAL`),
      estado (`ACTIVO` | `HABILITADO` | `INACTIVO`), fecha de creación.
2. **OTP**: Código de un solo uso para autenticación.
    - Atributos: email destinatario, código (6 caracteres), fecha de
      expiración, contador de intentos fallidos, estado (`activo` | `usado` | `invalidado`).

3. **Municipio**: División geográfica municipal.
    - Atributos: identificador, nombre, departamento al que pertenece (FK a Departamento).
4. **Departamento**: División geográfica departamental.
    - Atributos: identificador, nombre.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un votante completa el registro en menos de 30 segundos desde que inicia el formulario.
- **SC-002**: El OTP llega al correo del usuario en menos de 10 segundos tras la solicitud en el 95% de los casos.
- **SC-003**: Un votante completa el flujo de login (solicitud OTP + ingreso de código) en menos de 60 segundos.
- **SC-004**: El sistema rechaza el 100% de los intentos de login con estado distinto a `HABILITADO`.
- **SC-005**: El sistema soporta 500 solicitudes de OTP simultáneas sin degradación del tiempo de respuesta.
