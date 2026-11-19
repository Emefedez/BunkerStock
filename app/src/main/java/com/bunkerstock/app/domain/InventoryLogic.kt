package com.bunkerstock.app.domain

import com.bunkerstock.app.data.DashboardMetrics
import com.bunkerstock.app.data.ExpiryInfo
import com.bunkerstock.app.data.ExpiryStatus
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemPrefill
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.data.SortOption
import com.bunkerstock.app.data.StatusFilter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.roundToInt

object InventoryLogic {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun relativeDateString(daysOffset: Long, today: LocalDate = LocalDate.now()): String =
        today.plusDays(daysOffset).format(isoFormatter)

    fun getDefaultDemoData(today: LocalDate = LocalDate.now()): List<InventoryItem> = listOf(
        InventoryItem(
            id = "item-1",
            barcode = "8410000810015",
            name = "Garrafas Agua Mineral 5L",
            category = ItemCategory.WATER.storageKey,
            qty = 12.0,
            unit = ItemUnit.JUGS_5L.storageKey,
            noExpiry = false,
            expiryDate = relativeDateString(180, today),
            location = "Palé Suelo - Zona A",
            notes = "60 litros totales en envase PET reforzado"
        ),
        InventoryItem(
            id = "item-2",
            barcode = "8410076472199",
            name = "Latas de Atún en Aceite (Pack 6)",
            category = ItemCategory.FOOD.storageKey,
            qty = 8.0,
            unit = ItemUnit.PACKS.storageKey,
            noExpiry = false,
            expiryDate = relativeDateString(14, today),
            location = "Estantería 1 - Balda B",
            notes = "Rotar urgentemente al consumo diario"
        ),
        InventoryItem(
            id = "item-3",
            barcode = "8480000142310",
            name = "Barritas Energéticas de Avena y Miel",
            category = ItemCategory.FOOD.storageKey,
            qty = 15.0,
            unit = ItemUnit.UNITS.storageKey,
            noExpiry = false,
            expiryDate = relativeDateString(-5, today),
            location = "Mochila 72h",
            notes = "Revisar y reemplazar lote caducado"
        ),
        InventoryItem(
            id = "item-4",
            barcode = "8410128001024",
            name = "Arroz Blanco Envasado al Vacío",
            category = ItemCategory.FOOD.storageKey,
            qty = 10.0,
            unit = ItemUnit.KG.storageKey,
            noExpiry = false,
            expiryDate = relativeDateString(320, today),
            location = "Estantería 1 - Balda A",
            notes = "Bolsas de 1kg selladas"
        ),
        InventoryItem(
            id = "item-5",
            barcode = "BNK-MED-005",
            name = "Botiquín Primeros Auxilios Trauma",
            category = ItemCategory.MATERIALS.storageKey,
            qty = 2.0,
            unit = ItemUnit.UNITS.storageKey,
            noExpiry = false,
            expiryDate = relativeDateString(55, today),
            location = "Armario Médico - Puerta",
            notes = "Verificar gasas estériles y antiséptico"
        ),
        InventoryItem(
            id = "item-6",
            barcode = "5010724526101",
            name = "Pastillas Potabilizadoras de Agua",
            category = ItemCategory.WATER.storageKey,
            qty = 3.0,
            unit = ItemUnit.BOXES.storageKey,
            noExpiry = false,
            expiryDate = relativeDateString(410, today),
            location = "Caja Estanca Azul",
            notes = "Cada caja trata hasta 100 litros"
        ),
        InventoryItem(
            id = "item-7",
            barcode = "BNK-TOOL-007",
            name = "Linterna LED Dinamo + Solar",
            category = ItemCategory.TOOLS.storageKey,
            qty = 3.0,
            unit = ItemUnit.UNITS.storageKey,
            noExpiry = true,
            expiryDate = "",
            location = "Panel Herramientas",
            notes = "Comprobar carga cada 3 meses"
        ),
        InventoryItem(
            id = "item-8",
            barcode = "5000394075122",
            name = "Pilas Alcalinas AA (Blíster 12)",
            category = ItemCategory.TOOLS.storageKey,
            qty = 4.0,
            unit = ItemUnit.PACKS.storageKey,
            noExpiry = false,
            expiryDate = relativeDateString(22, today),
            location = "Cajón Energía 2",
            notes = "Para radios y frontales"
        ),
        InventoryItem(
            id = "item-9",
            barcode = "8480000221053",
            name = "Lentejas Cocidas en Conserva",
            category = ItemCategory.FOOD.storageKey,
            qty = 14.0,
            unit = ItemUnit.CANS.storageKey,
            noExpiry = false,
            expiryDate = relativeDateString(72, today),
            location = "Estantería 2 - Balda C",
            notes = "Listas para consumir sin cocción"
        )
    )

