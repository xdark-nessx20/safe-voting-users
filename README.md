# Sistema de Votaciones Online

Este documento define la infraestructura técnica y lógica para la gestión de procesos electorales digitales, integrando el registro y habilitación de votantes, la administración de partidos, candidatos y jornadas de votación, y la auditoría de resultados en tiempo real.

---

## 👤 Módulo 1: Gestión de Usuarios

Este módulo centraliza el registro, validación y ciclo de vida de los ciudadanos dentro del sistema. Es la fuente de verdad para determinar quién está autorizado a participar en una jornada electoral.

### 1.1. Atributos del Usuario

Cada ciudadano registrado debe contar con los siguientes datos de identidad:

* **Nombre completo:** Identificación nominal del votante.
* **Email:** Canal principal de comunicación y recuperación de cuenta.
* **Teléfono:** Dato de contacto secundario.
* **Número de documento de identidad:** Clave única de identificación civil dentro del sistema.
* **Lugar de inscripción del documento:** Municipio o entidad donde fue expedido el documento, utilizado para validar el alcance geográfico del voto.

### 1.2. Estados del Usuario (Ciclo de Vida)

El sistema debe gestionar las transiciones de estado para garantizar la integridad del proceso electoral:

* **ACTIVO:** El ciudadano está registrado en el sistema pero aún no ha sido habilitado para votar.
* **HABILITADO:** El usuario ha superado la validación de identidad y tiene permiso para autenticarse y emitir su voto.
* **INACTIVO:** El usuario ha sido suspendido del sistema por inconsistencias en sus datos, irregularidades detectadas u otras causas administrativas.

> ⚠️ Solo los usuarios en estado **HABILITADO** pueden autenticarse y participar en una votación activa.

---

## 🗳️ Módulo 2: Partidos, Candidatos y Votaciones

Este módulo administra los actores del proceso electoral (partidos y candidatos) y la configuración de cada jornada de votación. Define las reglas bajo las cuales se emiten y contabilizan los votos.

### 2.1. Partidos Políticos

Un partido político es la entidad organizadora que agrupa a los candidatos. Sus atributos son:

* **Nombre:** Denominación oficial del partido.
* **Descripción:** Texto opcional con la presentación o ideario del partido.
* **Logo (URL):** Enlace a la imagen representativa del partido. Al no almacenar binarios en base de datos, este campo persiste como un `String` con la URL pública de la imagen.

### 2.2. Candidatos

El candidato es una entidad propia dentro del sistema, ya que aunque su información base provenga de un usuario registrado, requiere atributos adicionales propios del contexto electoral:

* **Relación con Usuario:** Referencia al registro del ciudadano en el Módulo 1.
* **Relación con Partido:** Vínculo al partido político que lo postula.
* **Foto (URL):** Enlace a la imagen del candidato que se mostrará en el tarjetón digital. Al igual que el logo del partido, se persiste como un `String` con la URL pública.

> 📌 La separación entre **Usuario** y **Candidato** permite que un ciudadano registrado en el sistema pueda ser postulado en múltiples procesos electorales sin duplicar su información de identidad.

### 2.3. Votaciones

Una votación representa una jornada electoral configurable. Sus atributos son:

* **Nombre:** Título descriptivo de la jornada (ej. *Elecciones Presidenciales 2026*).
* **Tipo:** Categoría del cargo en disputa.

    | Valor | Descripción |
    | :--- | :--- |
    | `PRESIDENCIA` | Elección del jefe de Estado a nivel nacional. |
    | `CONGRESO` | Elección de representantes legislativos. |
    | `ALCALDIA` | Elección del mandatario municipal. |
    | `GOBERNACION` | Elección del mandatario departamental. |

* **Alcance:** Cobertura geográfica de la votación.

    | Valor | Descripción |
    | :--- | :--- |
    | `MUNICIPAL` | Aplica a un único municipio. |
    | `DEPARTAMENTAL` | Aplica a un departamento completo. |
    | `REGIONAL` | Abarca una región o conjunto de departamentos. |
    | `NACIONAL` | Cubre el territorio nacional completo. |

* **Estado (Ciclo de Vida):**

    * **ACTIVA:** La votación está configurada.
    * **EN_PROGRESO:** La jornada está abierta para recibir votos.
    * **FINALIZADA:** La jornada cerró exitosamente y los resultados están disponibles.
    * **CANCELADA:** La votación fue suspendida antes de completarse.

### 2.4. Votos

Cada emisión de voto queda registrada de forma individual y auditada:

* **Estado del Voto:**

    * **VÁLIDO:** El voto fue emitido correctamente y se contabiliza en los resultados.
    * **ANULADO:** El voto fue marcado como inválido por inconsistencias detectadas durante o después de la jornada.

---

## 📊 Módulo 3: Auditoría y Resultados

Este módulo garantiza la transparencia del proceso electoral mediante el registro de snapshots de resultados y la generación de reportes parciales y totales. Es la herramienta de trazabilidad y rendición de cuentas del sistema.

### 3.1. Tipos de Snapshot

Los snapshots capturan el estado de los votos en un momento determinado de la jornada:

* **Parcial:** Corte intermedio de resultados filtrado por una dimensión geográfica. Permite, por ejemplo, consultar los resultados de una votación presidencial desagregados por departamento o por municipio antes del cierre oficial de la jornada.
* **Total:** Resumen consolidado de toda la jornada electoral, agrupando la totalidad de los votos emitidos independientemente del alcance geográfico.

### 3.2. Matriz de Conteo por Estado de Voto

La generación de resultados —tanto parciales como totales— aplica la siguiente lógica de contabilización:

| Estado del Voto | ¿Se contabiliza? | Observación |
| :--- | :--- | :--- |
| **VÁLIDO** | ✅ Sí | Se suma al marcador del candidato correspondiente. |
| **ANULADO** | ❌ No | Se excluye del conteo por candidato pero se registra en el total de votos procesados para efectos de auditoría. |

### 3.3. Dimensiones de Auditoría

Los reportes parciales pueden segmentarse según el alcance configurado en la votación:

* **Por Municipio:** Resultados desagregados a nivel municipal (aplica a votaciones con alcance `MUNICIPAL`, `DEPARTAMENTAL`, `REGIONAL` o `NACIONAL`).
* **Por Departamento:** Resultados consolidados a nivel departamental.
* **Por Región:** Agrupación de resultados entre departamentos de una misma región.
* **Nacional:** Vista agregada de todos los votos válidos a nivel país.
