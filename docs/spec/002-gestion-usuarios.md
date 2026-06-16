# Feature Specification: Gestión de Usuarios

**Created**: 2026-06-13  
**Stakeholder**: Gestor Electoral

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Modificar estado de un usuario individual (Priority: P1)

Un gestor electoral autenticado cambia el estado de un usuario específico identificado por su número de documento de
identidad, siempre que el usuario esté dentro de su alcance de operación.

**Why this priority**: La habilitación de votantes es la operación más crítica del gestor electoral. Sin ella, los
votantes registrados no pueden acceder al sistema.

**Independent Test**: Puede probarse autenticando un gestor con alcance `DEPARTAMENTAL`, enviando el número de documento
de un usuario cuyo municipio pertenece a ese departamento, y verificando el cambio de estado.

**Acceptance Scenarios**:

1. **Habilitación exitosa de un votante**
    - **Given** un gestor con alcance `DEPARTAMENTAL` cuyo departamento es `Antioquia`, y un usuario en estado `ACTIVO`
      con municipio de inscripción en `Antioquia`
    - **When** el gestor solicita cambiar el estado del usuario a `HABILITADO`
    - **Then** el sistema actualiza el estado del usuario a `HABILITADO` y retorna confirmación

2. **Usuario fuera del alcance del gestor**
    - **Given** un gestor con alcance `DEPARTAMENTAL` en `Antioquia`, y un usuario con municipio de inscripción en
      `Cundinamarca`
    - **When** el gestor intenta modificar el estado de ese usuario
    - **Then** el sistema rechaza la operación con error 403 indicando que el usuario está fuera de su alcance

3. **Transición de estado inválida**
    - **Given** un usuario en estado `HABILITADO`
    - **When** el gestor intenta cambiar el estado a `ACTIVO` (transición no permitida)
    - **Then** el sistema rechaza la operación con error 422 indicando que la transición no es válida

4. **Documento no encontrado**
    - **Given** el número de documento `999999999` no existe en el sistema
    - **When** el gestor intenta modificar el estado de ese documento
    - **Then** el sistema retorna error 404 indicando que el usuario no fue encontrado

5. **Intento de modificar a otro gestor**
    - **Given** el documento pertenece a un usuario con rol `GESTOR_ELECTORAL`
    - **When** un gestor intenta modificar su estado
    - **Then** el sistema rechaza la operación con error 403 indicando que no se puede modificar a otro gestor

---

### User Story 2 - Modificación masiva por alcance geográfico (Priority: P1)

Un gestor electoral cambia el estado de todos los usuarios cuyo lugar de inscripción corresponde a un alcance
geográfico (`NACIONAL`, `DEPARTAMENTAL`, `MUNICIPAL`), restringido por su propio `alcance_operacion`.

**Why this priority**: En períodos pre-electorales es necesario habilitar o suspender grandes volúmenes de votantes por
región. La operación masiva evita procesar usuarios uno por uno.

**Independent Test**: Autenticar un gestor departamental, ejecutar modificación masiva con alcance `MUNICIPAL` sobre un
municipio de su departamento, y verificar que solo los usuarios de ese municipio cambian de estado.

**Acceptance Scenarios**:

1. **Habilitación masiva por municipio**
    - **Given** un gestor con alcance `DEPARTAMENTAL` en `Antioquia`, y 50 usuarios `ACTIVO` en el municipio `Medellín`
    - **When** el gestor ejecuta modificación masiva con alcance `MUNICIPAL` sobre `Medellín` a estado `HABILITADO`
    - **Then** los 50 usuarios pasan a `HABILITADO`. El sistema retorna el conteo de usuarios modificados.

2. **Habilitación masiva por departamento**
    - **Given** un gestor con alcance `NACIONAL`, y usuarios en múltiples departamentos
    - **When** el gestor ejecuta modificación masiva con alcance `DEPARTAMENTAL` sobre `Cundinamarca`
    - **Then** solo los usuarios del departamento `Cundinamarca` cambian de estado

3. **Gestor opera fuera de su alcance**
    - **Given** un gestor con alcance `MUNICIPAL` en `Medellín`
    - **When** intenta ejecutar modificación masiva con alcance `DEPARTAMENTAL` o `NACIONAL`
    - **Then** el sistema rechaza la operación con error 403

