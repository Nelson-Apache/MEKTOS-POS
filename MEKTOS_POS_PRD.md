# Product Requirements Document (PRD)
## MEKTOS POS v1.0 — Sistema de Punto de Venta Desktop

**Versión:** 1.3  
**Fecha:** Febrero 2026  
**Estado:** En desarrollo  
**Propietario del producto:** MEKTOS  
**Cambios v1.1:** Se incorporan las entidades Cliente, Compra y DetalleCompra. Se añade `precioCompra` a Producto. Se documentan módulos de crédito a clientes y registro de compras de inventario.  
**Cambios v1.2:** Se añade `porcentajeGanancia` al Proveedor. Se define la lógica de cálculo automático del `precioVenta` basado en `precioCompra` + margen del proveedor.  
**Cambios v1.3:** El porcentaje de ganancia del proveedor puede ajustarse por producto (aumentar o disminuir). El ADMIN elige el proveedor principal del producto cuando hay múltiples proveedores. Se reemplaza `precioSobrescrito` por `porcentajeAjuste` para mayor flexibilidad.

---

## 1. Resumen Ejecutivo

MEKTOS POS es un sistema de punto de venta de escritorio diseñado para la tienda física MEKTOS, un negocio que comercializa papelería, productos de aseo personal y alimentos. El sistema opera 100% en modo offline, está instalado localmente en Windows y está construido con una arquitectura limpia orientada al crecimiento futuro (multi-caja, nube).

---

## 2. Problema y Oportunidad

La tienda MEKTOS opera sin un sistema POS formal, lo que genera riesgos en el control de inventario, trazabilidad de ventas, seguridad financiera y cierre de caja. Se requiere una solución robusta, transaccional y segura que pueda ser operada por personal con perfil básico (cajero) y administrada por el dueño o administrador.

---

## 3. Objetivos del Producto

- Registrar y procesar ventas de forma confiable y transaccional.
- Controlar el inventario en tiempo real, evitando stock negativo.
- Gestionar la apertura y cierre de caja con trazabilidad completa.
- Garantizar la seguridad de acceso mediante autenticación y roles.
- Gestionar clientes con opción de crédito y seguimiento de saldo.
- Registrar compras de inventario para control contable básico.
- Calcular automáticamente el precio de venta de cada producto aplicando el margen de ganancia del proveedor sobre el precio de compra.
- Proveer una base arquitectónica limpia que permita escalar sin reescribir el núcleo.

---

## 4. Usuarios Objetivo

| Rol | Descripción | Permisos |
|-----|-------------|----------|
| **ADMIN** | Dueño o administrador del negocio | Gestión completa del sistema |
| **CAJERO** | Operador de caja | Solo ventas y operaciones de caja |

---

## 5. Alcance del Producto v1.0

### Dentro del alcance

- Autenticación de usuarios con control de roles.
- Gestión de productos (CRUD): código de barras, nombre, precio de venta (calculado automáticamente), precio de compra, stock.
- Cálculo automático del precio de venta aplicando el porcentaje de ganancia del proveedor principal del producto.
- Procesamiento de ventas con descuento automático de inventario.
- Apertura y cierre de caja con cálculo de totales.
- Generación de ticket de venta (impresora térmica).
- Backup automático de base de datos al cerrar caja.
- Gestión de clientes: registro con datos personales y opción de crédito.
- Ventas a crédito: asociar una venta a un cliente con saldo pendiente.
- Registro de compras de inventario (módulo contable básico) con datos del proveedor y número de referencia/factura.
- Funcionamiento 100% offline.

### Fuera del alcance (v1.0)

- Sincronización en la nube.
- Múltiples sucursales o cajas simultáneas.
- Reportes avanzados o dashboards analíticos.
- Integración con plataformas e-commerce.
- Gestión de múltiples porcentajes de ganancia por categoría de producto (el margen aplica a nivel de proveedor).

---

## 6. Requerimientos Funcionales

### 6.1 Autenticación y Seguridad

- El sistema debe solicitar login obligatorio al iniciar.
- Las contraseñas deben estar encriptadas con BCrypt (nunca en texto plano).
- El rol ADMIN tiene acceso completo; el rol CAJERO solo a ventas y caja.
- No se debe exponer stacktrace al usuario final.
- Se deben manejar excepciones de negocio (`BusinessException`) y técnicas (`TechnicalException`) de forma separada.

