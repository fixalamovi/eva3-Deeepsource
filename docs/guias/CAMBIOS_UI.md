# Cambios de interfaz

Las pantallas están en `app/src/main/java/com/example/deepsource/ui/screens`, los controles reutilizables en `ui/components` y el tema en `ui/theme`.

- Login con correo, contraseña, crear cuenta y explorar demostración.
- El botón de iniciar sesión cambia de gris a morado con animación al completar ambos campos.
- Errores de formulario con ícono y texto visible junto al campo.
- Colores y tipografía definidos en `Color.kt`, `Theme.kt` y `Type.kt`.
- Animaciones de entrada, selección, carga y confirmación.
- Tareas por proyecto; una tarea puede tener varios registros de tiempo.
- Cronómetro mostrado en horas/minutos, con redondeo hacia arriba al terminar.
- Perfil con cámara/galería y widget del escritorio.

El resultado actual de las pruebas y las limitaciones de ejecución están en [docs/PRUEBAS.md](../PRUEBAS.md). No se ha generado un APK release nuevo en este entorno.
