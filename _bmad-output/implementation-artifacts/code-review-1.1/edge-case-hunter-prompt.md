# Code Review 1.1 — Edge Case Hunter Prompt

## Diff to review

See: `_bmad-output/implementation-artifacts/code-review-1.1/diff.patch` (commit `e08e670`)

## Instructions

Invoke the `bmad-review-edge-case-hunter` skill on the diff above.

Focus: walk every branching path and boundary condition in the code. Report only unhandled edge cases. For Android scaffolding + theme code, consider: API levels, SDK versions, configuration changes, resource qualifiers, theme fallback, missing permissions, build variant edge cases, CI edge cases, and anything that could break in a non-obvious scenario.

Return a Markdown list of findings. Each finding must include file, line, edge case, and consequence. If no unhandled edge cases are found, explicitly state "No unhandled edge cases found".
