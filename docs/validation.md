# Validación de clientes y seguimiento

Fecha: 9 de noviembre de 2026.

## Pruebas automáticas

- API, entrega anterior: 8 pruebas de integración aprobadas con servidor HTTP real y H2 en modo PostgreSQL.
- Registro de cliente y orden, recuperación del cliente al listar, rechazo de datos inválidos y cliente inexistente.
- Recorrido completo de estados con historial cronológico, rechazo de saltos y retrocesos, estados desconocidos y orden inexistente.
- Dos solicitudes simultáneas de inicio producen una respuesta 200 y otra 409; solo se añade una transición.
- Angular, entrega anterior: 5 pruebas aprobadas: creación, errores de conexión, cambio de estado e historial, conflicto de actualización y filtro de órdenes entregadas.
- Compilación de Angular sin advertencias de tamaño y empaquetado de Spring Boot completados.
- El umbral de aviso del CSS del componente es 6 kB (error a 8 kB), ajustado al incorporar el seguimiento.

## Prueba con PostgreSQL

La migración V2 conserva la orden de la primera entrega y añade su registro inicial
con la fecha de creación original. Se ejecuta el flujo de pendiente a entregada en el
navegador para Taller Norte · Demostración, con datos ficticios. Se comprueba que
el estado y las cuatro entradas del historial persistan después de recargar.

## Alcance de la evidencia

H2 no sustituye PostgreSQL: la prueba manual valida la migración y el recorrido principal
contra PostgreSQL 17. La concurrencia se prueba automáticamente con H2; su validación
bajo carga con PostgreSQL está pendiente. CI remoto todavía no configurado.
Los eventos anteriores a la migración V3 no tienen usuario; los nuevos identifican al autor autenticado.

## Acceso y usuarios (tercera entrega)

13 pruebas de API y 9 de Angular aprobadas (22 en total), 9 de noviembre de 2026. Empaquetado Java y compilación Angular completados.

Las pruebas adicionales verifican acceso anónimo rechazado, CSRF, contraseña incorrecta, rotación de sesión, cierre de sesión, almacenamiento BCrypt, creación exclusiva de operadores, permisos del operador y autoría de cambios. La interfaz prueba inicio y cierre de sesión, restricciones visuales y expiración por respuesta 401.

Las pruebas de autenticación usan cuentas exclusivamente de prueba y H2. La verificación interactiva con la cuenta local queda pendiente hasta que el propietario defina su contraseña.


Comprobación local adicional: migración V3 aplicada en PostgreSQL, cero usuarios iniciales y cuatro eventos anteriores conservados sin autor. Interfaz responde HTTP 200 y API rechaza consulta anónima con 401. La revisión visual quedó pendiente por bloqueo del navegador integrado. API detenida al finalizar para permitir configurar la cuenta inicial.

## Inventario (cuarta entrega)

17 pruebas de integración del servidor y 12 de Angular aprobadas: 29 en total. Las nuevas pruebas cubren entradas/consumos decimales, stock y autoría persistidos, cantidades negativas/cero/fraccionarias inválidas, límites numéricos, órdenes pendientes/cerradas, permisos y dos consumidores simultáneos con stock insuficiente para ambos. Uno obtiene 201 y otro 409; solo se registra el consumo aceptado. Angular comprueba permisos visuales, selección de órdenes activas, actualización del stock/historial y conservación del formulario ante conflicto.

Empaquetado Java y compilación Angular completados. La concurrencia automatizada se verifica en H2; una prueba de carga sobre PostgreSQL sigue pendiente. La revisión interactiva del inventario requiere que el propietario vuelva a iniciar sesión tras actualizar la API.

V4 aplicada correctamente en PostgreSQL local. Se conservan la cuenta administradora y la orden existente. El inventario parte vacío; no se introdujeron materiales ni movimientos ficticios en esta actualización.

## Recorrido de inventario verificado en navegador y PostgreSQL

9 de noviembre de 2026: se reutiliza el material Acero redondo registrado por el propietario (METRO, stock inicial cero). Entrada ficticia de 10 metros y consumo ficticio de 2.5 metros asociado a OT-0034, creada para esta validación e iniciada con el cliente de demostración. Resultado: 7.5 metros. Un intento de consumir 8 metros devuelve conflicto por stock insuficiente, sin añadir movimiento ni cambiar stock. Tras recargar, persisten el saldo y los dos movimientos con autor admin, fechas y orden. Evidencia: evidence/inventario-validado.png. La orden de prueba permanece en proceso.

## Resumen operativo (quinta entrega)

16 pruebas de Angular aprobadas, incluidas cuatro nuevas: límites inclusivos del día local, combinación de filtros y limpieza, intervalo invertido y actualización de contadores tras cambios de estado. No hay cambios de servidor en esta entrega; sus 17 pruebas aprobadas corresponden a la entrega de inventario. Se verificó previamente en navegador el período 09/11/2026 y estado En proceso: una de dos órdenes coincide. La verificación visual final queda pendiente de iniciar sesión tras reiniciar el entorno.

Compilación de producción Angular completada sin advertencias de tamaño. Entorno local reiniciado; la pestaña antigua del navegador integrado no permitió la comprobación final.

## Publicación y primera ejecución remota

9 de noviembre de 2026 (America/La_Paz): publicación autorizada de richardgrim/erp-fullstack. [ERP CI #1](https://github.com/richardgrim/erp-fullstack/actions/runs/42743654992) completó backend y frontend correctamente sobre el commit 66447e7eeab1b72dcb5d4c616e88254b08e677e1. Se generaron backend-jar, frontend-dist y backend-test-reports. GitHub mostró avisos de transición de runtimes de acciones y de ubuntu-latest, sin fallos de ejecución. La licencia se eligió posteriormente: MIT, autorizada por el propietario.