4. **Sin usuarios coincidentes**
    - **Given** un gestor ejecuta modificación masiva sobre un municipio sin usuarios en el estado origen
    - **When** la operación se completa
    - **Then** el sistema retorna conteo cero y confirmación de operación exitosa

---

### User Story 3 - Modificación masiva por municipio de inscripción (Priority: P1)

Un gestor electoral cambia el estado de todos los usuarios filtrados por un municipio de inscripción específico, dentro
de los límites de su `alcance_operacion`.

**Why this priority**: Similar a US2, pero con filtro directo por municipio en lugar de alcance. Permite granularidad
fina para operaciones puntuales.

**Independent Test**: Autenticar un gestor departamental, filtrar por un municipio de su departamento, y verificar el
cambio de estado solo en ese municipio.

**Acceptance Scenarios**:

1. **Modificación por municipio específico**
    - **Given** un gestor con alcance `DEPARTAMENTAL` en `Valle del Cauca`, y el municipio `Cali`
    - **When** el gestor cambia el estado de usuarios en `Cali` a `INACTIVO`
    - **Then** todos los usuarios con lugar de inscripción en `Cali` pasan a `INACTIVO`

2. **Municipio fuera del alcance del gestor**
    - **Given** un gestor con alcance `MUNICIPAL` en `Medellín`
    - **When** intenta modificar usuarios del municipio `Bello` (diferente al suyo)
    - **Then** el sistema rechaza con error 403

3. **Municipio inexistente**
    - **Given** un municipio_id que no existe en la jerarquía geográfica
    - **When** el gestor intenta la modificación masiva
    - **Then** el sistema retorna error 404

---

### User Story 4 - Listar usuarios por lugar de inscripción (Priority: P2)

Un gestor electoral consulta el listado de usuarios filtrado por municipio de inscripción, con paginación, restringido a
su `alcance_operacion`.

**Why this priority**: La consulta de usuarios es necesaria para auditoría y verificación, pero es una operación de
lectura que no bloquea otras funcionalidades.

**Independent Test**: Autenticar un gestor, solicitar lista de usuarios de un municipio en su alcance, y verificar
paginación y filtrado correctos.

**Acceptance Scenarios**:

1. **Listado paginado exitoso**
    - **Given** un gestor con alcance `DEPARTAMENTAL` en `Antioquia`, y 150 usuarios en el municipio `Medellín`
    - **When** solicita la primera página con tamaño 50
    - **Then** el sistema retorna los primeros 50 usuarios, con metadatos de paginación (total, página actual, total de
      páginas)

2. **Consulta fuera de alcance**
    - **Given** un gestor con alcance `MUNICIPAL` en `Medellín`
    - **When** solicita el listado de usuarios del municipio `Bogotá`
    - **Then** el sistema rechaza con error 403

3. **Municipio sin usuarios**
    - **Given** un municipio válido dentro del alcance del gestor, sin usuarios registrados
    - **When** el gestor consulta el listado
    - **Then** el sistema retorna lista vacía con total cero

---

### User Story 5 - Buscar usuario por número de cédula (Priority: P2)

Un gestor electoral busca un usuario específico por su número de documento de identidad, restringido a su
`alcance_operacion`.

**Why this priority**: La búsqueda puntual es esencial para verificación de identidad y soporte a votantes, pero es
complementaria a las operaciones de modificación.

**Independent Test**: Autenticar un gestor, buscar un número de documento dentro de su alcance, y verificar que retorna
los datos del usuario o 404 si no existe.

**Acceptance Scenarios**:

1. **Búsqueda exitosa**
    - **Given** un gestor con alcance `DEPARTAMENTAL` en `Antioquia`, y existe un usuario con documento `123456789`
      inscrito en `Medellín`
    - **When** el gestor busca por el documento `123456789`
    - **Then** el sistema retorna los datos del usuario (nombre, email, estado, municipio de inscripción)

2. **Usuario fuera de alcance**
    - **Given** un gestor con alcance `MUNICIPAL` en `Medellín`, y existe un usuario con documento `987654321` inscrito
      en `Bogotá`
    - **When** el gestor busca por `987654321`
    - **Then** el sistema retorna error 404 (sin revelar que el usuario existe pero está fuera de alcance)

3. **Documento no encontrado**
    - **Given** el número de documento `000000000` no existe en el sistema
    - **When** el gestor lo busca
    - **Then** el sistema retorna error 404