    fun getExpiryInfo(
        item: InventoryItem,
        today: LocalDate = LocalDate.now(),
        notApplicableLabel: String = "No aplica"
    ): ExpiryInfo {
        if (item.noExpiry || item.expiryDate.isBlank()) {
            return ExpiryInfo(
                status = ExpiryStatus.NONE,
                daysLeft = Long.MAX_VALUE,
                formattedDate = notApplicableLabel
            )
        }

        val expDate = runCatching { LocalDate.parse(item.expiryDate.trim(), isoFormatter) }
            .getOrNull() ?: return ExpiryInfo(
            status = ExpiryStatus.NONE,
            daysLeft = Long.MAX_VALUE,
            formattedDate = notApplicableLabel
        )

        val daysLeft = ChronoUnit.DAYS.between(today, expDate)
        val formatted = expDate.format(displayFormatter)

        val status = when {
            daysLeft < 0L -> ExpiryStatus.EXPIRED
            daysLeft <= 30L -> ExpiryStatus.CRITICAL
            daysLeft <= 90L -> ExpiryStatus.WARNING
            else -> ExpiryStatus.OPTIMAL
        }

        return ExpiryInfo(
            status = status,
            daysLeft = daysLeft,
            formattedDate = formatted,
            isToday = daysLeft == 0L
        )
    }

    fun computeDashboardMetrics(
        items: List<InventoryItem>,
        persons: Int = 2,
        litersPerPersonDay: Int = 3,
        today: LocalDate = LocalDate.now()
    ): DashboardMetrics {
        val totalRefs = items.size
        var totalUnits = 0.0
        var expiredCount = 0
        var criticalCount = 0
        var warningCount = 0
        var optimalOrNoneCount = 0
        var waterLiters = 0.0
        var foodUnits = 0.0

        for (item in items) {
            val qty = item.qty.coerceAtLeast(0.0)
            totalUnits += qty
            when (getExpiryInfo(item, today).status) {
                ExpiryStatus.EXPIRED -> expiredCount++
                ExpiryStatus.CRITICAL -> criticalCount++
                ExpiryStatus.WARNING -> warningCount++
                ExpiryStatus.OPTIMAL, ExpiryStatus.NONE -> optimalOrNoneCount++
            }

            if (item.category.equals(ItemCategory.WATER.storageKey, ignoreCase = true)) {
                waterLiters += when {
                    item.unit.equals(ItemUnit.LITERS.storageKey, ignoreCase = true) -> qty
                    item.unit.equals(ItemUnit.JUGS_5L.storageKey, ignoreCase = true) -> qty * 5.0
                    else -> qty
                }
            }

            if (item.category.equals(ItemCategory.FOOD.storageKey, ignoreCase = true)) {
                foodUnits += qty
            }
        }

        val dailyConsumption = (persons.coerceAtLeast(1) * litersPerPersonDay.coerceAtLeast(1)).toDouble()
        val waterDays = floor(waterLiters / dailyConsumption).toInt()
        val healthyPercent = if (totalRefs == 0) {
            0
        } else {
            (((optimalOrNoneCount + warningCount).toDouble() / totalRefs.toDouble()) * 100.0).roundToInt()
        }

        return DashboardMetrics(
            totalRefs = totalRefs,
            totalUnits = totalUnits,
            expiredCount = expiredCount,
            criticalCount = criticalCount,
            warningCount = warningCount,
            optimalOrNoneCount = optimalOrNoneCount,
            waterLiters = waterLiters,
            waterDays = waterDays,
            foodUnits = foodUnits,
            healthyPercent = healthyPercent
        )
    }

    fun filterAndSort(
        items: List<InventoryItem>,
        query: String,
        categoryFilter: String?, // null means ALL
        statusFilter: StatusFilter,
        sortOption: SortOption,
        today: LocalDate = LocalDate.now()
    ): List<InventoryItem> {
        val q = query.trim().lowercase()
        val filtered = items.filter { item ->
            val info = getExpiryInfo(item, today)
            val matchesQuery = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.barcode.lowercase().contains(q) ||
                item.location.lowercase().contains(q) ||
                item.notes.lowercase().contains(q)

            val matchesCategory = categoryFilter == null ||
                item.category.equals(categoryFilter, ignoreCase = true)

            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL -> true
                StatusFilter.ALERT_ALL ->
                    info.status == ExpiryStatus.EXPIRED || info.status == ExpiryStatus.CRITICAL
                StatusFilter.EXPIRED -> info.status == ExpiryStatus.EXPIRED
                StatusFilter.CRITICAL -> info.status == ExpiryStatus.CRITICAL
                StatusFilter.WARNING -> info.status == ExpiryStatus.WARNING
                StatusFilter.OPTIMAL -> info.status == ExpiryStatus.OPTIMAL
                StatusFilter.NONE -> info.status == ExpiryStatus.NONE
            }

            matchesQuery && matchesCategory && matchesStatus
        }

