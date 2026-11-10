# Automatización y distribución

## GitHub Actions

.github/workflows/ci.yml prepara dos trabajos independientes en runners hospedados de GitHub: API (Java 21, pruebas de integración y JAR mediante Maven Wrapper) e interfaz (Node 22.23.1, npm ci, pruebas Angular y build de producción). Se ejecutan para pushes a main, pull requests y solicitudes manuales. No requieren PAT, claves cloud ni secretos del proyecto. Las cuentas de prueba pertenecen únicamente a H2 en memoria.

El workflow tiene permiso contents: read, tiempos máximos de 15 minutos y acciones fijadas por SHA. Informes Java y compilaciones se conservan como artefactos durante siete días. No despliega servicios ni publica paquetes. Dependabot propone actualizaciones mensuales de Maven, npm y acciones; cada propuesta requiere revisión, no se fusiona automáticamente.

Configuración preparada localmente. La primera ejecución remota y sus enlaces se añadirán después de autorizar la publicación. No se presenta como CI aprobado hasta entonces.

## Paquete de código fuente

Desde la raíz en PowerShell:

```powershell
.\scripts\package-source.ps1
```

Genera release/maestranza-erp-source-FECHA.zip y un archivo SHA256. Incluye fuentes, pruebas, migraciones, wrappers, configuración pública, documentos y capturas. Usa una lista explícita de rutas; excluye .env, datos PostgreSQL, registros, sesiones, node_modules, target y archivos privados locales.

Es un paquete de fuentes, no un instalador ni un despliegue listo para Internet. Extraer y seguir docs/runbook.md para instalar dependencias, compilar y configurar las credenciales propias. Los artefactos frontend-dist de CI necesitan un servidor web con proxy /api al backend; no basta abrir index.html como archivo. El modo local documentado usa el servidor de desarrollo Angular.

No se ha asignado todavía una licencia al proyecto. Su elección queda pendiente; la publicación del código no equivale a conceder una licencia.

