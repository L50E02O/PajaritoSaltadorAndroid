# Guia: actualizar la Release en GitHub con binarios (APK)

GitHub **no genera una Release** al hacer `git push`. Las Releases son **publicaciones manuales** (o con herramientas) que asocian un **tag** de version, notas y archivos adjuntos, por ejemplo el APK.

## Checklist antes de publicar

1. **Sube el codigo** a la rama deseada (por ejemplo `master`) con los cambios ya commiteados.
2. **Sube la version** en `app/build.gradle` dentro de `defaultConfig`:
   - `versionCode`: entero que **siempre debe aumentar** en cada subida a Play o distribucion seria (Play Console lo exige).
   - `versionName`: texto visible para usuarios, por ejemplo `1.2.0`.
3. **Compila el APK release firmado** (ver seccion siguiente).
4. **Crea el tag** y la **Release** en GitHub con el APK adjunto.

## Compilar el APK release (firmado)

### Requisitos

- JDK 17 (Android Studio incluye un JBR valido).
- `keystore.properties` en la **raiz del proyecto** (no versionar; usar `keystore.properties.example` como plantilla).
- Archivo `.jks` en la ruta indicada en `keystore.properties`.

### Comando (Windows, desde la raiz del repo)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease --no-daemon
```

En Linux/macOS:

```bash
./gradlew assembleRelease
```

### Donde esta el artefacto

Ruta tipica:

`app/build/outputs/apk/release/app-release.apk`

Opcional: copia el APK a `releases/` con nombre claro, por ejemplo `pajarito-saltador-1.2.0-release.apk`, y commitea solo si el equipo versiona binarios en el repo (muchas veces **no** se commitea el APK; solo se adjunta en GitHub Releases).

## Crear o actualizar una Release en GitHub (interfaz web)

1. Abre el repositorio en GitHub.
2. **Releases** (lateral derecho o pestana del repo) → **Create a new release** (o **Draft a new release**).
3. **Choose tag**: crea un tag nuevo, por ejemplo `v1.2.0`, apuntando al commit actual de `master` (o la rama estable).
4. **Release title**: por ejemplo `v1.2.0`.
5. Describe cambios en el cuerpo (notas de version).
6. **Attach binaries**: arrastra el APK (`app-release.apk` o el que hayas renombrado).
7. Publica con **Publish release**.

Para **actualizar** una release ya existente:

- GitHub no permite reemplazar archivos de una release de forma tan flexible como un sistema de CI. Lo habitual es:
  - **Publicar una release nueva** con tag nuevo (`v1.2.1`) y el APK nuevo, o
  - Editar la release, **borrar el adjunto viejo** y **subir el APK nuevo** (si la UI lo permite en tu rol).

Lo mas limpio para usuarios: **nueva version = nuevo tag + nueva release** (historial claro).

## Crear la Release desde la linea de comandos (GitHub CLI)

Instalacion: [https://cli.github.com/](https://cli.github.com/)

Autenticacion una vez: `gh auth login`

### Crear tag local y subirlo

```bash
git tag -a v1.2.0 -m "Release 1.2.0"
git push origin v1.2.0
```

### Crear la release con APK adjunto

```bash
gh release create v1.2.0 "app/build/outputs/apk/release/app-release.apk" ^
  --title "v1.2.0" ^
  --notes "Cambios: describe aqui."
```

En PowerShell puedes usar comillas y rutas completas al APK. Ajusta la ruta si copiaste el archivo a `releases/`.

### Listar releases

```bash
gh release list
```

## Buenas practicas

- **Un tag por version** que coincida con `versionName` (por ejemplo tag `v1.2.0` y `versionName "1.2.0"`).
- En las notas: que se arreglo, para quien prueba (QA), y si hay migraciones de datos.
- No subas `keystore.properties` ni el `.jks` al repositorio publico.

## Referencia rapida de version en Gradle

Archivo: `app/build.gradle`

```gradle
defaultConfig {
    versionCode 3
    versionName "1.2.0"
}
```

Tras cambiar version, vuelve a ejecutar `assembleRelease` antes de adjuntar el APK en GitHub.
