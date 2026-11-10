# Ejecución local en Windows

Requisitos existentes: Java 21, Node.js 22.12 o superior en la rama 22, Docker Desktop.
No hace falta instalar Maven ni Angular globalmente: se usa Maven Wrapper y el CLI local.

## 1. Base de datos

Desde la raíz de este proyecto, con Docker Desktop abierto:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Reemplaza el valor de DB_PASSWORD por una contraseña local propia. Luego:

```powershell
docker compose up -d --wait
```

El volumen `erp-maestranza_erp-data` conserva los registros al detener el contenedor.
Cambiar la contraseña de .env no cambia una contraseña ya inicializada en el volumen.

## 2. Compilar e iniciar la API

Primera ejecución, desde la raíz del proyecto (Java 21 debe estar disponible):

```powershell
Set-Location backend
.\mvnw.cmd --batch-mode --no-transfer-progress verify
Set-Location ..
.\scripts\start-api.ps1 -ConfigureAdmin
```

El aviso oculto solicita la contraseña de la primera cuenta admin. Después del primer acceso, detén con Ctrl+C e inicia con .\scripts\start-api.ps1, sin ConfigureAdmin. No vuelve a crear ni cambia una cuenta existente.

## 3. Interfaz

En otra terminal, desde la raíz del proyecto:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd start
```

Abre http://127.0.0.1:4200 e inicia sesión como admin. Registra un cliente ficticio y luego su orden.
La interfaz se comunica con la API mediante el proxy del servidor de desarrollo.

En cada orden, usa Iniciar trabajo, Marcar como terminada y Registrar entrega.
Ver historial muestra el registro inicial y los cambios. El filtro permite consultar cada estado.
Flyway aplica las migraciones pendientes al iniciar la API y conserva los datos existentes.

## Pruebas

Backend: desde backend, `.\mvnw.cmd --batch-mode --no-transfer-progress verify`.
Frontend: desde frontend, `npm.cmd test -- --watch=false` y `npm.cmd run build`.

En Windows, detén la API antes de ejecutar `verify` o `package`: el proceso Java puede
bloquear el JAR e impedir que Maven lo reemplace. `test` puede ejecutarse por separado.

## Detener

Ctrl+C en las terminales de API e interfaz. Desde la raíz: `docker compose stop`.
No usar `down -v` si deseas conservar los datos.

## Crear la primera cuenta local

Con PostgreSQL iniciado y la API detenida, desde la raíz del proyecto:

```powershell
.\scripts\start-api.ps1 -ConfigureAdmin
```

Introduce tu contraseña en el aviso oculto (mínimo 12 caracteres; máximo 72 bytes UTF-8). No se escribe en archivos ni se incluye en los argumentos del comando. El usuario inicial es `admin`. Si ya existe un administrador, este procedimiento no cambia su contraseña.

Después de iniciar sesión correctamente, detén la API con Ctrl+C y vuelve a iniciarla sin la opción de configuración para retirar la clave inicial del entorno del proceso:

```powershell
.\scripts\start-api.ps1
```

El administrador puede abrir Usuarios y crear cuentas de operador. Los operadores inician y terminan trabajos; únicamente el administrador registra entregas.

## Materiales e inventario

Abre Materiales e inventario. Como administrador, registra un material con su unidad de medida; comienza en cero. Selecciónalo, elige Entrada de almacén, indica cantidad y motivo, y registra el movimiento.

Para consumir, crea/inicia una orden en Operaciones, pulsa Actualizar inventario y selecciona Consumo en orden. Elige la orden en proceso y la cantidad. Una salida superior al stock se rechaza sin alterar las existencias. Operadores pueden consultar y consumir; no pueden crear materiales ni registrar entradas.

La unidad no se cambia después del registro. Usa datos ficticios para esta demostración. Cada movimiento aparece en el historial del material con la orden, usuario y fecha. Para consultar un estado actualizado tras trabajar en otra pestaña, pulsa Actualizar inventario.

## Resumen operativo

Desde y Hasta filtran la fecha de creación de la orden, incluyendo ambos días según la hora local del equipo. Se pueden usar por separado. Se combinan con búsqueda y estado; todos los contadores de órdenes reflejan el resultado. Clientes es el total de contactos, sin filtros. Las terminadas son trabajos pendientes de entrega; las entregadas están cerradas. Un período invertido muestra una advertencia. Limpiar filtros restaura el listado completo. Los filtros no se guardan al recargar.
