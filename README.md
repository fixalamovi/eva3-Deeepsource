# Deepsource

Aplicación Android en Kotlin y Jetpack Compose para organizar proyectos, tareas y registros de horas de trabajo. El contexto simulado es un equipo informático que necesita consultar cuánto tiempo dedica a cada tarea y compararlo con las estimaciones del proyecto.

## Versión 1.7

El acceso por Google se implementó con Google Identity y Spring Boot. El flujo necesita configurar OAuth y comprobarse en un emulador con Google Play. Hay una animación más visible en el login y mensajes de registro que distinguen la conexión de la carga inicial. Sigue la [guía de login y conexión](docs/LOGIN_Y_CONEXION.md). Las evidencias anteriores corresponden a la versión 1.6.

## Por dónde empezar

1. Abre esta carpeta en Android Studio: contiene `settings.gradle.kts`.
2. Para ejecutar la app, consulta [docs/GUIA_USO.md](docs/GUIA_USO.md).
3. Para obtener el APK firmado, sigue [docs/APK_FIRMADO.md](docs/APK_FIRMADO.md).

| Carpeta | Contenido |
| --- | --- |
| `app/` | Código Android: pantallas, componentes, tema, ViewModels, datos y pruebas. |
| `backend/` | Los dos microservicios Spring Boot y sus scripts de inicio. |
| `docs/` | Guías, documentación técnica y evidencias de pruebas. |
| `scripts/` | Comandos para probar, comprobar cobertura y generar release. |
| `firma/` | Clave release propia, certificado y propiedades privadas de firma. |
| `entregables/` | Destino del APK y comprobantes cuando se genere. |
| `gradle/` | Gradle Wrapper y criterios del JDK. |

El ZIP conserva los archivos Gradle en la raíz para que Android Studio pueda abrir el proyecto directamente.

## Integrantes

- Vicente Jose Duran Leal.
- Segundo integrante y número de equipo: pendientes de completar.
- Repositorio GitHub y tablero Trello: pendientes de incorporar.

## Arquitectura actual

| Componente | Función |
| --- | --- |
| App Android | Interfaz, validaciones, cronómetro por minutos, fotos y widget. |
| Proyectos-service, puerto 8081 | Cuentas, sesiones, proyectos y tareas, con SQLite persistente. |
| Registros-service, puerto 8082 | CRUD de horas, validaciones y resumen, con SQLite persistente. |
| Retrofit | Conecta Android con ambos microservicios propios. |
| SQLite del dispositivo | Demostración, variante local, cronómetro activo y caché del widget. |

## Ejecutar la versión conectada al servidor

1. Extrae el ZIP completo. Abre en Android Studio la carpeta que contiene `settings.gradle.kts`.
2. En una terminal PowerShell, entra en `backend` y ejecuta `.\iniciar.ps1`. Necesitas JDK 17 o posterior y Maven; la primera compilación requiere internet para las dependencias.
3. Comprueba que ambos servicios estén listos. El detalle de instalación, comandos y endpoints está en [backend/README.md](backend/README.md).
4. Sincroniza Gradle y selecciona **serverDebug** en Android Studio.
5. Ejecuta en un emulador. Las URLs predeterminadas apuntan a `10.0.2.2:8081` y `10.0.2.2:8082`.
6. Crea una cuenta de servidor. Crea un proyecto, selecciona una tarea y registra horas. Los datos se consultan y guardan por API.

Android requiere SDK 37 y el JDK integrado compatible con el archivo de criterios de Gradle. El backend requiere Java 17 o posterior.

Las cuentas locales antiguas siguen disponibles en **localDebug**. **Explorar demostración** funciona en ambas variantes sin registrar una cuenta ni iniciar el backend.

Para detener los servicios conservando sus bases SQLite, ejecuta `backend/detener.ps1`.

## Funcionalidades

- Acceso por correo y contraseña y modo de demostración sin cuenta.
- Proyectos con estimaciones y tareas predefinidas o propias.
- Una tarea admite varios registros, con fecha y duración independientes.
- Registro manual, edición, eliminación, búsqueda y exportación CSV.
- Cronómetro en horas/minutos: 1 h 33 min 30 s se guarda como 1 h 34 min. Las pausas no suman tiempo.
- Resumen mensual y registros agrupados por tarea.
- Perfil, cámara/galería y widget de Android.
- Caché separada de los datos de prueba y claves para evitar duplicar un registro del cronómetro al reintentar.

## Endpoints y documentación

- [Backend, bases SQLite y todos los endpoints](backend/README.md).
- [Tareas y redondeo a minutos](docs/guias/TAREAS_Y_MINUTOS.md).
- [Cambios de interfaz](docs/guias/CAMBIOS_UI.md).
- [Documentación técnica](docs/DOCUMENTACION_TECNICA.md).
- [Pruebas y cobertura real](docs/PRUEBAS.md).
- [Firma release y generación del APK](docs/APK_FIRMADO.md).
- [Matriz de entrega EVA 4](docs/ENTREGA_EVA4.md).
- [Validación y pendientes](docs/VALIDACION_BACKEND.md).

Google Identity es la integración externa de autenticación. Falta configurarla y demostrarla. Confirma con el docente si el login cubre el criterio de API externa de la EVA 4.

## Pruebas y firma release

```powershell
.\scripts\probar.ps1
.\scripts\generar-release.ps1
```

El primer script ejecuta pruebas y genera el informe de cobertura. El segundo también compila la variante release y verifica su firma. Deben ejecutarse en Android Studio con Gradle operativo; su flujo completo no pudo comprobarse en este entorno.

La clave propia y sus propiedades están en `firma/` y se excluyen de Git. Las instrucciones están en [docs/APK_FIRMADO.md](docs/APK_FIRMADO.md). El ZIP aún no incluye un APK nuevo.

## Verificación adicional

```powershell
cd backend
mvn test
cd ..
.\gradlew.bat :app:assembleServerDebug :app:testServerDebugUnitTest
.\gradlew.bat :app:connectedServerDebugAndroidTest
```

La variante local se comprueba con `assembleLocalDebug`, `testLocalDebugUnitTest` y `connectedLocalDebugAndroidTest`.

## Pendientes de la entrega EVA 4

- Probar la app en emulador/dispositivo con los dos servicios en ejecución.
- Configurar y comprobar Google Identity. Confirmar si el criterio de API externa requiere además una integración de datos.
- Ampliar la cobertura lógica: se midió 19,89 % global y 87,58 % en dominio; el mínimo global del 80 % sigue pendiente.
- Ejecutar la generación y comprobación del APK release firmado e incorporar capturas reales. La llave y su configuración ya están incluidas.
- Completar integrantes, GitHub y Trello con evidencias reales de colaboración.

El ZIP contiene el código de la app y ambos servicios, evidencia de 77 pruebas unitarias Android y 15 pruebas backend en la versión 1.6, informe HTML/XML de cobertura, documentación técnica y una clave release propia. No contiene un APK firmado nuevo: Gradle quedó bloqueado por restricciones del entorno. Véase [docs/PRUEBAS.md](docs/PRUEBAS.md) para conocer el alcance de la compilación y de las comprobaciones.
