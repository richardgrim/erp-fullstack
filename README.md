# Maestranza ERP

ERP en desarrollo para organizar los trabajos de una maestranza.
Proyecto de portafolio de Richard Grichukin Mendez. Los datos de prueba son ficticios.

## Qué permite esta entrega

- Iniciar sesión con roles de administrador y operador, y crear operadores desde la cuenta administradora.
- Registrar clientes con nombre, teléfono y correo opcional.
- Crear una orden vinculada a un cliente existente.
- Consultar y buscar órdenes, con filtros por estado.
- Avanzar de pendiente a en proceso, terminada y entregada, sin saltos ni retrocesos.
- Consultar el historial de cada orden con fechas de registro y de cambios.
- Registrar materiales, entradas y consumos por orden, con stock e historial de movimientos.
- Consultar un resumen por estado y filtrar órdenes por fecha de creación, texto y estado.
- Validar los datos tanto en la interfaz como en la API.

## Tecnologías

Angular 21, Java 21, Spring Boot 4.0.9, PostgreSQL 17 y Flyway.
Un monolito modular es suficiente para este primer flujo; no se requieren servicios cloud.

## Ejecutar

Consulta [la guía local](docs/runbook.md). La interfaz usa el puerto 4200,
la API 8081 y PostgreSQL 5433. Todos se limitan al equipo local.

## Documentación

- [Alcance y arquitectura](docs/architecture.md)
- [Pruebas](docs/validation.md)
- [Seguridad y límites](docs/security.md)
- [Automatización y paquetes](docs/ci-and-packaging.md)

## Próximas entregas

1. Elegir una licencia para el proyecto.
2. Verificar la primera ejecución de GitHub Actions.
3. Incorporar el proyecto al índice portfolio.

Esta entrega todavía no incluye edición, borrado, facturación ni valoración económica del inventario.
No utilizar con datos reales ni exponer a Internet.
Licencia pendiente de elección; este repositorio todavía no concede una licencia de uso.






