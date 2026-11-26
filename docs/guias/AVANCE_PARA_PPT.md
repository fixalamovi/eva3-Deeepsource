# Deepsource — contenido para presentar el avance

Esta página describe la versión 1.6 y su evidencia. Los cambios de acceso de la versión 1.7 están en [Acceso a Deepsource](../LOGIN_Y_CONEXION.md).

## Problema y solución

Un equipo informático necesita saber cuánto tiempo dedica a cada proyecto y tarea. Deepsource permite registrar horas desde Android, usar un cronómetro y consultar un resumen mensual.

## Implementación

- Kotlin, Jetpack Compose y Material 3; pantallas, ViewModels y repositorios separados.
- Dos microservicios Spring Boot con SQLite: proyectos/cuentas/tareas y registros/resúmenes.
- Tareas predefinidas o propias, con varios registros por tarea.
- Cronómetro con pausa y redondeo al minuto superior al terminar.
- CRUD de registros, CSV, perfil, cámara/galería y widget.
- Acceso por correo y demostración sin registrarse.

## Evidencia de pruebas

- 77 pruebas unitarias Android aprobadas; 15 pruebas backend aprobadas en esta verificación.
- Informe JaCoCo incluido: dominio 87,58 %; conjunto lógico ampliado 19,89 %.
- Fuentes instrumentadas compiladas, con ejecución en emulador pendiente.

## Entrega pendiente de ejecutar

La clave release y su configuración ya están preparadas. Falta generar e instalar el APK en Android Studio, integrar una API externa, completar el 80 % global de cobertura y adjuntar capturas y colaboración reales.

No presentar un porcentaje de avance o nota estimada. Mostrar lo implementado y explicar qué pruebas se ejecutaron realmente.