        return when (sortOption) {
            SortOption.EXPIRY_ASC -> filtered.sortedBy { getExpiryInfo(it, today).daysLeft }
            SortOption.EXPIRY_DESC -> filtered.sortedByDescending {
                val d = getExpiryInfo(it, today).daysLeft
                if (d == Long.MAX_VALUE) Long.MIN_VALUE else d
            }
            SortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOption.QTY_ASC -> filtered.sortedBy { it.qty }
            SortOption.QTY_DESC -> filtered.sortedByDescending { it.qty }
        }
    }

    fun parseStructuredQr(rawText: String): ItemPrefill? {
        val trimmed = rawText.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            runCatching {
                val obj = json.parseToJsonElement(trimmed) as? JsonObject ?: return null
                fun str(vararg keys: String): String {
                    for (k in keys) {
                        val v = obj[k]?.jsonPrimitive?.contentOrNull?.trim()
                        if (!v.isNullOrEmpty()) return v
                    }
                    return ""
                }
                val name = str("name", "nombre")
                if (name.isNotEmpty()) {
                    val catRaw = str("category", "categoria")
                    val category = ItemCategory.entries
                        .firstOrNull { it.storageKey.equals(catRaw, ignoreCase = true) }
                        ?.storageKey ?: ItemCategory.FOOD.storageKey
                    return ItemPrefill(
                        barcode = str("code", "barcode", "codigo"),
                        name = name,
                        category = category,
                        expiryDate = str("expiry", "expiryDate", "caducidad").ifEmpty { null },
                        notes = str("notes", "notas").ifEmpty { "Importado desde QR" }
                    )
                }
            }
        }

        if (trimmed.contains("|")) {
            val parts = trimmed.split("|").map { it.trim() }
            if (parts.size >= 2 && parts[0].isNotEmpty()) {
                val matchedCat = ItemCategory.entries
                    .firstOrNull { it.storageKey.equals(parts[1], ignoreCase = true) }
                    ?.storageKey ?: ItemCategory.FOOD.storageKey
                val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
                val possibleDate = parts.getOrNull(2)?.takeIf { dateRegex.matches(it) }
                return ItemPrefill(
                    barcode = parts.getOrNull(3).orEmpty(),
                    name = parts[0],
                    category = matchedCat,
                    expiryDate = possibleDate,
                    notes = "Importado desde etiqueta QR"
                )
            }
        }

        return null
    }

    fun exportToJson(items: List<InventoryItem>): String = json.encodeToString(items)

    fun importFromJson(raw: String): Result<List<InventoryItem>> = runCatching {
        json.decodeFromString<List<InventoryItem>>(raw)
    }

    fun exportToCsv(
        items: List<InventoryItem>,
        today: LocalDate = LocalDate.now(),
        noExpiryText: String = "No caduca",
        statusLabelProvider: (ExpiryInfo) -> String
    ): String {
        val headers = listOf(
            "Codigo_Barras_QR",
            "Nombre",
            "Categoria",
            "Cantidad",
            "Unidad",
            "Caducidad",
            "Estado",
            "Dias_Restantes",
            "Ubicacion",
            "Notas"
        )
        fun clean(v: String): String = "\"${v.replace("\"", "\"\"")}\""

        val rows = items.map { item ->
            val info = getExpiryInfo(item, today)
            listOf(
                clean(item.barcode),
                clean(item.name),
                clean(item.category),
                formatQty(item.qty),
                clean(item.unit),
                clean(if (item.noExpiry) noExpiryText else item.expiryDate),
                clean(statusLabelProvider(info)),
                if (info.daysLeft == Long.MAX_VALUE) "N/A" else info.daysLeft.toString(),
                clean(item.location),
                clean(item.notes)
            ).joinToString(";")
        }

        return "\uFEFF" + (listOf(headers.joinToString(";")) + rows).joinToString("\n")
    }

    fun formatQty(value: Double): String {
        val rounded = (value * 100.0).roundToInt() / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", rounded)
        }
    }
}
