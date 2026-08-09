# Edge Case Hunter — Prompt de revisión

**Ejecutar en una sesión NUEVA de opencode** (los subagentes personalizados solo están disponibles en sesiones iniciadas después de crearlos).

## Cómo ejecutar

En una sesión nueva de opencode, lanza:

```
@edge-case-hunter Invoke the bmad-review-edge-case-hunter skill on this diff:
```

Y pega el contenido de `diff.txt` (o indica que lea el archivo `_bmad-output/implementation-artifacts/code-review-2026-08-09-criticos/diff.txt`).

## Contexto

App Android de presencia que despliega overlay negro a pantalla completa sobre la app objetivo, salida por 1 tap, watchdog 2s, FGS specialUse. Los fixes:

1. AppContainer (scope de aplicación, lazy circular con tipos explícitos)
2. Limpieza defensiva en onCreate (solo si savedInstanceState == null)
3. Overlay con WindowInsetsController + cutout ALWAYS + sin FLAG_NOT_FOCUSABLE + setOnKeyListener consume back
4. IntentLauncher con fallback getLaunchIntentForPackage
5. PatternDetector 1 tap regionFraction 1.0
6. Avisos FLAG_SECURE

## Instrucciones

Recorre cada camino de ramificación y condición límite del diff. Reporta SOLO edge cases no manejados — método, no actitud. Para cada hallazgo: la rama/límite exacto, por qué no está manejado, y el input/estado concreto que lo dispara. Salida: lista Markdown o JSON con location, trigger_condition, guard_snippet, potential_consequence.

Puedes leer el código fuente en `app/src/main/java/com/stayalert/` para contexto adicional.