4. **Búsqueda de gestor electoral**
    - **Given** el documento pertenece a un usuario con rol `GESTOR_ELECTORAL`
    - **When** otro gestor intenta buscarlo
    - **Then** el sistema retorna error 404 (los gestores no son visibles para otros gestores)

---

### Edge Cases

- ¿Qué ocurre si un usuario en estado `HABILITADO` es cambiado a `INACTIVO` mientras tiene un OTP activo?  
  → El OTP debe invalidarse automáticamente.
- ¿Qué ocurre si un usuario es cambiado a `INACTIVO` y tiene solicitudes de cambio de inscripción pendientes?  
  → Las solicitudes pendientes se cancelan automáticamente (ver spec 003-cambio-inscripcion.md).
- ¿Qué ocurre si una modificación masiva abarca miles de usuarios?  
  → La operación debe ser atómica por lote o transaccional según el diseño de base de datos elegido.
- ¿Qué ocurre si el gestor intenta modificarse a sí mismo?  
  → El sistema rechaza la operación con error 403.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir al gestor electoral modificar el estado de un usuario individual identificado por
  número de documento.
- **FR-002**: El sistema DEBE validar que el usuario objetivo esté dentro del `alcance_operacion` del gestor antes de
  cualquier operación.
- **FR-003**: El sistema DEBE restringir las transiciones de estado a las permitidas: `ACTIVO → HABILITADO`,
  `ACTIVO → INACTIVO`, `HABILITADO → INACTIVO`, `INACTIVO → ACTIVO`.
- **FR-004**: El sistema DEBE impedir que un gestor modifique a otro usuario con rol `GESTOR_ELECTORAL`.
- **FR-005**: El sistema DEBE permitir modificación masiva de estado por alcance geográfico (`NACIONAL`,
  `DEPARTAMENTAL`, `MUNICIPAL`).
- **FR-006**: El sistema DEBE permitir modificación masiva de estado por municipio de inscripción específico.
- **FR-007**: El sistema DEBE validar que el alcance solicitado en una operación masiva no exceda el `alcance_operacion`
  del gestor.
- **FR-008**: El sistema DEBE permitir listar usuarios paginados filtrados por municipio de inscripción.
- **FR-009**: El sistema DEBE permitir buscar un usuario por número de documento de identidad (búsqueda exacta).
- **FR-010**: El sistema DEBE retornar error 404 (en lugar de 403) cuando un gestor busca un usuario que existe pero
  está fuera de su alcance, para no revelar información.
- **FR-011**: El sistema DEBE invalidar cualquier OTP activo de un usuario cuando su estado cambia a `INACTIVO`.

### Key Entities

- **Usuario**: Identidad del ciudadano (definido en spec 001-auth&login.md). Se extiende con:
    - **Gestor Electoral**: Usuario con rol `GESTOR_ELECTORAL`. Atributo adicional: `alcance_operacion` (`NACIONAL` |
      `DEPARTAMENTAL` | `MUNICIPAL`). El departamento/municipio de referencia se deriva de su propio `municipio_id` (
      lugar de inscripción de su cédula).
- **Municipio**: División geográfica municipal (definido en spec 001).
- **Departamento**: División geográfica departamental (definido en spec 001).

**Reglas de alcance para validación**:

- `NACIONAL`: opera sobre todo el país.
- `DEPARTAMENTAL`: opera sobre el departamento al que pertenece el municipio de inscripción del gestor.
- `MUNICIPAL`: opera solo sobre el municipio de inscripción del gestor.

Un gestor puede ejecutar operaciones con alcance menor o igual al suyo (ej. un gestor `DEPARTAMENTAL` puede operar sobre
su departamento o sobre un municipio dentro de él, pero no sobre otro departamento ni a nivel `NACIONAL`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un cambio de estado individual se completa en menos de 1 segundo.
- **SC-002**: Una modificación masiva sobre 10,000 usuarios se completa en menos de 5 segundos.
- **SC-003**: La búsqueda por número de documento retorna resultado en menos de 500 ms.
- **SC-004**: El listado paginado de usuarios soporta páginas de hasta 100 registros sin degradación.
- **SC-005**: El 100% de las operaciones fuera de alcance son rechazadas correctamente con el código de error apropiado.
