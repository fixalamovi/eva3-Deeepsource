# Tareas reutilizables y cronómetro por minutos

Esta página describe la versión 1.6 y su evidencia. Los cambios de acceso de la versión 1.7 están en [Acceso a Deepsource](../LOGIN_Y_CONEXION.md).

## Redondeo del cronómetro

El contador muestra horas y minutos. Al finalizar, el tiempo activo se redondea hacia arriba al siguiente minuto completo:

| Tiempo activo | Tiempo registrado |
| --- | --- |
| 1 segundo | 1 minuto |
| 30 segundos | 1 minuto |
| 1 minuto exacto | 1 minuto |
| 1 h 33 min exactos | 1 h 33 min |
| 1 h 33 min 30 s | 1 h 34 min |

Las pausas no suman tiempo. Los milisegundos se conservan internamente para sumar los tramos activos y aplicar un solo redondeo final. En pausa, el contador muestra el tiempo que se registraría al guardar.

El límite diario de 24 horas se comprueba después del redondeo. Si se supera, el registro se rechaza y el cronómetro permanece disponible. Los registros históricos no se redondean retroactivamente.

El widget también muestra horas y minutos. Su actualización en segundo plano depende de la planificación y el ahorro de batería de Android; el cálculo del tiempo guardado usa siempre el tiempo activo real.

## Tareas listas para utilizar

Cada proyecto incluye **Planificación**, **Desarrollo**, **Pruebas** y **Documentación**. También puedes crear tareas propias, incluso antes de registrar tiempo.

En **Inicio → Tu proyecto → Tareas del proyecto**:

1. Pulsa **Crear tarea** para guardar otra tarea.
2. Pulsa **Registrar** en una tarea para agregarle un registro manual.
3. Usa el icono de cronómetro para comenzar una sesión con esa tarea seleccionada.
4. Pulsa **Registros** para ver sus registros y su tiempo acumulado del mes seleccionado.
5. Repite el registro las veces que necesites: cada registro conserva su fecha, duración y origen, y se relaciona con la misma tarea.

También puedes seleccionar o crear tareas desde **Registrar horas** y **Cronómetro**. Al cambiar de proyecto, se limpia la selección de tarea para evitar asociarla a otro proyecto.

Crear una tarea con un nombre ya existente en el proyecto reutiliza la misma tarea. Eliminar un registro conserva la tarea y sus demás registros.

## Datos existentes

La base SQLite del dispositivo pasa a versión 3: la versión 2 incorporó tareas y la versión 3 agrega caché remota y claves persistentes del cronómetro. La migración agrega las tareas y sus relaciones sin borrar proyectos, registros ni cronómetros. Las descripciones de tareas antiguas se convierten en tareas reutilizables por proyecto. Las duraciones históricas permanecen iguales.

Actualiza la instalación existente con el mismo identificador de aplicación y firma para conservar esos datos.

## Verificación

El corte actual tiene 77 pruebas unitarias Android aprobadas. Las fuentes instrumentadas compilan, pero no se ejecutaron en dispositivo. Los resultados y el informe de cobertura están en [PRUEBAS.md](../PRUEBAS.md).

Para ejecutar desde la raíz del proyecto:

```powershell
.\scripts\probar.ps1
.\scripts\probar.ps1 -Dispositivo
```

## Registros conectados al servidor

serverDebug envía los registros a Spring Boot, que valida sesión, tarea, fecha, redondeo y límite diario. La demostración y localDebug conservan el flujo local. Consulta [backend/README.md](../../backend/README.md).
