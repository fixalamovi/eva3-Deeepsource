# Validación de Spring Boot + SQLite

Esta página describe la versión 1.6 y su evidencia. Los cambios de acceso de la versión 1.7 están en [Acceso a Deepsource](LOGIN_Y_CONEXION.md).

## Verificado

- Compilación de ambos microservicios con Maven, JDK 21 y Spring Boot 3.5.16.
- 15 pruebas backend aprobadas: seis de API de proyectos/cuentas/tareas, seis de registros y tres de contrato HTTP.
- Las pruebas de API utilizan controladores y repositorios reales con archivos SQLite temporales. Sustituyen las llamadas a otros servicios por dobles; el contrato HTTP se comprueba por separado.
- Compilación directa de todas las fuentes Kotlin Android y Compose con las dependencias instaladas.
- 77 pruebas unitarias Android aprobadas en el corte de pruebas y documentación.
- Compilación de las fuentes de pruebas instrumentadas, incluida la separación de IDs entre caché del servidor y demostración.
- Sintaxis de los scripts PowerShell y XML del proyecto revisada.

## Límites de la comprobación

- El entorno bloquea sockets locales: no permitió iniciar Tomcat ni realizar una prueba con los dos servicios en puertos HTTP reales. Debe completarse en el equipo de ejecución.
- El empaquetado Maven encontró dependencias de archivado aún no descargadas; la primera compilación con acceso a Maven Central debe descargarlas. No se entregan JAR ejecutables precompilados.
- No se ejecutó la app en emulador ni se generó un APK nuevo.
- El consumo de una API externa permanece pendiente de implementar.
- Se incluyó un informe JaCoCo real: 19,89 % global del conjunto lógico y 87,58 % en dominio. El mínimo global del 80 % permanece pendiente.
- Clave release propia generada y certificado exportado; firma Gradle y script de generación preparados. No se generó un APK nuevo. El flujo del script debe comprobarse en Android Studio.

## Prueba de aceptación

1. Ejecuta `backend/iniciar.ps1` y verifica `http://127.0.0.1:8081/api/health` y `http://127.0.0.1:8082/api/health`.
2. Ejecuta **serverDebug** en el emulador y crea una cuenta.
3. Crea un proyecto y una tarea propia.
4. Guarda dos registros para la misma tarea y comprueba el total.
5. Edita y elimina un registro y vuelve a consultar la lista.
6. Inicia y pausa el cronómetro. Termínalo para verificar redondeo y confirmación del servidor.
7. Reabre la app y reinicia los servicios para verificar persistencia.
8. Entra en demostración y comprueba que sus datos siguen separados.

El backend expone edición y eliminación de proyectos y tareas. La interfaz móvil actual ofrece creación/consulta de proyectos y tareas, y CRUD completo de registros de horas.