### 6.2 Gestión de Productos

- Crear, editar, desactivar y consultar productos.
- Cada producto tiene: código de barras, nombre, precio de venta, precio de compra, proveedor principal, ajuste de porcentaje propio, stock y estado activo.
- El `precioCompra` es de uso interno — sirve para calcular márgenes y no se muestra al cliente.
- El `precioVenta` se calcula automáticamente con la fórmula:  
  `precioVenta = precioCompra × (1 + (porcentajeGananciaProveedor + ajusteProducto) / 100)`  
  donde `ajusteProducto` puede ser positivo (aumentar el margen) o negativo (reducirlo), y su valor por defecto es `0`.
- El ADMIN puede definir un `ajusteProducto` específico para cualquier producto sin cambiar el porcentaje base del proveedor. Esto permite subir o bajar el margen solo para ese producto.
- Cuando se cambia el proveedor principal de un producto, el `ajusteProducto` se reinicia a `0` y el `precioVenta` se recalcula con el porcentaje base del nuevo proveedor.
- El sistema debe impedir que el stock quede en negativo.
- Los productos inactivos no deben aparecer disponibles para venta.

### 6.3 Gestión de Clientes

- Crear, editar y consultar clientes.
- Datos obligatorios: nombre completo, cédula, número de celular, dirección de residencia.
- Datos opcionales: monto de crédito aprobado, plazo de pago (15 o 30 días).
- Si un cliente no tiene monto de crédito asignado, no puede realizar compras a crédito.
- El sistema debe mostrar el saldo de crédito disponible del cliente al momento de la venta.
- Regla: el monto de una venta a crédito no puede superar el saldo disponible del cliente.
- Se debe poder registrar abonos al saldo pendiente de un cliente.

### 6.4 Procesamiento de Ventas

El flujo de una venta debe ejecutarse dentro de una transacción atómica:

1. Validar que existe una caja abierta.
2. Validar disponibilidad de stock para cada producto.
3. Si la venta es a crédito: validar que el cliente existe y tiene saldo suficiente.
4. Crear el registro de la venta (con o sin cliente asociado).
5. Crear los detalles de la venta (productos, cantidades, precios unitarios).
6. Descontar el stock de cada producto vendido.
7. Si es a crédito: actualizar el saldo utilizado del cliente.
8. Persistir todo en base de datos (`@Transactional`).
9. Imprimir ticket de venta.

Si cualquier paso falla, se debe ejecutar rollback automático completo.

El total de la venta se calcula en el dominio — nunca se recibe desde la interfaz de usuario.

### 6.5 Gestión de Caja

- Solo puede haber una caja abierta a la vez.
- No se puede procesar ninguna venta sin caja abierta.
- Al abrir caja: registrar fecha, hora y monto inicial.
- Al cerrar caja: calcular total de ventas del período, registrar monto final, cambiar estado a CERRADA y generar backup automático del archivo SQLite.

### 6.6 Registro de Compras de Inventario

El módulo de compras permite registrar las adquisiciones que realizan los propietarios para reabastecer el inventario. Es la base del control contable del negocio.

**Gestión de Proveedores:**
- Crear, editar y consultar proveedores antes de registrar una compra.
- Campos del proveedor: nombre *(obligatorio)*, NIT *(opcional)*, celular *(opcional)*, dirección *(opcional)*, porcentaje de ganancia *(obligatorio — define el margen base que se aplica a sus productos)*.
- Un proveedor puede estar asociado a múltiples compras históricas.
- Cuando se modifica el `porcentajeGanancia` de un proveedor, el sistema recalcula automáticamente el `precioVenta` de todos los productos que tienen ese proveedor como principal.

**Gestión del proveedor principal por producto:**
- Un producto puede haber sido comprado a múltiples proveedores en distintas ocasiones.
- El ADMIN elige manualmente cuál proveedor es el "proveedor principal" de cada producto. Esta elección determina qué porcentaje base se usa para calcular el `precioVenta`.
- Al registrar una compra, el sistema NO cambia automáticamente el proveedor principal del producto; ese cambio es siempre una decisión explícita del ADMIN.
- La pantalla de edición del producto debe mostrar un selector del proveedor principal con todos los proveedores a los que ese producto ha sido comprado históricamente.

