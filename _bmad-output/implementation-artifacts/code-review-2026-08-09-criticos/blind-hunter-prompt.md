# Blind Hunter — Prompt de revisión

**Ejecutar en una sesión NUEVA de opencode** (los subagentes personalizados solo están disponibles en sesiones iniciadas después de crearlos).

## Cómo ejecutar

En una sesión nueva de opencode, lanza:

```
@blind-hunter Invoke the bmad-review-adversarial-general skill on this diff:
```

Y pega el contenido de `diff.txt` (o indica que lea el archivo `_bmad-output/implementation-artifacts/code-review-2026-08-09-criticos/diff.txt`).

## Contexto

App Android de presencia que despliega un overlay negro a pantalla completa sobre la app objetivo (Teams), salida por 1 tap, watchdog de sesión, FGS specialUse. Los fixes revisados:

1. AppContainer con scope de aplicación (la sesión vive en el proceso, no en la actividad)
2. Limpieza defensiva en onCreate (solo si savedInstanceState == null)
3. Overlay con WindowInsetsController para ocultar barras + cutout mode ALWAYS + sin FLAG_NOT_FOCUSABLE
4. IntentLauncher con fallback a getLaunchIntentForPackage
5. PatternDetector 1 tap en toda la pantalla (regionFraction 1.0)
6. Avisos de FLAG_SECURE en el aviso de uso responsable y la notificación

## Instrucciones

Revisa con escepticismo extremo — asume que hay problemas. Busca lo que FALTA, no solo lo que está mal. Encuentra al menos 10 issues. Salida: lista Markdown de descripciones, sin severidad ni ranking.

Puedes leer el código fuente en `app/src/main/java/com/stayalert/` para contexto adicional.
