# UI Rules

This document is a frontend execution guide for building screens consistently from the provided designs.

## Before starting a screen

- First check whether the project already has a similar screen, layout, or component.
- Reuse existing structure, styles, and naming whenever the role is the same.
- Do not invent a new pattern if an equivalent one already exists.

## Layout

- Prefer `VBox` and `HBox` by default.
- Use `spacing`, `padding`, `alignment`, and `maxWidth` before trying more manual positioning.
- Use `StackPane` mainly for layering, such as background image + overlay + foreground content.
- Avoid `AnchorPane` unless there is a clear reason it is necessary.

## Spacing and alignment

- Match the design intentionally. Do not guess spacing.
- Align the full content block first, then tune the spacing inside it.
- Use container alignment for placement and `spacing` for rhythm.
- Do not fake spacing with random empty nodes unless there is a strong reason.

## Sizing and responsiveness

- The app should open using the intended design ratio.
- Do not force fullscreen on launch unless product behavior requires it.
- Layouts must still behave cleanly when resized or maximized.
- Avoid hard-coded positioning that only works at one size.

## Reuse and naming

- If two components play the same role, use the same naming.
- Reuse shared style classes instead of creating near-duplicates.
- Keep screen-specific CSS thin when styles can live in shared CSS.
- Asset names should be descriptive and meaningful.

## Visual consistency

- Preserve approved background treatments such as image + overlay combinations.
- Keep typography, field styling, and button styling consistent across similar screens.
- If clickable text must be implemented as a `Button`, style it to look like text, not like a default JavaFX button.
- If final artwork is not ready, use an obvious placeholder without breaking the intended layout.

## Final check

Before finishing a screen, verify:

- The screen matches the design proportion and structure.
- Similar screens use the same layout approach and naming.
- Shared styles are reused instead of duplicated.
- Spacing, alignment, and interactive text look intentional.
- Backgrounds, overlays, and assets are preserved correctly.