**Registro de Compra y actualización de precios:**
- Registrar una compra con: fecha, proveedor, número de factura o referencia y lista de productos adquiridos.
- Por cada producto en la compra se registra: cantidad comprada y precio de compra unitario.
- El subtotal de cada ítem y el total general de la compra se calculan automáticamente en el dominio — nunca se ingresan desde la UI.
- Al confirmar la compra, dentro de la misma transacción atómica el sistema:
  1. Incrementa el stock de cada producto.
  2. Actualiza el `precioCompra` del producto con el valor del ítem.
  3. Recalcula el `precioVenta` usando el porcentaje del **proveedor principal actual** del producto (no necesariamente el de esta compra).
- Solo el rol ADMIN puede registrar compras y gestionar proveedores.
- Regla: no se permite registrar una compra con cantidad cero o negativa en ningún ítem.

### 6.7 Impresión de Tickets

- Al finalizar cada venta se debe imprimir automáticamente un ticket en impresora térmica.
- El ticket debe incluir: nombre del negocio, fecha y hora, lista de productos, cantidades, precios unitarios, total y método de pago.

---

## 7. Requerimientos No Funcionales

| Categoría | Requisito |
|-----------|-----------|
| **Disponibilidad** | 100% offline — no depende de internet |
| **Consistencia** | Todas las operaciones críticas son transaccionales |
| **Seguridad** | Autenticación obligatoria, contraseñas hasheadas, roles diferenciados |
| **Escalabilidad** | Arquitectura limpia que permita incorporar multi-caja o nube sin reescribir el núcleo |
| **Mantenibilidad** | Código limpio, separación de capas, sin lógica en controladores UI |
| **Compatibilidad** | Windows (instalación local vía jpackage) |
| **Resiliencia** | Backup automático de SQLite al cerrar caja |

---

## 8. Modelo de Dominio

### Entidades Principales

**Producto**
- `id`, `codigoBarras`, `nombre`, `precioVenta`, `precioCompra`, `proveedorPrincipal`, `ajusteProducto` *(decimal, por defecto 0)*, `stock`, `activo`
- Fórmula de precio: `precioVenta = precioCompra × (1 + (proveedorPrincipal.porcentajeGanancia + ajusteProducto) / 100)`
- `ajusteProducto` permite aumentar o reducir el margen del proveedor para este producto específico sin afectar al proveedor ni a otros productos. Ej: proveedor tiene 30%, `ajusteProducto = +10` → margen efectivo 40%; `ajusteProducto = -5` → margen efectivo 25%.
- Al cambiar el `proveedorPrincipal`, el `ajusteProducto` se reinicia a `0` y el precio se recalcula con el porcentaje base del nuevo proveedor.
- Regla: el margen efectivo total (`porcentajeGananciaProveedor + ajusteProducto`) debe ser mayor que cero.
- Regla: stock nunca puede ser negativo.

**Cliente**
- `id`, `nombre`, `cedula`, `celular`, `direccion`, `montoCredito` *(opcional)*, `plazoPago` *(15 o 30 días, opcional)*, `saldoUtilizado`, `activo`
- Regla: si `montoCredito` es nulo o cero, el cliente no puede comprar a crédito.
- Regla: `saldoUtilizado` no puede superar `montoCredito`.
- Regla: `plazoPago` solo aplica si `montoCredito` está definido.

**Venta**
- `id`, `fecha`, `total`, `metodoPago`, `cliente` *(opcional — null si es venta directa)*, `usuario`, `caja`, `estado`, `listaDetalles`
- Regla: total siempre calculado en dominio; no se recibe desde UI.
- Regla: si `metodoPago` es CREDITO, `cliente` es obligatorio.

**DetalleVenta**
- `producto`, `cantidad`, `precioUnitario`, `subtotal`

**Caja**
- `fechaApertura`, `fechaCierre`, `montoInicial`, `montoFinal`, `estado (ABIERTA / CERRADA)`
- Regla: solo una caja puede estar abierta en cualquier momento.

**Usuario**
- `username`, `passwordHash`, `rol (ADMIN / CAJERO)`, `activo`

