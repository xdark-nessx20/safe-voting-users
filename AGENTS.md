# Safe-Voting: Users Module (Módulo 1 — Gestión de Usuarios)

Este repositorio es el microservicio de usuarios del sistema Safe-Voting.  
Maneja registro, autenticación OTP, ciclo de vida de votantes, y cambios de lugar de inscripción.

## Estado actual

Pre-implementación — no hay código fuente, build system, ni framework elegido.  
El proyecto está en fase de especificación y planificación.

## Documentación

Los specs de features viven en `docs/spec/` y siguen la plantilla `spec-template.md`.  
El plan de implementación usa `docs/plan/plan-template.md`.

Flujo de trabajo por feature: `spec → plan → implementar`

**No commitear archivos template** — `.gitignore` excluye `*template.md`.

## Dominio

- **Stakeholders**: Votante, Gestor Electoral, Administrador (`docs/stakeholders.md`)
- **Usuario**: nombre, email (único), teléfono, documento (único), municipio de inscripción
- **Estados de usuario**: ACTIVO → HABILITADO ⇄ INACTIVO. Solo `HABILITADO` puede autenticarse.
- **Login**: OTP de 6 dígitos enviado por correo, expira 5 min, máximo 3 intentos.
- **Gestor Electoral**: creado por Admin, tiene `alcance_operacion` (NACIONAL | DEPARTAMENTAL | MUNICIPAL) derivado de su propio municipio de inscripción.
- **Jerarquía geográfica**: Departamento → Municipio. El lugar de inscripción del usuario es FK a Municipio.

## Repositorio

- Remote: `origin` → `https://github.com/xdark-nessx20/safe-voting-users.git`
- Todo el contenido está en español.
