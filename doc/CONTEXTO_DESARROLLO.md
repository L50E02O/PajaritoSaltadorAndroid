# Contexto para desarrolladores (y flujo con IA)

Este documento resume **como esta organizado Pajarito Saltador** para que puedas orientar a herramientas de IA o a nuevas personas en el repo sin perder tiempo en descubrir el mapa del codigo.

## Que es el proyecto

Juego arcade Android (vista vertical): un pajaro evita tuberias, suma puntos, recoge items que reducen cooldowns y usa tres poderes (invencibilidad, velocidad, romper tuberia). La logica evita numeros magicos en pixeles absolutos: el **viewport logico** del juego escala a la pantalla.

## Reglas de arquitectura (resumen)

Definidas en `.cursorrules` en la raiz; conviene respetarlas en cambios nuevos:

- **MVVM**: `MainActivity` + `GameViewModel` (y `PipeViewModel` donde aplique) separados de la logica pura.
- **Renderizado**: `GameView` (`SurfaceView`) dibuja en `Canvas`; bucle de juego en hilo propio.
- **Poderes**: `PowerUpManager` centraliza cooldowns y duraciones.
- **Persistencia**: preferencias y puntajes con **SharedPreferences** (o DataStore si se migra); operaciones `apply()` para no bloquear el hilo principal.
- **Responsividad**: posiciones y colisiones en `GameLogic` y `GameView` usan **fracciones del ancho/alto** del viewport (360x640 virtual) escalado a la vista real.

## Mapa de carpetas y clases clave

| Ruta | Rol |
|------|-----|
| `app/src/main/java/com/pajaritosaltador/game/MainActivity.kt` | Enlaza UI, `GameView`, `GameViewModel`, dialogo de ajustes, barra de poderes arrastrable, `SoundEffectsPlayer`, servicio de musica. |
| `app/src/main/java/com/pajaritosaltador/game/GameView.kt` | Superficie de juego, render, hilo `GameThread`, callbacks hacia la Activity. |
| `app/src/main/java/com/pajaritosaltador/game/GameLogic.kt` | Estado del juego, pajaro, tuberias, coleccionables, dificultad progresiva, scroll de fondo. |
| `app/src/main/java/com/pajaritosaltador/game/PowerUpManager.kt` / `Physics.kt` (`PowerUp`) | Gestor de poderes y modelo de datos de poder. |
| `app/src/main/java/com/pajaritosaltador/game/GameViewModel.kt` | Preferencias (musica, SFX, posicion barra de poderes), high score. |
| `app/src/main/java/com/pajaritosaltador/game/PowerBarGeometry.kt` | Limites y migracion de posicion de la barra de poderes (normalizado 0-1). |
| `app/src/main/java/com/pajaritosaltador/game/SoundEffectsPlayer.kt` | Efectos cortos con `ToneGenerator` (si se sustituyen por `SoundPool` + `res/raw`, mantener el toggle SFX). |
| `app/src/main/java/com/pajaritosaltador/game/MusicService.kt` | Musica de fondo en loop (`res/raw/lofi_background.mp3`). |
| `app/src/main/java/com/pajaritosaltador/game/RetroPipeDrawer.kt` | Dibujo de tuberias en canvas. |
| `app/src/main/res/layout/activity_main.xml` | Jerarquia de capas: juego, overlays de menu/pausa/game over, **barra de poderes**, HUD encima para que el engranaje sea clicable en menu. |
| `app/src/main/res/layout/dialog_settings.xml` | Ajustes y accesibilidad. |
| `app/src/test/java/.../GameLogicTest.kt` | Pruebas unitarias de la logica central. |
| `app/src/test/java/.../PowerBarGeometryTest.kt` | Pruebas de geometria de la barra. |

## Flujo de datos tipico

1. `GameLogic.update(delta, jump)` avanza fisica, tuberias, poderes y colisiones.
2. `GameView` llama a `update` desde el hilo del juego y luego `render(canvas)`.
3. Callbacks (`onScoreUpdate`, `onGameOver`, `onCollectiblePicked`, etc.) pasan a la Activity en el hilo principal (`Handler`).
4. `GameViewModel` persiste high score y preferencias cuando la Activity actualiza.

## Cambios frecuentes (donde tocar)

| Objetivo | Donde mirar primero |
|----------|---------------------|
| Dificultad, velocidad de tubos, gap | `GameLogic.kt` (constantes y `increaseDifficulty`). |
| Nuevo poder o tiempo de cooldown | `PowerUpManager.kt`, `PowerUp` en `Physics.kt`, UI en `activity_main.xml` + `MainActivity`. |
| Graficos del pajaro o fondo | `GameView.kt` (metodos `draw*`). |
| Sonidos de acciones | `SoundEffectsPlayer.kt`, llamadas desde `MainActivity`. |
| Textos o idioma | `res/values/strings.xml`. |
| Version publicada | `app/build.gradle` (`versionCode`, `versionName`) y proceso en [GUIA_RELEASE_GITHUB.md](GUIA_RELEASE_GITHUB.md). |

## Convenciones del repo

- **Commits y comentarios** en español, sin emojis en mensajes de commit (convencion del equipo).
- Pruebas: `./gradlew test` (o `gradlew.bat test` en Windows) antes de integrar cambios grandes en la logica.

## Sugerencia para prompts de IA

Incluye en el prompt:

- Ruta de este archivo o de `.cursorrules`.
- El objetivo concreto (por ejemplo: “solo cambiar spawn de tuberias”).
- Que no se rompa el escalado por viewport ni el `PowerUpManager`.

Asi se reduce el alcance y se evitan refactors innecesarios en el mismo cambio.
