# Matriz de entrega EVA 4

Fecha: 26 de noviembre de 2026. Esta matriz distingue código disponible, evidencia y comprobaciones pendientes. No estima una nota.

| Criterio | Peso de pauta | Evidencia actual | Pendiente |
| --- | ---: | --- | --- |
| Aplicación móvil completa | 15 % | Pantallas, formularios, horas, cronómetro, perfil, widget y demostración | Ejecutar todos los flujos en emulador; terminar edición/eliminación de proyectos y tareas en la UI si se incluyen en el alcance final |
| Microservicios Spring Boot y BD | 15 % | Dos servicios con SQLite real, endpoints y pruebas | Ejecutar ambos servicios por HTTP y demostrar persistencia después de reiniciar |
| CRUD móvil con servicios propios | 15 % | CRUD de registros y creación/consulta de proyectos/tareas por Retrofit | Comprobar el flujo conectado desde el APK instalado |
| API externa | 15 % | Google Identity incorporado para autenticación | Configurar OAuth y verificar el flujo en el emulador. Confirmar con el docente si el login cubre el criterio. |
| Pruebas y cobertura lógica ≥ 80 % | 15 % | 77 pruebas JVM Android; 15 backend; informe real incluido | Cobertura global medida 19,89 %; ampliar y ejecutar pruebas restantes |
| APK release firmado | 10 % | Clave propia, certificado, configuración y script de generación/verificación | Ejecutar Gradle en Android Studio, generar APK, instalar y adjuntar evidencia |
| Colaboración GitHub/Trello | 15 % | Campos preparados en README | Añadir enlaces reales y evidencias de contribuciones de ambos integrantes |

La documentación técnica está en [DOCUMENTACION_TECNICA.md](DOCUMENTACION_TECNICA.md); los requisitos de funcionamiento, pruebas y firma se explican en sus respectivas guías.

## Archivos y evidencias que deben acompañar la entrega

- Código Android y ambos microservicios, con sus archivos de configuración.
- README con integrantes, equipo, contexto, funciones, endpoints y pasos de ejecución.
- APK `serverRelease` generado y verificado, clave `.jks` y configuración de firma según la modalidad solicitada por el docente.
- Reporte de pruebas y cobertura regenerado sobre la versión final.
- Capturas reales de pantallas principales, errores de formulario, dos registros en una tarea, operaciones CRUD, API externa, pruebas y firma.
- Repositorio accesible al docente y tablero con tareas, responsables y evidencias reales de colaboración.
- Archivos y datos de identificación requeridos por AVA para la entrega grupal e individual.

No se han inventado capturas, porcentajes de avance, commits ni enlaces del equipo. Las capturas deben obtenerse después de ejecutar la versión final.

## Guion breve para comprobar la entrega

1. Explicar el problema y mostrar la separación entre pantallas, ViewModels, repositorios y servicios.
2. Levantar ambos servicios; revisar sus endpoints de salud y los archivos SQLite.
3. Ingresar con cuenta de servidor, crear proyecto/tarea y guardar dos registros de la misma tarea.
4. Editar y eliminar un registro; mostrar el cambio en historial y resumen.
5. Mostrar pausa/reanudación del cronómetro y explicar el redondeo a minutos.
6. Demostrar la API externa configurada y el resultado visible que exige la pauta.
7. Abrir el resultado de pruebas y el porcentaje global real de cobertura.
8. Verificar la firma del APK y mostrarlo instalado.

Ensayar este guion antes de la evaluación. Durante la defensa deben respetarse las condiciones de materiales de apoyo que indique el docente.
