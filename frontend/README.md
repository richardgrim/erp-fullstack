# Frontend de Maestranza ERP

Interfaz Angular para registrar clientes y recibir órdenes de trabajo.
Consulta [la guía del proyecto](../docs/runbook.md) para iniciar la API y PostgreSQL.

- `npm ci`: instalar las dependencias fijadas en package-lock.json.
- `npm start`: iniciar en localhost:4200 con proxy local hacia la API.
- `npm test -- --watch=false`: ejecutar las pruebas.
- `npm run build`: compilar para producción; no publica el resultado.

Vitest se fija en 4.0.18 para evitar un fallo del resolvedor de dependencias de npm 10
con el conjunto de versiones que seleccionaba la plantilla inicial.
