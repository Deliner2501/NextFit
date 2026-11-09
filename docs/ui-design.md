# UI design guide

How the JavaFX interface of NextFit is designed, and how to build a screen that looks like the rest. Everything lives in
`com.nextfit.ui.fx` and in two style sheets.

## Look

Violet and lime, in two themes (dark and light). The base theme is AtlantaFX; on top of it:

| File | Content |
|---|---|
| `resources/fx/nextfit.css` | Base: colors as tokens (`-nf-*`) for `.root.dark` and `.root.light`, typography, buttons, inputs, tables, charts. |
| `resources/fx/nextfit-violet.css` | Loaded after the first one. Violet and lime look, depth, backgrounds, glows. |

Colors are **tokens**, never fixed values: use `-nf-text`, `-nf-muted`, `-nf-border`, `-nf-violet-soft`... so a screen is
correct in both themes. The theme is switched by `FxTheme`.

### Depth

Cards, tables and lists are drawn with layered backgrounds: a gradient **border ring**, the fill, a **top light** and,
for cards, two radial **glows** in the corners. The tokens are `-nf-ring-*`, `-nf-shine-*`, `-nf-top-light` and `-nf-lift`.
Two rules learned the hard way:

- In a JavaFX gradient do not use `transparent`: it is black with alpha 0 and leaves a grey halo. Use the **same color
  with alpha 0** (there are tokens for it).
- `ShellView` is the root of the scene and carries `nf-bg`, so a selector for the background must be
  `.root.dark.nf-bg, .root.dark .nf-bg`.

### Backgrounds and images

`images/bg/bg-dark.jpg` and `bg-light.jpg` are the window backgrounds (`-fx-background-size: cover`). The logo is in
`images/logo/` (SVG and PNG 32 to 512). The athlete of the menu is `images/sidebar/promo.png`, a PNG with feathered edges
so it melts into the sidebar.

## Building blocks

| Class or style | Use |
|---|---|
| `FxModule` | Contract of a screen of the menu: `id()`, `title()`, `icon()`, `view()`, `onShow()`, `allowedFor(Role)`. |
| `nf-card` | A card. Give it `setPadding(new Insets(20))`. |
| `KpiCard` | A big number with caption and hint, in three kinds (feature, violet, lime). `trend(text, up, tooltip)` adds a green or red chip in the corner; `KpiCard.chip(...)` makes the chip alone. |
| `FxTables` | Text columns and colored state columns for tables. Give a table `nf-table`. |
| `FxDialogs` | Forms, messages, confirmations and errors. Do not open a plain `Alert`. |
| `FxIcons` | Vector icons (`FxIcons.of(path, size)`); add a new constant for a new icon. |
| `FileExport` | The "save as" dialog for PDF, Excel and other files. |
| `nf-primary`, `nf-primary-small`, `nf-secondary`, `nf-icon-button` | Buttons. Only these three first ones react when pressed. |

## Motion

JavaFX CSS cannot animate, so all motion is in `FxMotion`:

| Method | Effect |
|---|---|
| `enter(nodes, delay, step)` | The nodes fade and slide in one after another. |
| `countUp(label, target, format)` | A number counts from zero to its value. |
| `popIn(node, delay)` | A badge grows with a small bounce. |
| `spinIn(node, delay)` | A round chart appears spinning. |
| `hoverLift(node, scale)` | A card grows slightly under the mouse. |
| `nudge(node, dx)` | A menu button moves a few pixels when hovered. |
| `installPressEffect(scene)` | Every primary or secondary button shrinks a little when pressed (installed once in `FxApp`). |

Charts rise by changing their data with a `Timeline` (line chart) or by scaling each bar (bar chart). Keep the motion
short (under a second) and never block the user: the screen is usable from the first frame.

Gotchas: `Interpolator.SPLINE` only accepts control points between 0 and 1 (a bounce needs its own `Interpolator`); an
exception while building the dashboard keeps the user on the login screen with no message.

## The shell

`ShellView` holds the sidebar, the top bar and the screen of the chosen module. The sidebar can be collapsed to icons
(button at the left of the title); the choice is remembered with `java.util.prefs`. It builds its buttons from the list
of modules, hiding those that `allowedFor(role)` refuses.

## Add a new screen

1. Create `MyModule implements FxModule` and build its `view()` with `VBox`/`HBox`, `nf-card` and `KpiCard`.
2. Put the colors in tokens, the texts in Spanish and the code and comments in English.
3. Show messages and errors with `FxDialogs`; catch `NextFitException` and let the service decide the rules.
4. Animate the entrance with `FxMotion.enter` (optional).
5. Register the module in the list of `Main`. If a `PendingModule` with the same id exists, replace it.
6. Check the screen in dark and light, with the menu open and collapsed.

## Checklist before a pull request

- [ ] Looks right in dark and light.
- [ ] No fixed colors in Java or CSS (use tokens).
- [ ] No business rule in the screen (it belongs to a service).
- [ ] Long texts wrap or get an ellipsis, nothing overlaps.
- [ ] Empty states have a friendly text ("Todavía no hay…").
