# NAP POS — Design System

## Direction & Feel

**Who:** Tendero colombiano, dueño de negocio pequeño/mediano. Abre la app temprano, entre café y primeras ventas. Necesita rapidez, no complejidad. No es técnico; confía en lo que se siente sólido y familiar.

**What:** Registrar ventas, controlar inventario, gestionar compras y clientes. Todo rápido, sin fricción, sin adornos que estorben.

**Feel:** Mostrador oscuro + papel de recibo cálido. La interfaz se siente como un local bien organizado — madera oscura del mueble, papel amarillento del recibo, tinta negra de la factura. Sólido, cálido, confiable. No frío-tech ni corporativo.

**Signature:** La dualidad mostrador/recibo — superficies oscuras de carbón (#1E2A3A) para estructura (sidebar, headers), y papeles cálidos de recibo (#F5F1EB, #FDFCFA) para contenido. Los bordes son rgba sobre la tinta, nunca colores sólidos extraños.

---

## Color Tokens

### Surfaces
| Token       | Hex       | Uso                                     |
|-------------|-----------|------------------------------------------|
| mek-dark    | `#1E2A3A` | Sidebar, headers (carbón de mostrador)   |
| mek-base    | `#F5F1EB` | Fondo de la app (papel de recibo)        |
| mek-card    | `#FDFCFA` | Cards, paneles elevados                  |
| mek-input   | `#EDE9E2` | Campos de entrada (hundidos)             |

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
| Token        | Hex       | Uso                            |
|--------------|-----------|--------------------------------|
| blue         | `#3B82F6` | Acción primaria                |
| blue-hover   | `#2563EB` | Hover                          |
| blue-pressed | `#1D4ED8` | Pressed / active               |
| blue-tint    | `#EFF6FF` | Fondo de selección suave       |

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

- **Family:** `"Segoe UI", "Arial", sans-serif`
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
- **Standalone icons:** Dentro de `StackPane.tarjeta-icono` (56×56, fondo circled con blue-tint #EFF6FF)
- **Button icons:** Como `<graphic>` con `contentDisplay="RIGHT"` para flechas de avance
- **Utility classes:** `.icon-accent`, `.icon-success`, `.icon-danger`, `.icon-warning`, `.icon-on-dark`, `.icon-on-primary`

---

## Key Component Patterns

### nav-item (sidebar button)
- Full width, left-aligned, transparent background
- Hover: `rgba(255,255,255,0.06)` + border-left `#3B82F6` 3px
- Active: `rgba(255,255,255,0.10)` + text `#F0EDE8` + blue left accent
- Text: on-dark-2 default → on-dark-1 hover → on-dark-1 active

### Cards (.tarjeta)
- Background: mek-card (#FDFCFA)
- Border: 2px solid `rgba(26,31,46,0.12)`, radius 12px
- Selected: border → blue (#3B82F6), shadow `0 0 0 3px rgba(59,130,246,0.15)`
- Icon container: 56×56 StackPane with circular blue-tint background

### Form Controls
- **Text fields:** mek-input (#EDE9E2) bg, radius 8px, border `rgba(26,31,46,0.12)`, focus → blue border + blue glow
- **ComboBox:** Same as text fields, dropdown list → mek-card bg
- **Spinner:** Split arrows right-aligned, same field styling
- **DatePicker:** Month-year header → mek-dark (#1E2A3A), cells → mek-input
- **CheckBox:** 18×18 box, checked → blue (#3B82F6) with white mark

### Buttons
- **Primary (.btn-primario):** blue bg, white text, radius 8px, hover → blue-hover
- **Secondary (.btn-secundario):** transparent bg, border `rgba(26,31,46,0.18)`, ink-1 text
- **Sidebar logout:** Transparent, on-dark-2, hover → on-dark-1

### Tables
- Header: mek-dark (#1E2A3A), on-dark-1 text, weight 600
- Rows: alternate mek-card / mek-base
- Selected: blue-tint (#EFF6FF), border-left blue

### Wizard
- Separate stylesheet (setup_wizard.css) with cooler palette (white bg, #CBD5E0 borders)
- Shared via importacion.css for import wizard
- Step indicators: numbered circles, active → blue

---

## Stylesheets

| File              | Scope                                  |
|-------------------|----------------------------------------|
| `styles.css`      | Global — login, main window, all views |
| `setup_wizard.css`| Setup wizard + importación wizard      |
| `importacion.css` | Import-specific overrides              |

---

## Rejecting Defaults

| Default                      | Our Choice                              |
|------------------------------|-----------------------------------------|
| Gray sidebar, white content  | Carbón mostrador (#1E2A3A) + papel cálido |
| Generic `--gray-700` tokens  | `ink-1`, `mek-dark`, `mek-input` — product vocabulary |
| OS emoji icons               | Ikonli FontAwesome 5 vector icons        |
| Hex solid borders            | rgba borders that breathe with surfaces  |
| White inputs on white cards  | Darker mek-input (#EDE9E2) "inset" feel  |
| Flat semantic colors         | Paired fg/bg ("sello aprobado/anulado")  |
