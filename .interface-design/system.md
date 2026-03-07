# NAP POS — Design System

## Direction & Feel

**Who:** Tendero colombiano, dueño de negocio pequeño/mediano. Abre la app temprano, entre café y primeras ventas. Necesita rapidez, no complejidad. No es técnico; confía en lo que se siente sólido y familiar.

**What:** Registrar ventas, controlar inventario, gestionar compras y clientes. Todo rápido, sin fricción, sin adornos que estorben.

**Feel:** Mostrador oscuro + papel de recibo cálido. La interfaz se siente como un local bien organizado — madera oscura del mueble, papel amarillento del recibo, tinta negra de la factura. Sólido, cálido, confiable. No frío-tech ni corporativo.

**Signature:** La dualidad mostrador/recibo — sidebar con gradiente carbón (#192030 → #303441), ítem activo como píldora blanca sobre ese oscuro. El contenido en papeles cálidos (#F5F1EB, #FDFCFA). El acento es azul-violeta (#5A6ACF) — calma y precisión. Los bordes son rgba sobre la tinta, nunca colores sólidos extraños.

---

## Color Tokens

### Surfaces

| Token | Hex | Uso |
| --- | --- | --- |
| mek-dark | `#192030` | Base sidebar / headers (carbón de mostrador) |
| (gradient) | `#192030` → `#303441` | Sidebar — gradiente vertical |
| mek-base | `#F5F1EB` | Fondo de la app (papel de recibo) |
| mek-card | `#FDFCFA` | Cards, paneles elevados |
| mek-input | `#EDE9E2` | Campos de entrada (hundidos) |

### Ink / Text Hierarchy
| Token       | Value              | Uso                            |
|-------------|--------------------|---------------------------------|
| ink-1       | `#1A1F2E`          | Texto primario                  |
| ink-2       | `#4B5563`          | Texto secundario                |
| ink-3       | `#78716C`          | Terciario / gris cálido         |
| ink-4       | `#A8A29E`          | Desactivado / placeholder       |

### On Dark Surface
| Token       | Value              | Uso                            |
|-------------|--------------------|---------------------------------|
| on-dark-1   | `#F0EDE8`          | Texto primario sobre oscuro     |
| on-dark-2   | `rgba(..., 0.62)`  | Texto secundario sobre oscuro   |
| on-dark-3   | `rgba(..., 0.35)`  | Desactivado sobre oscuro        |

### Brand Accent

| Token | Hex | Uso |
| --- | --- | --- |
| mek-primary | `#5A6ACF` | Acción primaria (azul-violeta) |
| mek-primary-light | `#7A88DB` | Iconos activos, acentos sutiles |
| mek-primary-hover | `#4A58BF` | Hover de botón primario |
| mek-primary-pressed | `#3C48AA` | Pressed / active |
| mek-primary-tint | `#ECEEF7` | Fondo de selección suave |
| mek-sidebar-active | `#FFFFFF` | Fondo nav-item activo (píldora blanca) |
| mek-sidebar-active-soft | `#F6F7FC` | Hover sobre ítem activo |

### Semantic
| State   | Foreground | Background | Metáfora          |
|---------|-----------|------------|---------------------|
| success | `#15803D` | `#DCFCE7`  | Sello aprobado      |
| danger  | `#DC2626` | `#FEE2E2`  | Sello anulado       |
| warning | `#D97706` | `#FEF3C7`  | Cobre / caja        |

### Borders
| Tipo           | Value                      |
|----------------|---------------------------|
| standard       | `rgba(26,31,46, 0.12)`    |
| soft           | `rgba(26,31,46, 0.06)`    |
| emphasis       | `rgba(26,31,46, 0.28)`    |
| on-dark        | `rgba(240,237,232, 0.10)` |

---

## Depth Strategy

**Borders + subtle shadows.** La separación se da con bordes rgba (nunca hex sólido) para que respiren con el fondo cálido. Cards y paneles elevados usan un dropshadow suave:

```
dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2)
```

Login card usa sombra más dramática por estar sobre fondo oscuro:

```
dropshadow(gaussian, rgba(0,0,0,0.30), 48, 0, 0, 10)
```

Inputs son más oscuros que su contenedor (mek-input #EDE9E2 sobre mek-card #FDFCFA) — efecto "inset".

---

## Typography

- **Family:** `"Inter", "Segoe UI", "Arial", sans-serif`
- **Base size:** 13px
- **Hierarchy:**
  - Brand: 24px, weight 700, tracking 1.5px
  - Section titles (topbar): 16px, weight 600
  - Group labels (sidebar): 10px, weight 600, uppercase, tracking 1px
  - Body: 13px, weight 400
  - Labels/captions: 12px
  - Small metadata: 11px

---

## Spacing

- **Base unit:** 4px
- **Scale:** 4 · 8 · 12 · 14 · 16 · 18 · 20 · 24 · 28 · 32
- **Component padding:** 10px 16px (buttons), 14px 32px (wizard footers)
- **Section spacing:** 14–28px between groups
- **Sidebar width:** 220px

---

## Border Radius

- **Controls:** 8px (inputs, buttons, combo-boxes)
- **Cards:** 12–14px
- **Modals/login:** 14px
- **Chips/badges:** 16–20px
- **Indicators (dots):** 50% (full circle)

---

## Iconography

- **Library:** Ikonli FontAwesome 5 (`ikonli-javafx:12.3.1` + `ikonli-fontawesome5-pack:12.3.1`)
- **Usage:** `FontIcon` en FXML con `iconLiteral` y `iconSize`
- **Standalone icons:** Dentro de `StackPane.tarjeta-icono` (56×56, fondo circled con mek-primary-tint #e8f4fc)
- **Button icons:** Como `<graphic>` con `contentDisplay="RIGHT"` para flechas de avance
- **Utility classes:** `.icon-accent`, `.icon-accent-light`, `.icon-success`, `.icon-danger`, `.icon-warning`, `.icon-on-dark`, `.icon-on-primary`

---

## Key Component Patterns

### nav-item (sidebar button)

**Shape:** Full width, left-aligned, transparent background by default.

**Icon:** `FontIcon` as `<graphic>`, `styleClass="nav-icon"`, `iconSize="16"`. Icon color driven entirely by CSS child selectors — no hardcoded values in FXML or Java.

**Color states (token-driven, no hardcoded hex):**

| State | Text | Icon (nav-icon) | Background |
| --- | --- | --- | --- |
| Default | `nav-default-text` (on-dark-1 @ 60%) | same | transparent |
| Hover | `on-dark-1` (#F0EDE8) | `on-dark-1` | rgba(255,255,255,0.06) |
| Active | `#192030` (dark) | `mek-primary` #5A6ACF | `#FFFFFF` (white pill) |

**Active pill blanca (`.nav-item-active`):**

- `-fx-background-color: nav-active-bg` (`#FFFFFF`)
- `-fx-background-radius: 10` — pill redondeada
- `-fx-background-insets: 4 10` — inset desde los bordes del sidebar
- `-fx-border-color: transparent` — sin borde
- `-fx-text-fill: #192030` — texto oscuro sobre fondo blanco
- `-fx-font-weight: 700`
- `-fx-padding: 10 16 10 16`

**Transition animation (JavaFX Timeline — no CSS transitions in JavaFX 21):**

- Entering active: 220ms fade-in `transparent-white → opaque-white`, `Interpolator.EASE_BOTH`
- Leaving active: 160ms fade-out `opaque-white → transparent-white`, `Interpolator.EASE_BOTH`
- Se usa `Color.color(1,1,1,0)` como inicio/fin (blanco transparente) para evitar gris en la interpolación
- Inline style fuerza `-fx-text-fill: #192030` durante toda la animación (legible en fondo claro y oscuro)
- On finish: `setStyle("")` cede control al CSS; previene acumulación de estilos
- Race condition guard: `Map<Button, Timeline>` cancela animación previa antes de iniciar nueva

**JavaFX CSS tokens (defined on `.root` as looked-up colors):**
```
mek-primary:       #5A6ACF
mek-primary-light: #7A88DB
nav-active-bg:     #FFFFFF
nav-active-bg-hover: #F6F7FC
nav-active-border: #5A6ACF
nav-default-text:  rgba(240,237,232,0.60)
on-dark-1:         #F0EDE8
```
Tokens propagate to all child nodes without `-fx-` prefix — native JavaFX looked-up color system.

### Cards (.tarjeta)
- Background: mek-card (#FDFCFA)
- Border: 2px solid `rgba(26,31,46,0.12)`, radius 12px
- Selected: border → mek-primary (#5A6ACF), shadow `0 0 0 3px rgba(90,106,207,0.15)`
- Icon container: 56×56 StackPane with circular mek-primary-tint background (#ECEEF7)

### Form Controls

- **Text fields:** mek-input (#EDE9E2) bg, radius 8px, border `rgba(26,31,46,0.12)`, focus → mek-primary (#5A6ACF) border + glow `rgba(90,106,207,0.18)`
- **ComboBox:** Same as text fields, dropdown list → mek-card bg
- **Spinner:** Split arrows right-aligned, same field styling
- **DatePicker:** Month-year header → mek-dark (#192030), cells → mek-input
- **CheckBox:** 18×18 box, checked → mek-primary (#5A6ACF) with white mark

### Buttons

- **Primary (.btn-primario):** mek-primary (#5A6ACF) bg, white text, radius 8px, hover → mek-primary-hover (#4A58BF)
- **Secondary (.btn-secundario):** transparent bg, border `rgba(26,31,46,0.18)`, ink-1 text
- **Sidebar logout:** Transparent, on-dark-2, hover → on-dark-1

### Tables
- Header: mek-dark (#192030), on-dark-1 text, weight 600
- Rows: alternate mek-card / mek-base
- Row hover: warm beige `#F0EDE7`
- Selected: mek-primary-tint (#ECEEF7), border-left mek-primary

### Login Window

- **Estructura:** HBox raíz — split panel horizontal
- **Panel izquierdo (login-panel-left):** `#192030`, `prefWidth=340`, `minWidth=240`. Dos círculos decorativos (`login-deco-1` violeta tenue `rgba(90,106,207,0.18)`, `login-deco-2` blanco muy tenue) + marca centrada ("NAP POS" 30px bold blanco, subtítulo dim, línea violeta `rgba(90,106,207,0.55)`, tagline itálico)
- **Panel derecho (login-panel-right):** `#F5F6FA`, `HBox.hgrow=ALWAYS`. Formulario centrado sin card: store name 20px bold `#1E2A3A`, hint `#A0AEC0`, campos `login-field`
- **Inputs (login-field):** `#FFFFFF` + borde `#CBD5E0` — misma paleta cool que wizard
- **Resizabilidad:** `setMinWidth(520)`, `setMinHeight(480)`. **No usar** `setResizable(false)`

### Wizard

- Separate stylesheet (setup_wizard.css) with cooler palette (white bg, #CBD5E0 borders)
- Login también usa esta paleta (ver Login Window)
- Step indicators: numbered circles, active → mek-primary (#5A6ACF)

### Resizabilidad de Ventanas

- **Wizard:** `setMinWidth(700)`, `setMinHeight(560)` — sin `setResizable(false)`
- **Login:** `setMinWidth(520)`, `setMinHeight(480)` — sin `setResizable(false)`
- **Main window:** `setMinWidth(1100)`, `setMinHeight(700)` — sin `setResizable(false)`
- Regla: ninguna ventana usa `setResizable(false)`

---

## Stylesheets

| File              | Scope                                        |
|-------------------|----------------------------------------------|
| `styles.css`      | Global — login, main window, all views       |
| `setup_wizard.css`| Setup wizard + importación wizard + login    |
| `importacion.css` | Import-specific overrides                    |

---

## Rejecting Defaults

| Default | Our Choice |
| --- | --- |
| Gray sidebar, white content | Carbón mostrador (#1E2A3A) + papel cálido |
| Generic `--gray-700` tokens | `ink-1`, `mek-dark`, `mek-input` — product vocabulary |
| OS emoji icons | Ikonli FontAwesome 5 vector icons |
| Hex solid borders | rgba borders that breathe with surfaces |
| White inputs on white cards | Darker mek-input (#EDE9E2) "inset" feel |
| Flat semantic colors | Paired fg/bg ("sello aprobado/anulado") |
| Electric blue (#3B82F6) | Azul-violeta (#5A6ACF) — calma y precisión |
| Border-left nav accent | White pill on dark sidebar — contraste limpio |
