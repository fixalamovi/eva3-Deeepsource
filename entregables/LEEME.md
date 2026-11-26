# APK pendiente de generar

Este ZIP no contiene un APK release nuevo. Se incluyeron la clave propia, su certificado y la configuración de firma en `firma/`.

Desde la terminal PowerShell del proyecto en Android Studio, ejecutar:

```powershell
.\scripts\generar-release.ps1
```

Al completarse la compilación y la verificación de firma, aparecerán en esta carpeta `Deepsource-server-release.apk`, `firma-server.txt` y `SHA256-server.txt`.

El motivo por el que no pudo generarse en el entorno de trabajo y los pasos completos están en [docs/APK_FIRMADO.md](../docs/APK_FIRMADO.md).
