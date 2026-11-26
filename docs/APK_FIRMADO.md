# Firma release y generación del APK

## Estado de esta entrega

Se generó una clave release propia de Deepsource, se exportó su certificado y se preparó `signingConfigs.release` en `app/build.gradle.kts`. **Este ZIP todavía no contiene un APK release nuevo.**

El intento de Gradle quedó bloqueado por las restricciones del entorno: primero acceso denegado al bloqueo de caché y luego imposibilidad de conectar con su daemon local. El registro está en [bloqueo-gradle.txt](evidencias/bloqueo-gradle.txt). La compilación directa de Kotlin y las pruebas no sustituyen el empaquetado Android.

## Archivos de firma incluidos

| Archivo | Uso |
| --- | --- |
| `firma/deepsource-eva4-release.jks` | Clave privada release de este proyecto. |
| `firma/keystore.properties` | Ruta, alias y contraseñas para la compilación de esta entrega. |
| `firma/release-cert.pem` | Certificado público. |
| `firma/certificado.txt` | Sujeto, validez y huellas del certificado. |
| `firma/keystore.properties.example` | Plantilla sin contraseñas reales. |

El alias es `deepsource-eva4`. Las contraseñas se consultan en el archivo privado, no en el README. El `.jks`, las propiedades reales y los APK están excluidos de Git mediante `.gitignore`; se incluyen en el ZIP privado de entrega. Conserva una copia de la clave para firmar futuras actualizaciones.

Huella SHA-256 del certificado generado:

```text
3A:3E:CE:87:59:36:0A:FC:22:72:7A:11:71:C5:36:B6:84:55:07:55:74:FC:E7:8D:C9:BD:91:22:F1:8C:AB:CC
```

Es la huella del certificado, no el SHA-256 de un APK. Las demás huellas se encuentran en `firma/certificado.txt`.

## Generación automática en el equipo de presentación

1. Extrae el ZIP en una carpeta donde puedas escribir.
2. Abre el proyecto en Android Studio, instala SDK 37 y sincroniza Gradle con internet.
3. Comprueba que el JDK de Gradle corresponde al solicitado por `gradle/gradle-daemon-jvm.properties` (Java 25).
4. Abre la terminal PowerShell desde la raíz y ejecuta:

```powershell
.\scripts\generar-release.ps1
```

El script ejecuta las pruebas unitarias, genera el reporte de cobertura, compila `serverRelease`, verifica criptográficamente la firma con `apksigner` y copia el APK a `entregables/Deepsource-server-release.apk`. También crea `firma-server.txt` y `SHA256-server.txt`.

El script exige que esas operaciones terminen correctamente antes de anunciar un APK generado. Su flujo completo permanece pendiente de ejecución en un entorno con Gradle operativo. El control opcional del 80 % se ejecuta por separado con `probar.ps1 -Exigir80`.

Para una variante de revisión con datos locales:

```powershell
.\scripts\generar-release.ps1 -Modo local
```

La variante `serverRelease` es la que permite demostrar los microservicios propios. Su demostración local también está disponible desde el login.

## Paso a paso desde Android Studio

1. Descarga y extrae **todo el ZIP**. En Android Studio entra a **File > Open** y selecciona `Deepsource_eva4`, la carpeta que contiene `settings.gradle.kts`. No selecciones solamente `app`.
2. Espera a que termine **Gradle Sync**. Si solicita instalar SDK 37, acepta su instalación desde **SDK Manager**. En **Settings > Build, Execution, Deployment > Build Tools > Gradle**, utiliza el JDK integrado compatible con Java 25 que solicita este proyecto.
3. Abre **Build > Generate Signed App Bundle or APK**. Si el menú aparece deshabilitado, espera a que termine la sincronización o resuelve el error que muestre Gradle.
4. Selecciona **APK** y pulsa **Next**. No elijas Android App Bundle para esta entrega.
5. En **Key store path**, pulsa el selector de archivo y abre `firma/deepsource-eva4-release.jks`. La clave ya está creada; no necesitas usar **Create new**.
6. Abre `firma/keystore.properties` como texto dentro de Android Studio. Copia el valor de `STORE_PASSWORD` a **Key store password**. En **Key alias**, usa `deepsource-eva4`. Copia `KEY_PASSWORD` a **Key password**. Copia solo lo que está después del signo `=`.
7. Pulsa **Next** y elige la variante **serverRelease**. Es la que conecta la app con Spring Boot. `localRelease` sirve para una revisión local; `debug` no es la variante de entrega release.
8. Mantén el destino que indique el asistente y pulsa **Create** o **Finish**, según la versión de Android Studio. Espera el mensaje de compilación terminada; una compilación que falla no produce una entrega nueva válida.
9. Pulsa **Locate** en la notificación para abrir la carpeta del APK. Guarda ese archivo en `entregables/` con un nombre que identifique la variante, por ejemplo `Deepsource-server-release.apk`.
10. Comprueba la firma con `apksigner verify --verbose --print-certs <ruta-del-apk>`. La alternativa automática de `scripts/generar-release.ps1` realiza esta comprobación y genera también la huella SHA-256 del archivo.

Para probar el APK conectado, inicia los dos microservicios en el mismo equipo y utiliza el emulador. Generar el APK no inicia el backend. También puedes abrir **Explorar demostración** sin una cuenta.

Por terminal, la salida estándar de `assembleServerRelease` es `app/build/outputs/apk/server/release/app-server-release.apk`.

## Comprobación antes de entregar

- Comparar la huella que muestra `apksigner` con la del certificado incluido.
- Instalar la release en el emulador e iniciar sesión con los dos microservicios activos.
- Ejecutar los casos A01–A10 de [PRUEBAS.md](PRUEBAS.md), según corresponda.
- Incorporar capturas reales del APK instalado y de la verificación de firma.

Si existe una instalación anterior firmada con otra clave, Android puede rechazar la actualización. Para conservar sus datos utiliza un emulador aparte; desinstalar elimina los datos locales de esa instalación.

## Conexión con el backend

La configuración por defecto utiliza `10.0.2.2:8081` y `10.0.2.2:8082`, accesibles desde un emulador en el mismo equipo que ejecuta Spring Boot. Un celular físico requiere ajustar la conexión; la alternativa USB con `adb reverse` está descrita en [backend/README.md](../backend/README.md).


## Referencias oficiales

- [Compilar y firmar desde terminal](https://developer.android.com/build/building-cmdline).
- [Configuración de variantes y firma](https://developer.android.com/build/build-variants).
- [Reportes de cobertura](https://developer.android.com/studio/test/coverage-report).
