# Pruebas y cobertura de Deepsource

Esta página describe la versión 1.6 y su evidencia. Los cambios de acceso de la versión 1.7 están en [Acceso a Deepsource](LOGIN_Y_CONEXION.md).

Fecha del corte: 26 de noviembre de 2026. Se utiliza JUnit 4 para pruebas JVM de Android y JUnit 5/MockMvc para Spring Boot.

## Resultados disponibles

| Conjunto | Resultado | Evidencia |
| --- | --- | --- |
| Android JVM | 77 pruebas aprobadas, cero fallos | [pruebas-android.txt](evidencias/pruebas-android.txt) |
| Backend | 15 pruebas aprobadas en esta verificación | [pruebas-backend.json](evidencias/pruebas-backend.json) |
| Instrumentadas | Fuentes compiladas; no ejecutadas en dispositivo en este corte | `app/src/androidTest` |

En este entorno se compilaron las fuentes Kotlin con el compilador 2.2.10 y su plugin Compose, utilizando las dependencias instaladas. Se ejecutó JUnit directamente sobre esas clases. Esta comprobación no equivale a un `assemble` de Gradle ni genera un APK. El backend también se comprobó después de retirar el acceso externo; sus 15 pruebas volvieron a aprobarse.

## Qué revisan las pruebas Android

| Clase | Casos principales |
| --- | --- |
| `DomainTest` | Duraciones, límites, pausas, reinicios, totales y desviaciones. |
| `FormValidationTest` | Errores de proyecto, tarea y cambio de contraseña. |
| `LocalPrototypeTest` | Contraseñas derivadas, sales independientes y espacio del widget. |
| `TimerTasksTest` | Redondeo por minutos y agrupación de registros por tarea. |
| `LoginViewModelTest` | Acceso, registro, correo duplicado, demostración y cancelación. |
| `RemoteHoursRepositoryTest` | Lectura, POST/PUT/DELETE, autorización, IDs, clave de reintento y unidades. |
| `BackendResponseTest` | HTTP, mensajes JSON, respuestas vacías, red y cancelación. |
| `BusinessRulesTest` | Bordes de 24 horas, tareas compartidas, fechas, CSV y validaciones. |
| `ExampleUnitTest` | Prueba de plantilla original; no acredita por sí sola lógica funcional. |

Los dobles de las APIs devuelven respuestas controladas y registran las llamadas. Las pruebas del login sustituyen `LoginActions`. Esto verifica decisiones de los ViewModels/repositorios, pero no acredita que los servidores estén accesibles.

## Medición real de cobertura

Informe navegable: [cobertura-logica/index.html](evidencias/cobertura-logica/index.html). Datos: [jacoco.xml](evidencias/cobertura-logica/jacoco.xml).

| Alcance | Líneas cubiertas / totales | Cobertura |
| --- | ---: | ---: |
| `domain` | 134 / 153 | 87,58 % |
| Dominio, datos, ViewModels y `AppState` | 208 / 1.046 | 19,89 % |

La medición incluida utiliza JaCoCo 0.8.11 sobre las clases compiladas directamente y cuenta también las clases de esas capas que no se ejecutaron. Excluye Composables, widget y código generado de configuración. No se seleccionaron solo clases probadas para declarar el 80 % global. Los filtros de Kotlin de otras versiones de JaCoCo pueden producir diferencias al regenerar el informe con Gradle.

**El mínimo del 80 % global todavía no está cumplido.** La mayor deuda está en los demás ViewModels, `AppState`, el repositorio local/remoto combinado, autenticación local y los adaptadores Android de SQLite, fotos y sesión.

Para completar la cobertura se necesitan pruebas de las acciones de esos ViewModels y repositorios, con dependencias sustituibles, además de ejecutar las pruebas instrumentadas que usan Android real. No basta con aumentar la cantidad de aserciones de una función ya cubierta.

## Ejecutar en Android Studio

Desde la raíz del proyecto, con el JDK de Android Studio y dependencias descargadas:

```powershell
.\scripts\probar.ps1
```

Equivalente mediante Gradle:

```powershell
.\gradlew.bat :app:testServerDebugUnitTest :app:logicServerDebugCoverage
```

La configuración de `debug` habilita cobertura. La tarea `logicServerDebugCoverage` mantiene el mismo alcance amplio por capas y deja HTML/XML en `app/build/reports/coverage/logic/serverDebug`. El reporte completo de la variante se puede generar con `:app:createServerDebugUnitTestCoverageReport`.

Para comprobar el mínimo sobre un informe nuevo:

```powershell
.\scripts\probar.ps1 -Exigir80
```

La comprobación devuelve error cuando el porcentaje global de líneas es inferior al mínimo. Para evaluar solo el informe guardado en este ZIP:

```powershell
.\scripts\comprobar-cobertura.ps1
```

Con el corte incluido, el resultado esperado de este último comando es **19,89 % y comprobación rechazada**. La condición del 80 % no se usa para impedir que se genere un APK de revisión.

## Pruebas instrumentadas

Con un dispositivo o emulador conectado:

```powershell
.\scripts\probar.ps1 -Dispositivo
```

También puede utilizarse `:app:connectedServerDebugAndroidTest`. Las fuentes incluyen formularios, opciones de login, cuentas locales, fotos, SQLite y migración/separación de IDs de caché. Las pruebas Compose pueden reemplazar la actividad para aislar una pantalla; no son una demostración de la aplicación conectada completa.

## Backend

Dentro de `backend`, ejecutar `mvn test`. Se prueban controladores y servicios con archivos SQLite temporales. MockMvc no abre un puerto real; las comunicaciones con el otro servicio se sustituyen. `ProjectsGatewayTest` comprueba aparte el contrato HTTP mediante `MockRestServiceServer`.

| Suite | Pruebas |
| --- | ---: |
| `ProjectsApiTest` | 6 |
| `RecordsApiTest` | 6 |
| `ProjectsGatewayTest` | 3 |

## Aceptación manual pendiente

| Caso | Acción | Resultado esperado | Estado |
| --- | --- | --- | --- |
| A01 | Abrir demostración sin servidor | Entra sin registrarse | Por ejecutar en este corte |
| A02 | Crear cuenta de servidor y volver a ingresar | Conserva identidad y proyectos | Por ejecutar |
| A03 | Guardar dos registros de la misma tarea | Ambos aparecen y suman correctamente | Por ejecutar |
| A04 | Editar y eliminar un registro | Se actualizan historial y resumen | Por ejecutar |
| A05 | Pausar/reanudar cronómetro | La pausa no se suma | Por ejecutar |
| A06 | Reintentar un guardado tras fallo de red | Se conserva la clave y no se duplica | Por ejecutar |
| A07 | Reiniciar app y backend | Se conservan los datos confirmados | Por ejecutar |
| A08 | Cámara, galería y widget | Muestran datos de la cuenta activa | Por ejecutar |
| A09 | Integrar una API externa | Resultado visible y coherente en la app | Pendiente de implementar |
| A10 | Instalar y ejecutar release | Firma válida y flujo principal operativo | Requiere generar APK |

Estas filas deben marcarse aprobadas únicamente después de ejecutarlas y adjuntar la evidencia real.
