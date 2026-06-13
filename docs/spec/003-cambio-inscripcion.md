# Feature Specification: Cambio de Lugar de Inscripción

**Created**: 2026-06-13  
**Stakeholders**: Votante, Gestor Electoral

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Solicitar cambio de inscripción (Priority: P1)

Un votante autenticado solicita cambiar su lugar de inscripción a un nuevo municipio, proporcionando el motivo del
cambio. La solicitud queda en estado `PENDIENTE` hasta que un gestor electoral la procese.

**Why this priority**: Es la acción que origina todo el flujo de cambio de inscripción. Sin ella, los gestores no tienen
solicitudes que procesar.

**Independent Test**: Puede probarse autenticando un votante, enviando una solicitud con municipio destino y motivo
válidos, y verificando que la solicitud se crea en estado `PENDIENTE`.

**Acceptance Scenarios**:

1. **Solicitud creada exitosamente**
    - **Given** un votante autenticado con municipio de inscripción `Medellín`
    - **When** envía una solicitud con municipio destino `Bogotá` y motivo "Cambio de residencia laboral"
    - **Then** el sistema crea la solicitud en estado `PENDIENTE` y retorna confirmación con el ID de la solicitud

2. **Solicitud duplicada rechazada**
    - **Given** el votante ya tiene una solicitud en estado `PENDIENTE`
    - **When** intenta crear una nueva solicitud de cambio
    - **Then** el sistema rechaza con error 409 indicando que ya existe una solicitud pendiente

3. **Municipio destino igual al actual**
    - **Given** el votante tiene municipio de inscripción `Medellín`
    - **When** envía una solicitud con municipio destino `Medellín`
    - **Then** el sistema rechaza automáticamente con error 422 indicando que el municipio destino debe ser diferente al
      actual

4. **Municipio destino inexistente**
    - **Given** el votante envía un `municipio_id` que no existe en la jerarquía geográfica
    - **When** se procesa la solicitud
    - **Then** el sistema retorna error 404 indicando que el municipio no existe

5. **Votante no autenticado**
    - **Given** no hay un token de sesión válido
    - **When** se intenta crear una solicitud de cambio
    - **Then** el sistema retorna error 401

6. **Motivo vacío**
    - **Given** el votante envía la solicitud con el campo motivo vacío
    - **When** se procesa la solicitud
    - **Then** el sistema rechaza con error 422 indicando que el motivo es obligatorio

---

### User Story 2 - Listar solicitudes pendientes (Priority: P1)

Un gestor electoral consulta las solicitudes de cambio de inscripción en estado `PENDIENTE` cuyo municipio destino está
dentro de su `alcance_operacion`. La lista está paginada.

**Why this priority**: El gestor necesita ver qué solicitudes tiene pendientes para poder procesarlas. Es el paso previo
obligatorio a aceptar o rechazar.

**Independent Test**: Autenticar un gestor, crear solicitudes pendientes en su alcance, y verificar que el listado las
retorna correctamente paginadas.

**Acceptance Scenarios**:

1. **Listado de solicitudes en alcance**
    - **Given** un gestor con alcance `DEPARTAMENTAL` en `Cundinamarca`, y existen 3 solicitudes `PENDIENTES` con
      municipio destino en `Cundinamarca`
    - **When** el gestor solicita el listado de solicitudes pendientes
    - **Then** el sistema retorna las 3 solicitudes con datos del solicitante, municipio origen, municipio destino,
      motivo y fecha

2. **Listado filtrado por alcance**
    - **Given** un gestor con alcance `MUNICIPAL` en `Medellín`
    - **When** solicita el listado de pendientes
    - **Then** el sistema solo retorna solicitudes cuyo municipio destino es `Medellín`

3. **Sin solicitudes pendientes**
    - **Given** no existen solicitudes `PENDIENTES` en el alcance del gestor
    - **When** consulta el listado
    - **Then** el sistema retorna lista vacía con total cero

4. **Paginación**
    - **Given** existen 50 solicitudes pendientes en el alcance del gestor
    - **When** solicita la segunda página con tamaño 20
    - **Then** el sistema retorna los registros 21-40 con metadatos de paginación correctos

---

### User Story 3 - Aceptar solicitud de cambio (Priority: P1)

