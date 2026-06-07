# MiniMarket Plus - Backend Seguro

## Descripción del proyecto

MiniMarket Plus es una aplicación backend desarrollada con Spring Boot para la gestión de usuarios, productos, categorías, carrito, inventario y operaciones asociadas a un sistema de minimarket.

El proyecto incorpora mecanismos de seguridad mediante Spring Security, JWT y autorización basada en roles, permitiendo proteger los endpoints del backend y controlar el acceso según el perfil del usuario autenticado.

## Tecnologías utilizadas

- Java
- Spring Boot
- Spring Security
- JWT
- Maven
- Spring Data JPA
- H2 Database
- BCrypt
- Postman para pruebas

## Seguridad implementada

El backend implementa autenticación y autorización mediante Spring Security y JWT.

### Autenticación

- Inicio de sesión mediante `/api/auth/login`.
- Generación de token JWT.
- Validación de credenciales mediante `AuthenticationManager`.
- Carga de usuarios mediante `UserDetailsService`.
- Contraseñas cifradas con BCrypt.

### Autorización

Se definieron tres roles principales:

- `ROLE_CLIENTE`
- `ROLE_EMPLEADO`
- `ROLE_GERENTE`

Los endpoints se protegen mediante anotaciones `@PreAuthorize`, utilizando `hasAuthority` y `hasAnyAuthority`.

## Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| gerente | gerente123 | ROLE_GERENTE |
| empleado | empleado123 | ROLE_EMPLEADO |
| cliente | cliente123 | ROLE_CLIENTE |

## Ejecución del proyecto

Compilar el proyecto:

```bash
./mvnw clean compile