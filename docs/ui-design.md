# UI design guide

How the JavaFX interface of NextFit is designed, and how to build a screen that looks like the rest. Everything lives in
`com.nextfit.ui.fx` and in two style sheets.

## Look

NextFit uses a near-black violet base, brighter violet accents and lime for key actions and figures. Both dark and light themes use AtlantaFX as their base:

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

The dark workspace uses a solid near-black violet background. The Panel header uses the sign-in gym photo, with two smaller equipment crops in the membership and attendance cards. Charts use solid surfaces to keep their axes and labels readable. The light theme uses `images/bg/bg-light.jpg`. The logo is in `images/logo/` (SVG and PNG 32 to 512).

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
| `installPressEffect(scene)` | Every primary or secondary button shrinks a little when pressed (installed once in `FxApp`). |

Charts rise by changing their data with a `Timeline` (line chart) or by scaling each bar (bar chart). Keep the motion
short (under a second) and never block the user: the screen is usable from the first frame.

Gotchas: `Interpolator.SPLINE` only accepts control points between 0 and 1 (a bounce needs its own `Interpolator`); an
exception while building the dashboard keeps the user on the login screen with no message.

## The sign-in screen and the tables

`LoginView` draws the brand, what the system does and the sign-in card over a photo. The screen has its own dark colors
(`.nf-login` redefines the tokens, also in the light theme) because it sits on a photo, so it reads the same in both
themes. The password field has an eye button that swaps it with a text field bound to the same text. `LoginView` only
collects the credentials; checking them stays in `AuthService`.

Every table (`.nf-table`) draws a thin line under each row with `-nf-border` and a taller row (54 px) with side padding in
the cells, so the data does not look crowded. Empty rows below the data have no line.

Use `FxTables.alignNumbers(column)` for amounts and counts. It aligns the header and the values to the right. Keep visible
text on status badges so the meaning is clear without relying on color. `AccessFeedback` provides the same entry, exit
and denial treatment for the face and password screens.

The member file keeps its identity card above the tabs. Membership sales place the recipient, total and charge action
in a separate summary beside the form when there is room. In narrower windows the summary follows the form.

## The shell

`ShellView` holds the sidebar, the top bar and the screen of the chosen module. The sidebar can be collapsed to icons
(button at the left of the title); the choice is remembered with `java.util.prefs`. It builds its buttons from the list
of modules, hiding those that `allowedFor(role)` refuses. The sidebar groups modules by operation, monitoring and administration and scrolls independently in shorter windows. The Panel switches to two indicator columns and stacked charts when the content area is narrow.

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
