# BúnkerStock (Android)

Aplicación nativa de Android (Kotlin + Jetpack Compose + Material 3) para el **control logístico de inventario y caducidad**, con estética táctica de búnker y operación 100% offline-first.

## Características

- **Panel Táctico y Alertas**:
  - Banner descartable de alertas de rotación para artículos caducados y próximos a caducar (`≤ 30 días`) con filtro rápido *"Revisar Afectados"*.
  - 5 tarjetas KPI clicables: **Referencias**, **Caducados**, **Próx. 30 días**, **Reserva Agua** (cálculo automático de litros + días de autonomía configurables) y **Alimentos**.
  - Barra segmentada de **Estado de Vigencia del Almacén** (`% Vigente`).
- **Gestión de Inventario**:
  - Búsqueda instantánea por nombre, código de barras/QR, ubicación o notas.
  - Filtros por categoría (*Alimentos, Agua, Materiales, Herramientas*) y estado (*Caducados, Críticos, Precaución, Óptimos, Sin caducidad*).
  - Ordenación por caducidad, nombre o cantidad.
  - Ajuste rápido táctil `- / +` con objetivos de toque de `48 dp`.
  - Alta y edición con sugerencias inteligentes por categoría y selector de fecha nativo.
- **Escáner Óptico Offline (CameraX + ZXing)**:
  - Soporta `QR_CODE`, `EAN-13`, `EAN-8`, `UPC-A`, `UPC-E`, `CODE-128` y `CODE-39` sin depender de Google Play Services.
  - Selector de cámara, linterna, pitido + vibración y entrada manual.
  - Detección automática de artículos existentes (hoja rápida `+1`, `+6`, `+N`, editar), parseo de QR estructurado (`JSON` o `NOMBRE|CATEGORIA|CADUCIDAD|CODIGO`) y consulta automática a **Open Food Facts**.
- **Etiquetas QR Imprimibles**:
  - Generación automática de código `BNK-XXXXXX` si el artículo no tiene uno.
  - Vista previa blanca imprimible con impresión nativa Android (`PrintHelper`) y compartir/guardar PNG.
- **Datos y Copias de Seguridad (Storage Access Framework)**:
  - Exportar / Importar JSON, exportar CSV (UTF-8 con BOM y `;` para Excel), restaurar datos de ejemplo y vaciar almacén con confirmación.
- **Idioma y Apariencia (Ajustes)**:
  - **Español** por defecto con selector instantáneo **Español / English** en Ajustes.
  - Tema **Oscuro Búnker** (predeterminado), **Claro Táctico** o **Sistema**.
- **Diseño Android Nativo**:
  - Navegación adaptativa (`NavigationBar` en móvil, `NavigationRail` + cuadrícula de 2 columnas en tablet/plegable), edge-to-edge, Predictive Back, icono adaptativo vectorial (incluye icono monocromo Android 13+) y semántica accesible.

## Compilación

```bash
gradle testDebugUnitTest assembleDebug
```

El workflow de GitHub Actions compila y sube el artefacto `BunkerStock-v2.4-debug-apk` en cada push a `main`.
