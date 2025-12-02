# Guía para Publicar tu App en Google Play Store

## 📋 Requisitos Previos

### 1. Cuenta de Desarrollador de Google Play
- **Costo**: $25 USD (pago único de por vida)
- **Registro**: https://play.google.com/console/signup
- Necesitas una cuenta de Google y una tarjeta de crédito/débito

### 2. Preparar la App para Producción

#### A. Generar APK Firmado (Release)

1. **Crear un Keystore** (si no lo tienes):
   ```bash
   keytool -genkey -v -keystore pajarito-saltador-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pajarito-saltador
   ```
   - Guarda la contraseña y la información de forma segura
   - **IMPORTANTE**: Si pierdes el keystore, no podrás actualizar tu app

2. **Configurar el build.gradle** (app/build.gradle):
   ```gradle
   android {
       ...
       signingConfigs {
           release {
               storeFile file('path/to/pajarito-saltador-key.jks')
               storePassword 'tu-contraseña'
               keyAlias 'pajarito-saltador'
               keyPassword 'tu-contraseña'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled true
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```

3. **Generar el APK/AAB**:
   - En Android Studio: `Build > Generate Signed Bundle / APK`
   - Selecciona "Android App Bundle" (recomendado) o "APK"
   - Selecciona el keystore y completa la información
   - El archivo se generará en `app/release/`

#### B. Verificar Información de la App

1. **AndroidManifest.xml**:
   - ✅ Nombre de la app
   - ✅ Versión (versionCode y versionName)
   - ✅ Permisos necesarios
   - ✅ Icono de la app

2. **build.gradle** (app/build.gradle):
   ```gradle
   android {
       defaultConfig {
           applicationId "com.pajaritosaltador.game"
           minSdkVersion 21  // Android 5.0+
           targetSdkVersion 34  // Última versión
           versionCode 1  // Incrementar con cada actualización
           versionName "1.0.0"
       }
   }
   ```

## 🚀 Proceso de Publicación

### Paso 1: Acceder a Google Play Console
1. Ve a https://play.google.com/console
2. Inicia sesión con tu cuenta de desarrollador

### Paso 2: Crear una Nueva App
1. Click en "Crear aplicación"
2. Completa:
   - **Nombre de la app**: "Pájaro Saltador" (o el que prefieras)
   - **Idioma predeterminado**: Español
   - **Tipo de app**: Aplicación
   - **Gratis o de pago**: Gratis (o de pago si quieres cobrar)

### Paso 3: Configurar la Tienda

#### A. Información Principal
- **Nombre de la app**: Máximo 50 caracteres
- **Descripción corta**: Máximo 80 caracteres
- **Descripción completa**: Explica tu juego, características, etc.
- **Icono**: 512x512 px (PNG, sin transparencia)
- **Capturas de pantalla**: 
  - Mínimo 2, máximo 8
  - Teléfono: 16:9 o 9:16, mínimo 320px
  - Tableta: 16:9 o 9:16, mínimo 320px

#### B. Clasificación de Contenido
- Completa el cuestionario sobre el contenido de tu app
- Esto determina la edad mínima para descargar

#### C. Precios y Distribución
- Selecciona países donde quieres distribuir
- Configura si es gratis o de pago
- Políticas de reembolso (si aplica)

### Paso 4: Subir el APK/AAB

1. Ve a "Producción" > "Crear nueva versión"
2. Sube tu archivo `.aab` o `.apk` firmado
3. Completa las notas de la versión (qué hay de nuevo)
4. Guarda y revisa

### Paso 5: Políticas y Programas

#### Política de Privacidad
- **OBLIGATORIO**: Necesitas una URL de política de privacidad
- Puedes usar generadores gratuitos como:
  - https://www.freeprivacypolicy.com/
  - https://www.privacypolicygenerator.info/

#### Contenido de la App
- Acepta las políticas de Google Play
- Verifica que tu app cumple con las políticas

### Paso 6: Revisar y Publicar

1. Revisa toda la información
2. Verifica que no haya errores (aparecerán en rojo)
3. Click en "Enviar para revisión"
4. **Tiempo de revisión**: 1-7 días (normalmente 1-3 días)

## 📝 Checklist Antes de Publicar

- [ ] APK/AAB firmado generado
- [ ] VersionCode y versionName configurados
- [ ] Icono de la app (512x512)
- [ ] Capturas de pantalla (mínimo 2)
- [ ] Descripción completa y atractiva
- [ ] Política de privacidad (URL)
- [ ] Clasificación de contenido completada
- [ ] Países de distribución seleccionados
- [ ] App probada en diferentes dispositivos
- [ ] Sin errores en la consola

## 🎨 Recursos Necesarios

### Icono de la App
- **Tamaño**: 512x512 px
- **Formato**: PNG (sin transparencia)
- **Fondo**: Puede ser transparente, pero Google lo pondrá sobre fondo blanco

### Capturas de Pantalla
- **Cantidad**: Mínimo 2, máximo 8
- **Tamaño**: 
  - Teléfono: 320px - 3840px (ancho o alto)
  - Tableta: 320px - 3840px
- **Aspecto**: 16:9 o 9:16
- **Formato**: PNG o JPEG

### Imagen Promocional (Opcional)
- **Tamaño**: 1024x500 px
- **Formato**: PNG o JPEG

## 💡 Consejos

1. **Primera versión**: Empieza con una versión beta cerrada para probar
2. **Actualizaciones**: Incrementa `versionCode` en cada actualización
3. **Descripción**: Sé claro y atractivo, menciona características clave
4. **Capturas**: Muestra las mejores características del juego
5. **Feedback**: Responde a los comentarios de los usuarios

## 🔄 Actualizar tu App

1. Incrementa `versionCode` en `build.gradle`
2. Actualiza `versionName` (ej: "1.0.1")
3. Genera nuevo APK/AAB firmado
4. En Play Console: "Producción" > "Crear nueva versión"
5. Sube el nuevo archivo y completa las notas de versión
6. Envía para revisión

## 📞 Soporte

- **Documentación oficial**: https://support.google.com/googleplay/android-developer
- **Foro de desarrolladores**: https://support.google.com/googleplay/android-developer/community

## ⚠️ Notas Importantes

1. **Keystore**: Guárdalo en un lugar seguro. Si lo pierdes, no podrás actualizar tu app.
2. **Política de privacidad**: Es obligatoria, incluso si no recopilas datos.
3. **Revisión**: Google revisa todas las apps antes de publicarlas.
4. **Actualizaciones**: Pueden tardar algunas horas en estar disponibles después de la aprobación.

---

¡Buena suerte con tu publicación! 🎮🚀

