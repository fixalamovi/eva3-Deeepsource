# Backend de Deepsource con Spring Boot y SQLite

Se incluyen dos aplicaciones Spring Boot independientes. Ambas usan SQLite mediante JDBC y `JdbcTemplate`, con archivos persistentes y tablas creadas automáticamente mediante `schema.sql`.

| Servicio | Puerto | Archivo al ejecutar desde backend | Responsabilidad |
| --- | --- | --- | --- |
| proyectos-service | 8081 | data/proyectos.db | Cuentas, sesiones, proyectos y tareas. |
| registros-service | 8082 | data/registros.db | Registros de horas, validaciones y resumen mensual. |

El servicio de registros consulta al de proyectos para verificar la sesión y la tarea. El usuario se obtiene de la sesión del servidor; la API no acepta un UID del cliente como autenticación. Cada tarea puede tener varios registros.

## Ejecutar en Windows

Requisitos: JDK 17 o posterior y Maven 3.6 o posterior. La primera compilación puede necesitar internet para descargar dependencias y plugins de Maven.

Desde PowerShell, dentro de `backend`:

```powershell
.\iniciar.ps1
```

El script compila, ejecuta pruebas e inicia ambos servicios en segundo plano. Los logs quedan en `backend/runtime/logs`. Si Maven no está en PATH, busca una instalación descargada previamente por Maven Wrapper.

Para detenerlos conservando las bases de datos:

```powershell
.\detener.ps1
```

También puedes abrir `backend/pom.xml` en IntelliJ, ejecutar `package` y arrancar cada clase `Application`. Desde terminal, después de compilar:

```powershell
java -jar proyectos-service/target/proyectos-service-1.0.0.jar
java -jar registros-service/target/registros-service-1.0.0.jar
```

Ejecuta cada comando en una terminal distinta, ambas situadas en `backend`.

## Conectar Android

1. Inicia ambos servicios.
2. En Android Studio abre la carpeta raíz de Deepsource y sincroniza Gradle.
3. Selecciona la variante **serverDebug**, que se conecta al backend.
4. Ejecuta en un emulador Android. La dirección `10.0.2.2` permite llegar al equipo anfitrión.
5. Crea una cuenta de servidor desde la app. Las cuentas locales antiguas siguen disponibles en la variante **localDebug**.
6. Crea un proyecto, selecciona una tarea y registra horas. Al volver a consultar, los datos proceden del backend.

Las URLs predeterminadas son:

```properties
PROJECTS_API_URL=http://10.0.2.2:8081/
RECORDS_API_URL=http://10.0.2.2:8082/
```

Puedes cambiar estos valores en el `gradle.properties` del proyecto. Conserva la barra final.

Para probar en un celular físico con USB, una alternativa es:

```powershell
adb reverse tcp:8081 tcp:8081
adb reverse tcp:8082 tcp:8082
```

En ese caso usa `http://127.0.0.1:8081/` y `http://127.0.0.1:8082/` en las propiedades y recompila. La configuración Android permite HTTP solo para el emulador y las direcciones locales indicadas; los servidores escuchan en localhost por defecto.

## Modo de prueba y caché

**Explorar demostración** sigue utilizando SQLite local y no llama a los servicios. La variante **localDebug** conserva el comportamiento local anterior.

En **serverDebug**, las cuentas y datos principales se guardan en el servidor. El cronómetro activo y las fotos permanecen en el dispositivo. Los datos del servidor se copian a una caché local para el widget; esa caché no sustituye una operación remota fallida ni mezcla registros de demostración.

Al terminar el cronómetro se envía el tiempo activo redondeado hacia arriba al minuto completo. Una clave persistente por sesión evita crear dos registros si se repite la petición. El cronómetro se elimina del dispositivo después de recibir la confirmación del servidor.

Consulta [Acceso a Deepsource](../docs/LOGIN_Y_CONEXION.md) para configurar Google Identity.

## Endpoints

Los endpoints de datos requieren `Authorization: Bearer <token>`. El token se obtiene al registrar o iniciar sesión. Las contraseñas se derivan con PBKDF2 y las sesiones tienen caducidad.

| Servicio | Método y ruta | Función |
| --- | --- | --- |
| Proyectos | GET /api/health | Comprobar servicio y SQLite. |
| Proyectos | POST /api/auth/register | Crear cuenta: name, email, password. |
| Proyectos | POST /api/auth/login | Iniciar sesión: email, password. |
| Proyectos | POST /api/auth/google/challenge | Entregar ID OAuth web y desafío temporal si Google está configurado. |
| Proyectos | POST /api/auth/google | Verificar el token de Google y emitir una sesión propia. |
| Proyectos | GET /api/auth/session | Consultar la identidad autenticada. |
| Proyectos | POST /api/auth/logout | Revocar la sesión actual. |
| Proyectos | PUT /api/auth/profile | Actualizar name. |
| Proyectos | PUT /api/auth/password | Cambiar current y next. |
| Proyectos | GET /api/proyectos | Listar proyectos propios. |
| Proyectos | POST /api/proyectos | Crear: name, client, estimateMinutes. |
| Proyectos | GET /api/proyectos/{id} | Consultar proyecto. |
| Proyectos | PUT /api/proyectos/{id} | Actualizar proyecto. |
| Proyectos | DELETE /api/proyectos/{id} | Eliminar si no tiene registros. |
| Proyectos | GET /api/tareas | Listar tareas propias. |
| Proyectos | GET /api/proyectos/{id}/tareas | Listar tareas del proyecto. |
| Proyectos | POST /api/proyectos/{id}/tareas | Crear tarea: name. |
| Proyectos | GET /api/tareas/{id} | Consultar tarea. |
| Proyectos | PUT /api/tareas/{id} | Actualizar name. |
| Proyectos | DELETE /api/tareas/{id} | Eliminar si no tiene registros. |
| Registros | GET /api/health | Comprobar servicio y SQLite. |
| Registros | GET /api/registros | Listar registros propios. |
| Registros | POST /api/registros | Crear registro. Acepta cabecera Idempotency-Key. |
| Registros | GET /api/registros/{id} | Consultar registro propio. |
| Registros | PUT /api/registros/{id} | Actualizar registro propio. |
| Registros | DELETE /api/registros/{id} | Eliminar registro propio. |
| Registros | GET /api/resumen?mes=2026-11 | Resumen mensual y totales por tarea. |
| Registros | GET /api/registros/referencias?proyecto={id}&tarea={id} | Comprobar registros asociados. |

Cuerpo para crear o actualizar un registro:

```json
{
  "projectId": "identificador-del-proyecto",
  "taskId": "identificador-de-la-tarea",
  "day": "2026-11-26",
  "seconds": 5610,
  "source": "Cronómetro"
}
```

En ese ejemplo el servidor guarda 5640 segundos, equivalentes a 1 h 34 min. También comprueba pertenencia de la tarea, fecha válida y límite diario de 24 horas.

## Pruebas

```powershell
mvn test
```

Se incluyen pruebas de API con MockMvc y SQLite en archivos temporales, y pruebas de contrato HTTP entre los servicios. Cubren autenticación, aislamiento por usuario, CRUD, tareas predefinidas, múltiples registros por tarea, redondeo, límite diario, reintentos y referencias antes de eliminar.

La cantidad de pruebas aprobadas no demuestra por sí sola el 80 % de cobertura exigido por la EVA 4. Ese informe y las pruebas instrumentadas de Android deben completarse por separado.
