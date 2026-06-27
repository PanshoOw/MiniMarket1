# Minimarket Plus API

Backend REST para la gestión operativa de un minimarket, desarrollado con Spring Boot. El sistema incorpora autenticación con JWT, autorización basada en roles, control de usuarios, productos, inventario, ventas y carrito de compras, junto con pruebas automatizadas para validar reglas de negocio y seguridad.

## Características principales

* Autenticación de usuarios mediante JWT.
* Autorización por roles con Spring Security.
* Gestión de usuarios con DTOs para evitar exposición de datos sensibles.
* Gestión de productos con validaciones de negocio.
* Registro y control de movimientos de inventario.
* Registro de ventas mediante flujo controlado de negocio.
* Gestión de carrito de compras.
* Registro de auditoría para eventos de login exitoso y fallido.
* Pruebas automatizadas con JUnit, Mockito y MockMvc.
* Reporte de cobertura con JaCoCo.
* Correcciones y mejoras guiadas por análisis estático con SonarQube.

## Stack técnico

* Java
* Spring Boot
* Spring Web
* Spring Security
* JWT
* Spring Data JPA
* Maven
* JUnit 5
* Mockito
* MockMvc
* JaCoCo
* SonarQube
* Git / GitHub

## Módulos del sistema

| Módulo        | Descripción                                                        |
| ------------- | ------------------------------------------------------------------ |
| Autenticación | Login, generación de token JWT y registro público de usuarios.     |
| Usuarios      | Administración de usuarios mediante DTOs y validaciones.           |
| Productos     | Creación, actualización, eliminación y consulta de productos.      |
| Inventario    | Registro de movimientos de entrada y salida de productos.          |
| Ventas        | Registro de ventas con validaciones de usuario, productos y stock. |
| Carrito       | Gestión de productos asociados al carrito de un cliente.           |
| Seguridad     | Restricción de endpoints según rol del usuario autenticado.        |
| Auditoría     | Registro de eventos relevantes de autenticación.                   |

## Roles disponibles

| Rol             | Permisos principales                                                    |
| --------------- | ----------------------------------------------------------------------- |
| `ROLE_GERENTE`  | Administración de usuarios, productos, inventario y consulta de ventas. |
| `ROLE_EMPLEADO` | Operación de inventario y registro de ventas.                           |
| `ROLE_CLIENTE`  | Acceso restringido a funcionalidades propias del cliente.               |

El registro público asigna siempre el rol `ROLE_CLIENTE`, evitando que un usuario pueda registrarse directamente con privilegios administrativos u operativos.

## Seguridad

La API utiliza Spring Security junto con JWT para proteger los endpoints. El flujo general contempla:

1. El usuario envía credenciales al endpoint de login.
2. El sistema valida las credenciales.
3. Si son correctas, se genera un token JWT.
4. El token se usa para acceder a endpoints protegidos.
5. Los permisos se validan según el rol del usuario.

También se contemplan respuestas controladas para credenciales inválidas y eventos de auditoría para login exitoso o fallido.

## Buenas prácticas aplicadas

* Uso de DTOs en entradas y respuestas públicas.
* No exposición de contraseñas en respuestas de usuario.
* Validación de campos obligatorios.
* Validación de usuarios y correos duplicados.
* Validación de roles existentes.
* Encriptación de contraseñas mediante `PasswordEncoder`.
* Separación entre lógica de controlador y lógica de negocio.
* Uso de métodos específicos como `registrarVenta()` y `registrarMovimiento()` en lugar de persistencia directa indiscriminada.
* Pruebas para escenarios exitosos, negativos y casos límite.
* Verificación explícita de operaciones que no deben ejecutarse en escenarios inválidos.

## Pruebas automatizadas

El proyecto incluye pruebas orientadas a validar controladores, reglas de negocio y seguridad por roles.

| Clase de prueba            | Cobertura principal                                                          |
| -------------------------- | ---------------------------------------------------------------------------- |
| `AuthControllerTest`       | Login válido, login inválido, registro público y validaciones de registro.   |
| `UsuarioControllerTest`    | CRUD de usuarios, uso de DTOs, validaciones y protección de datos sensibles. |
| `ProductoControllerTest`   | Creación, actualización, eliminación y validaciones de productos.            |
| `InventarioControllerTest` | Movimientos de inventario, entradas, salidas y validaciones.                 |
| `VentaControllerTest`      | Registro de ventas mediante DTOs y validaciones de negocio.                  |
| `CarritoControllerTest`    | Agregado de productos, stock, cantidades inválidas y casos de error.         |
| `SecurityIntegrationTest`  | Autorización por roles en usuarios, productos, inventario y ventas.          |

## Ejecución del proyecto

Clonar el repositorio:

```bash
git clone <url-del-repositorio>
cd <nombre-del-proyecto>
```

Ejecutar la aplicación:

```bash
mvnw.cmd spring-boot:run
```

En Linux/macOS:

```bash
./mvnw spring-boot:run
```

## Ejecución de pruebas

Para ejecutar todas las pruebas:

```bash
mvnw.cmd clean test
```

En Linux/macOS:

```bash
./mvnw clean test
```

## Reporte de cobertura

Para ejecutar las pruebas y generar el reporte de cobertura con JaCoCo:

```bash
mvnw.cmd clean verify
```

En Linux/macOS:

```bash
./mvnw clean verify
```

El reporte queda disponible en:

```text
target/site/jacoco/index.html
```

## Estructura general

```text
src
├── main
│   ├── java
│   │   └── com.minimarket
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── repository
│   │       ├── security
│   │       └── service
│   └── resources
└── test
    └── java
        └── com.minimarket
            └── controller
```

## Endpoints principales

| Recurso       | Ruta base         |
| ------------- | ----------------- |
| Autenticación | `/api/auth`       |
| Usuarios      | `/api/usuarios`   |
| Productos     | `/api/productos`  |
| Inventario    | `/api/inventario` |
| Ventas        | `/api/ventas`     |
| Carrito       | `/api/carrito`    |

## Calidad y mantenibilidad

El proyecto fue reforzado con pruebas automatizadas, validaciones explícitas, control de acceso por roles y mejoras de diseño orientadas a seguridad y mantenibilidad. La estructura permite extender nuevos módulos sin acoplar directamente la capa de exposición HTTP con las entidades persistentes.

## Estado

Backend funcional con autenticación, autorización, validaciones de negocio, pruebas automatizadas y reporte de cobertura.