Un gestor electoral aprueba una solicitud de cambio de inscripción. El sistema actualiza el municipio de inscripción del
votante al municipio destino y marca la solicitud como `ACEPTADA`.

**Why this priority**: Es la acción resolutiva principal del flujo. Sin ella, las solicitudes quedarían pendientes
indefinidamente.

**Independent Test**: Crear una solicitud `PENDIENTE` en el alcance de un gestor, ejecutar la aceptación, y verificar
que el municipio del usuario cambia y la solicitud queda `ACEPTADA`.

**Acceptance Scenarios**:

1. **Aceptación exitosa**
    - **Given** una solicitud `PENDIENTE` de un usuario de `Medellín` hacia `Bogotá`, y un gestor con alcance que cubre
      `Bogotá`
    - **When** el gestor acepta la solicitud
    - **Then** el municipio de inscripción del usuario cambia a `Bogotá`, la solicitud pasa a `ACEPTADA`, se registra el
      `gestor_id` y la `fecha_resolucion`

2. **Solicitud ya procesada**
    - **Given** una solicitud en estado `ACEPTADA` o `RECHAZADA`
    - **When** el gestor intenta aceptarla
    - **Then** el sistema rechaza con error 409 indicando que la solicitud ya fue procesada

3. **Solicitud fuera de alcance**
    - **Given** un gestor con alcance `MUNICIPAL` en `Medellín`, y una solicitud `PENDIENTE` con municipio destino
      `Bogotá`
    - **When** el gestor intenta aceptarla
    - **Then** el sistema rechaza con error 403

4. **Solicitud inexistente**
    - **Given** un `solicitud_id` que no existe
    - **When** el gestor intenta aceptarla
    - **Then** el sistema retorna error 404

5. **Usuario en estado INACTIVO**
    - **Given** el usuario asociado a la solicitud fue cambiado a `INACTIVO` (lo que cancela automáticamente la
      solicitud)
    - **When** el gestor intenta aceptar una solicitud que fue cancelada
    - **Then** el sistema rechaza con error 409 indicando que la solicitud ya no está pendiente

---

### User Story 4 - Rechazar solicitud de cambio (Priority: P1)

Un gestor electoral rechaza una solicitud de cambio de inscripción, registrando el motivo del rechazo. El municipio de
inscripción del votante no se modifica.

**Why this priority**: No todas las solicitudes deben ser aprobadas. El rechazo con motivo permite mantener la
trazabilidad y transparencia del proceso.

**Independent Test**: Crear una solicitud `PENDIENTE` en el alcance de un gestor, ejecutar el rechazo con motivo, y
verificar que la solicitud queda `RECHAZADA` y el municipio del usuario no cambia.

**Acceptance Scenarios**:

1. **Rechazo exitoso**
    - **Given** una solicitud `PENDIENTE` en el alcance del gestor
    - **When** el gestor la rechaza con motivo "Documentación insuficiente"
    - **Then** la solicitud pasa a `RECHAZADA`, se registra `motivo_rechazo`, `gestor_id` y `fecha_resolucion`. El
      municipio del usuario no cambia.

2. **Rechazo sin motivo**
    - **Given** una solicitud `PENDIENTE`
    - **When** el gestor intenta rechazarla con el campo motivo vacío
    - **Then** el sistema rechaza con error 422 indicando que el motivo de rechazo es obligatorio

3. **Solicitud ya procesada**
    - **Given** una solicitud en estado `ACEPTADA`
    - **When** el gestor intenta rechazarla
    - **Then** el sistema rechaza con error 409 indicando que la solicitud ya fue procesada

4. **Solicitud fuera de alcance**
    - **Given** un gestor intenta rechazar una solicitud cuyo municipio destino no está en su alcance
    - **When** ejecuta la operación
    - **Then** el sistema rechaza con error 403

---

### Edge Cases

