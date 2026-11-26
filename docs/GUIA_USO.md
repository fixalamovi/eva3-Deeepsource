# Ejecutar y revisar Deepsource

## Versión conectada para EVA 4

1. Extrae el ZIP. Abre `Deepsource_eva4`, donde está `settings.gradle.kts`, en Android Studio.
2. Sincroniza Gradle con internet. Instala SDK 37 y utiliza el JDK integrado compatible con Java 25 solicitado por el proyecto.
3. En PowerShell, dentro de `backend`, ejecuta `.\iniciar.ps1`. Requiere JDK 17 o posterior y Maven; el detalle está en [backend/README.md](../backend/README.md).
4. Selecciona `serverDebug` y ejecuta un emulador. La app utiliza `10.0.2.2:8081` y `10.0.2.2:8082`.
5. Crea una cuenta de servidor. Las cuentas locales antiguas corresponden a la variante `localDebug`.
6. Crea un proyecto. Utiliza una tarea predefinida o crea otra; guarda varios registros para esa misma tarea.
7. En Registros, busca, filtra, edita y elimina un registro. Consulta el resumen y exporta un CSV.
8. Abre Cronómetro, selecciona proyecto y tarea e inicia. Puedes pausar/reanudar; al terminar se guarda el tiempo activo redondeado hacia arriba al minuto completo.
9. Para detener los servicios conservando los datos, ejecuta `backend/detener.ps1`.

## Probar sin cuenta

Pulsa **Explorar demostración** en el login. Los datos son locales y están separados de las cuentas de servidor. Para usar cuentas del dispositivo selecciona `localDebug`.

## Perfil y recursos nativos

En Perfil puedes editar el nombre y, para cuentas de correo, cambiar la contraseña. **Tomar foto** solicita permiso de cámara; **Elegir de la galería** utiliza el selector del sistema. Cancelar conserva la foto anterior. La imagen se guarda por cuenta.

En Widget, utiliza **Añadir a pantalla de inicio** o el selector de widgets del launcher. El contenido depende del espacio disponible; sus accesos permiten abrir el registro de horas y el cronómetro.

## Pruebas, firma y documentación

- `.\scripts\probar.ps1`: pruebas JVM e informe de cobertura. Agrega `-Dispositivo` para las instrumentadas o `-Exigir80` para comprobar el mínimo global.
- `.\scripts\generar-release.ps1`: prueba, compila `serverRelease` y verifica/copia el APK. Debe ejecutarse con Gradle operativo en Android Studio.
- [Documentación técnica](DOCUMENTACION_TECNICA.md).
- [Resultados y límites de las pruebas](PRUEBAS.md).
- [Clave y APK firmado](APK_FIRMADO.md).
- [Matriz de entrega](ENTREGA_EVA4.md).

Este ZIP incluye la clave propia y la configuración, pero no un APK release nuevo. No se ejecutó la versión final en un emulador en este corte. Consulta las evidencias para distinguir compilación, pruebas y verificaciones pendientes.
