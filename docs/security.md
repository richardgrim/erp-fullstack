# Seguridad del prototipo local

Sesiones de servidor con cookie HttpOnly y SameSite Strict; caducidad por inactividad de 30 minutos. CSRF obligatorio en operaciones de escritura, incluido inicio y cierre de sesión. El identificador de sesión cambia al autenticar. No se guardan contraseñas ni tokens en localStorage.

Administrador: registrar clientes y órdenes, confirmar entregas y crear operadores.
Operador: consultar clientes, órdenes e historial; iniciar y terminar trabajos. Las restricciones se comprueban en el servidor, además de ocultar controles en la interfaz.

Contraseñas con BCrypt (coste 12), mínimo 12 caracteres y máximo 72 bytes UTF-8. La configuración inicial solo crea admin si no existe ningún administrador; no restablece cuentas. El script solicita la clave de forma oculta y la transmite temporalmente mediante el entorno del proceso, sin guardarla en archivos. Reiniciar sin ConfigureAdmin después de crear la cuenta.

El historial atribuye los nuevos eventos al usuario autenticado. Los registros anteriores conservan actor nulo. Un administrador de base de datos puede modificar el historial: no es un registro inmutable.

Solo uso local con datos ficticios. Puertos limitados a 127.0.0.1. .env excluido de Git. Antes de un despliegue compartido faltan HTTPS y cookie Secure, limitación de intentos de acceso, recuperación de cuentas, copias de seguridad, análisis de dependencias y CI. No exponer este prototipo a Internet.

Inventario: el servidor restringe creación de materiales y entradas a ADMIN. Ambos roles pueden consumir únicamente en órdenes en proceso. Bloqueos de fila y restricciones SQL protegen stock y relaciones. Los movimientos no se editan ni borran mediante la API. Autoría obtenida de la sesión, nunca del formulario.