**Proveedor**
- `id`, `nombre` *(obligatorio)*, `nit` *(opcional)*, `celular` *(opcional)*, `direccion` *(opcional)*, `porcentajeGanancia` *(obligatorio — número decimal, ej: 30.0 representa el 30%)*, `activo`
- Regla: nombre y `porcentajeGanancia` son campos requeridos.
- Regla: `porcentajeGanancia` debe ser mayor que cero.
- Efecto: al modificar `porcentajeGanancia`, el sistema recalcula automáticamente el `precioVenta` de todos los productos que tienen este proveedor como principal, aplicando el `ajusteProducto` individual de cada uno.

**Compra** *(módulo contable de inventario)*
- `id`, `fecha`, `proveedor`, `numeroFactura` *(referencia o número de factura)*, `total`, `usuario`, `listaDetalles`
- Regla: solo usuarios ADMIN pueden registrar compras.
- Regla: el total se calcula automáticamente en el dominio sumando los subtotales de cada DetalleCompra — nunca se ingresa desde la UI.

**DetalleCompra**
- `producto`, `cantidad`, `precioCompraUnitario`, `subtotal`
- Regla: cantidad debe ser mayor a cero.
- Regla: al persistir la compra, el stock de cada producto se incrementa automáticamente dentro de la misma transacción.

### Diagrama de Relaciones (Conceptual)

```
Usuario ──────────────┬──── Venta ────────── DetalleVenta ──── Producto
                      │       │                                    │
                      │     Cliente (opcional)                     │
                      │                                            │
                      └──── Compra ─────── DetalleCompra ─────────┘
                               │
                            Proveedor
```

---

## 9. Arquitectura Técnica

### Stack Tecnológico

| Componente | Tecnología |
|-----------|------------|
| Lenguaje | Java 25 LTS |
| UI Desktop | JavaFX |
| Backend interno | Spring Boot (embedded) |
| ORM | JPA + Hibernate |
| Base de datos | SQLite (local) |
| Empaquetado | jpackage |
| Seguridad | BCrypt + control de roles |
| Transacciones | Spring `@Transactional` |

### Estructura de Paquetes (Obligatoria)

```
com.mektos.pos
├── domain          → Entidades, reglas de negocio, interfaces de repositorio
├── application     → Casos de uso, servicios de aplicación
├── infrastructure  → JPA, SQLite, impresión, repositorios concretos
├── ui              → Controladores JavaFX, vistas
└── config          → Configuración Spring, seguridad
```

### Reglas de Arquitectura

- La capa `ui` no puede acceder directamente a la base de datos.
- La lógica de negocio solo reside en `domain` y `application`.
- `infrastructure` solo contiene detalles técnicos (JPA, SQLite, impresión).
- `domain` no debe depender de Spring ni de ningún framework externo.
- No se permite lógica de negocio en controladores JavaFX.

---

## 10. Plan de Desarrollo por Fases

| Fase | Descripción | Entregable |
|------|-------------|-----------|
| **Fase 1** | Configuración base del proyecto | Proyecto Gradle + Spring Boot + SQLite operativo |
| **Fase 2** | Creación de entidades y repositorios | Modelo de dominio completo con JPA (incluye Cliente, Compra) |
| **Fase 3** | Implementación de servicios | Reglas de negocio con `@Transactional` (ventas, crédito, compras) |
| **Fase 4** | Desarrollo de UI | Pantallas: Login, Venta, Caja, Productos, Clientes, Proveedores, Compras |
| **Fase 5** | Integración impresora térmica | Ticket de venta impreso automáticamente |
| **Fase 6** | Validación y pruebas de seguridad | Sistema seguro y estable |
| **Fase 7** | Empaquetado con jpackage | Instalador ejecutable para Windows |

---

## 11. Criterios de Aceptación

