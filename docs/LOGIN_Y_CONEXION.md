# Acceso a Deepsource

La variante `serverDebug` o `serverRelease` requiere los servicios Proyectos y Registros corriendo. La cuenta por correo se guarda en Proyectos. Después del alta, la app consulta ambos servicios para mostrar los datos iniciales. Si el alta fue confirmada y esa consulta falla, el formulario indica que la cuenta existe y que puedes entrar desde el login cuando se restablezca la conexión.

## Revisar el backend en Windows

1. Abre PowerShell en el proyecto extraído y ejecuta:

   ```powershell
   cd backend
   .\iniciar.ps1
   ```

2. Espera «Servicios listos». Si el script informa un error de Maven, Java o puertos, corrígelo antes de probar el registro.
3. Abre http://127.0.0.1:8081/api/health y http://127.0.0.1:8082/api/health en el PC. Ambas respuestas deben incluir `"ok": true`. Si un servicio no responde, revisa `backend/runtime/logs/`.
4. Usa el emulador de Android Studio con `serverDebug`. Las URL predeterminadas usan `10.0.2.2`, que apunta al PC anfitrión.
5. Para probar en un celular físico por USB, usa la configuración `adb reverse` de [backend/README.md](../backend/README.md) y recompila con las URLs de loopback indicadas allí.
6. Si quieres usar la app sin servidor en modo local, abre **Explorar demostración** o selecciona `localDebug` para crear una cuenta en el dispositivo.

## Habilitar Google sin Firebase

1. En Google Cloud, configura la pantalla de consentimiento. Si la aplicación está en modo de prueba, agrega la cuenta de Google que usarás a los usuarios de prueba.
2. Crea un cliente OAuth **Android** para `com.example.deepsource` y la huella **SHA-1** de la firma de la variante instalada. Para debug puedes consultar `app:signingReport`. Para release, revisa `firma/certificado.txt`.
3. En el mismo proyecto de Google Cloud, crea un cliente OAuth de tipo **Aplicación web**. Copia su **ID de cliente**. La app no usa el secreto de cliente.
4. En la terminal que inicia Spring Boot, configura el ID y arranca los servicios. Si ya estaban ejecutándose, detenlos primero con `detener.ps1`.

   ```powershell
   $env:GOOGLE_WEB_CLIENT_ID = 'TU_ID_DE_CLIENTE_WEB.apps.googleusercontent.com'
   .\iniciar.ps1
   ```

5. Usa `serverDebug` en un emulador con una imagen que incluya Google Play. La app solicita un desafío al backend, abre Credential Manager y envía el ID token a Spring Boot. El servidor verifica el token y emite la sesión de Deepsource. Las cuentas existentes por correo mantienen ese método de acceso.

El HTTP de loopback está previsto para desarrollo local. Para usar el acceso contra un servidor remoto, configura HTTPS.

- [Guía oficial de Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation)
- [Verificación de tokens en el servidor](https://developers.google.com/identity/sign-in/android/backend-auth)

## Estado de verificación

Los informes que se incluyen en `docs/evidencias/` corresponden a la versión 1.6. Las pruebas nuevas de acceso y el flujo de Google quedan por ejecutar con las dependencias de Gradle y Maven y con clientes OAuth configurados. La versión 1.7 requiere recompilar tanto el backend como Android. El ZIP contiene el código fuente y no incluye un APK nuevo.