- ¿Qué ocurre si un usuario es cambiado a `INACTIVO` mientras tiene una solicitud `PENDIENTE`?  
  → El sistema cancela automáticamente la solicitud (estado `CANCELADA` con `motivo_rechazo` automático: "Usuario
  suspendido").
- ¿Qué ocurre si el municipio destino es eliminado de la jerarquía geográfica mientras hay solicitudes `PENDIENTES`
  hacia él?  
  → Las solicitudes asociadas se cancelan automáticamente.
- ¿Qué ocurre si un gestor intenta procesar una solicitud donde el votante ya cambió su municipio por otra solicitud
  aceptada previamente?  
  → Como el `municipio_origen_id` ya no coincide con el municipio actual del usuario, el sistema rechaza con error 409.
- ¿Qué ocurre si dos gestores intentan procesar la misma solicitud simultáneamente?  
  → El sistema debe garantizar que solo uno tenga éxito (control de concurrencia). El segundo recibe error 409.
- ¿Qué ocurre si un usuario con solicitud `ACEPTADA` o `RECHAZADA` quiere crear una nueva solicitud?  
  → Puede hacerlo, ya que solo se restringe tener una solicitud `PENDIENTE` a la vez.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al votante autenticado crear una solicitud de cambio de inscripción con municipio
  destino y motivo.
- **FR-002**: El sistema DEBE impedir que un votante tenga más de una solicitud en estado `PENDIENTE` simultáneamente.
- **FR-003**: El sistema DEBE rechazar automáticamente una solicitud si el municipio destino es igual al municipio
  actual del votante.
- **FR-004**: El sistema DEBE validar que el municipio destino exista en la jerarquía geográfica.
- **FR-005**: El sistema DEBE permitir al gestor electoral listar solicitudes `PENDIENTES` paginadas, filtradas por su
  `alcance_operacion` aplicado sobre el municipio destino de la solicitud.
- **FR-006**: El sistema DEBE permitir al gestor electoral aceptar una solicitud `PENDIENTE`, actualizando el municipio
  de inscripción del votante al municipio destino.
- **FR-007**: El sistema DEBE permitir al gestor electoral rechazar una solicitud `PENDIENTE`, registrando un motivo de
  rechazo obligatorio.
- **FR-008**: El sistema DEBE validar que el municipio destino de la solicitud esté dentro del `alcance_operacion` del
  gestor para cualquier operación de procesamiento.
- **FR-009**: El sistema NO DEBE permitir procesar una solicitud que no esté en estado `PENDIENTE` (idempotencia).
- **FR-010**: El sistema DEBE cancelar automáticamente las solicitudes `PENDIENTES` de un usuario cuando este cambia a
  estado `INACTIVO`.
- **FR-011**: El sistema DEBE cancelar automáticamente las solicitudes `PENDIENTES` asociadas a un municipio destino que
  es eliminado de la jerarquía geográfica.
- **FR-012**: El sistema DEBE registrar `gestor_id` y `fecha_resolucion` al aceptar o rechazar una solicitud.
- **FR-013**: El sistema DEBE garantizar control de concurrencia para evitar que una misma solicitud sea procesada por
  dos gestores simultáneamente.

### Key Entities

1. **SolicitudCambioInscripcion**: Registro de una petición de cambio de lugar de inscripción.
    - Atributos: identificador, usuario solicitante (FK a Usuario), municipio origen (FK a Municipio, capturado al
      momento de la solicitud), municipio destino (FK a Municipio), motivo del cambio,
      estado (`PENDIENTE` | `ACEPTADA` | `RECHAZADA` | `CANCELADA`), motivo de rechazo (requerido en RECHAZADA y
      CANCELADA),
      gestor que procesó (FK a Usuario con rol `GESTOR_ELECTORAL`, nullable hasta resolución), fecha de solicitud,
      fecha de resolución (nullable hasta resolución).
2. **Usuario**: Identidad del ciudadano (definido en spec 001-auth&login.md). Incluye `municipio_id` como lugar de
   inscripción actual.
3. **Gestor Electoral**: Usuario con rol `GESTOR_ELECTORAL` y atributo `alcance_operacion` (definido en spec
   002-gestion-usuarios.md).
4. **Municipio**: División geográfica municipal (definido en spec 001).
5. **Departamento**: División geográfica departamental (definido en spec 001).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un votante completa la creación de una solicitud en menos de 2 segundos.
- **SC-002**: El listado de solicitudes pendientes soporta páginas de hasta 50 registros sin degradación.
- **SC-003**: La aceptación o rechazo de una solicitud se completa en menos de 1 segundo.
- **SC-004**: El sistema garantiza que el 100% de las solicitudes procesadas concurrentemente mantienen integridad (sin
  doble procesamiento).
- **SC-005**: La cancelación automática de solicitudes por cambio de estado del usuario se ejecuta en la misma
  transacción que el cambio de estado.