- [ ] El sistema no permite iniciar sesión sin credenciales válidas.
- [ ] Un cajero no puede acceder a funcionalidades de administración.
- [ ] No se puede registrar una venta sin caja abierta.
- [ ] No se puede vender un producto con stock insuficiente.
- [ ] El total de la venta es calculado por el sistema, no ingresado por el usuario.
- [ ] Un fallo en cualquier paso de la venta revierte todos los cambios (rollback).
- [ ] Solo puede haber una caja abierta a la vez.
- [ ] Al cerrar caja se genera automáticamente un backup del archivo SQLite.
- [ ] Las contraseñas no se almacenan en texto plano.
- [ ] El sistema funciona completamente sin conexión a internet.
- [ ] No se puede asignar una venta a crédito si el cliente no tiene crédito aprobado.
- [ ] El saldo utilizado de un cliente no puede superar su monto de crédito.
- [ ] No se puede crear un proveedor sin nombre o sin porcentaje de ganancia.
- [ ] El `precioVenta` se calcula correctamente usando `precioCompra × (1 + (% proveedor + ajuste producto) / 100)`.
- [ ] El `ajusteProducto` puede ser positivo o negativo, pero el margen efectivo total nunca puede ser cero o negativo.
- [ ] El `precioVenta` se recalcula automáticamente al registrar una compra con nuevo `precioCompra`.
- [ ] El `precioVenta` se recalcula automáticamente al modificar el `porcentajeGanancia` del proveedor principal.
- [ ] Al cambiar el proveedor principal de un producto, el `ajusteProducto` se reinicia a `0`.
- [ ] El ADMIN puede cambiar el proveedor principal de un producto de forma independiente a las compras registradas.
- [ ] El registro de una compra NO cambia automáticamente el proveedor principal del producto.
- [ ] El registro de una compra incrementa el stock y actualiza precios de forma transaccional.
- [ ] Solo el rol ADMIN puede registrar compras de inventario y gestionar proveedores.
- [ ] El `precioCompra` de un producto nunca es visible en la pantalla de ventas.

---

## 12. Riesgos y Mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|------------|
| Corrupción de base de datos SQLite | Baja | Alto | Backup automático al cerrar caja |
| Falla de impresora en medio de una venta | Media | Medio | La venta se guarda aunque el ticket falle; reimpresión disponible |
| Error de stock desincronizado | Baja | Alto | Descuento/incremento de stock dentro de `@Transactional` con rollback |
| Acceso no autorizado al sistema | Baja | Alto | Login obligatorio + BCrypt + control de roles |
| Dependencia de drivers de impresora | Media | Medio | Fase 5 dedicada exclusivamente a integración y pruebas |
| Cliente excediendo límite de crédito | Media | Medio | Validación en dominio antes de procesar la venta |
| Registro de compra sin actualizar stock | Baja | Alto | Incremento de stock dentro de la misma transacción `@Transactional` |
| Precio de venta incorrecto por cambio de proveedor | Media | Medio | El proveedor principal es elegido explícitamente por el ADMIN; la compra no lo cambia automáticamente |

---

## 13. Glosario

| Término | Definición |
|---------|------------|
| **PorcentajeGanancia** | Margen base definido en el proveedor que se aplica sobre el `precioCompra` para calcular el `precioVenta` |
| **AjusteProducto** | Valor decimal (positivo o negativo) que modifica el margen del proveedor para un producto específico sin afectar al proveedor ni a otros productos |
| **MargenEfectivo** | Porcentaje real aplicado al producto: `porcentajeGananciaProveedor + ajusteProducto` |
| **ProveedorPrincipal** | Proveedor elegido por el ADMIN para un producto, cuyo porcentaje base se usa para calcular el `precioVenta` |
| **POS** | Point of Sale — Sistema de Punto de Venta |
| **Caja** | Sesión de trabajo de un cajero con monto inicial y final |
| **DetalleVenta** | Línea individual de un producto dentro de una venta |
| **Proveedor** | Empresa o persona de quien se adquieren productos para el inventario |
| **Compra** | Registro de adquisición de productos para reabastecer inventario |
| **DetalleCompra** | Línea individual de un producto dentro de una compra |
| **Crédito** | Modalidad de pago donde el cliente paga en un plazo posterior (15 o 30 días) |
| **SaldoUtilizado** | Monto de crédito ya consumido por el cliente en ventas pendientes de pago |
| **MontoCredito** | Límite máximo de crédito aprobado para un cliente |
| **BCrypt** | Algoritmo criptográfico para hasheo seguro de contraseñas |
| **@Transactional** | Anotación de Spring que garantiza atomicidad en operaciones de base de datos |
| **jpackage** | Herramienta de Java para empaquetar aplicaciones como instaladores nativos |
| **Clean Architecture** | Patrón de diseño que separa el dominio de los detalles técnicos |

---

*Documento preparado como referencia base para el desarrollo de MEKTOS POS v1.0.*
