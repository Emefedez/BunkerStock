package com.bunkerstock.app.domain

import com.bunkerstock.app.data.DashboardMetrics
import com.bunkerstock.app.data.ExpiryBatch
import com.bunkerstock.app.data.ExpiryInfo
import com.bunkerstock.app.data.ExpiryStatus
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemPrefill
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.data.ProductNutrition
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
            notes = "Rotar urgentemente al consumo diario",
            nutrition = ProductNutrition(
                nutriScoreGrade = "B",
                novaGroup = 2,
                healthScore = 76,
                energyKcal = 192.0,
                proteinGrams = 24.5,
                carbsGrams = 0.0,
                sugarsGrams = 0.0,
                fatGrams = 10.5,
                satFatGrams = 1.8,
                fiberGrams = 0.0,
                saltGrams = 1.1,
                safetyFlags = listOf("HIGH_PROTEIN")
            ),
            batches = listOf(
                ExpiryBatch(id = "b-2a", qty = 3.0, expiryDate = relativeDateString(14, today), note = "Lote antiguo"),
                ExpiryBatch(id = "b-2b", qty = 5.0, expiryDate = relativeDateString(95, today), note = "Compra reciente")
            )
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
            notes = "Revisar y reemplazar lote caducado",
            nutrition = ProductNutrition(
                nutriScoreGrade = "C",
                novaGroup = 3,
                healthScore = 54,
                energyKcal = 418.0,
                proteinGrams = 7.2,
                carbsGrams = 62.0,
                sugarsGrams = 24.0,
                fatGrams = 14.8,
                satFatGrams = 3.2,
                fiberGrams = 5.4,
                saltGrams = 0.35,
                safetyFlags = listOf("HIGH_SUGAR", "HIGH_FIBER")
            )
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
            notes = "Bolsas de 1kg selladas",
            nutrition = ProductNutrition(
                nutriScoreGrade = "A",
                novaGroup = 1,
                healthScore = 90,
                energyKcal = 356.0,
                proteinGrams = 7.1,
                carbsGrams = 79.0,
                sugarsGrams = 0.2,
                fatGrams = 0.7,
                satFatGrams = 0.2,
                fiberGrams = 1.4,
                saltGrams = 0.01
            )
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
            notes = "Listas para consumir sin cocción",
            nutrition = ProductNutrition(
                nutriScoreGrade = "A",
                novaGroup = 1,
                healthScore = 88,
                energyKcal = 84.0,
                proteinGrams = 6.2,
                carbsGrams = 11.4,
                sugarsGrams = 0.5,
                fatGrams = 0.4,
                satFatGrams = 0.1,
                fiberGrams = 4.9,
                saltGrams = 0.65,
                safetyFlags = listOf("HIGH_FIBER")
            )
        )
    )

    fun getExpiryInfo(
        item: InventoryItem,
        today: LocalDate = LocalDate.now(),
        notApplicableLabel: String = "No aplica",
        expiredDays: Int = 0,
        criticalDays: Int = 30,
        warningDays: Int = 90
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
            daysLeft < expiredDays.toLong() -> ExpiryStatus.EXPIRED
            daysLeft <= criticalDays.toLong() -> ExpiryStatus.CRITICAL
            daysLeft <= warningDays.toLong() -> ExpiryStatus.WARNING
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
        today: LocalDate = LocalDate.now(),
        expiredDays: Int = 0,
        criticalDays: Int = 30,
        warningDays: Int = 90
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
            when (getExpiryInfo(item, today, expiredDays = expiredDays, criticalDays = criticalDays, warningDays = warningDays).status) {
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
        today: LocalDate = LocalDate.now(),
        expiredDays: Int = 0,
        criticalDays: Int = 30,
        warningDays: Int = 90
    ): List<InventoryItem> = filterAndSortMulti(
        items = items,
        query = query,
        categories = categoryFilter?.let { setOf(it) } ?: emptySet(),
        statuses = if (statusFilter == StatusFilter.ALL) emptySet() else setOf(statusFilter),
        sortOption = sortOption,
        today = today,
        expiredDays = expiredDays,
        criticalDays = criticalDays,
        warningDays = warningDays
    )

    fun filterAndSortMulti(
        items: List<InventoryItem>,
        query: String,
        categories: Set<String>, // empty means ALL
        statuses: Set<StatusFilter>, // empty means ALL
        sortOption: SortOption,
        today: LocalDate = LocalDate.now(),
        expiredDays: Int = 0,
        criticalDays: Int = 30,
        warningDays: Int = 90
    ): List<InventoryItem> {
        val q = query.trim().lowercase()
        val effectiveStatuses = statuses.filterNot { it == StatusFilter.ALL }.toSet()
        val filtered = items.filter { item ->
            val info = getExpiryInfo(item, today, expiredDays = expiredDays, criticalDays = criticalDays, warningDays = warningDays)
            val matchesQuery = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.barcode.lowercase().contains(q) ||
                item.location.lowercase().contains(q) ||
                item.notes.lowercase().contains(q)

            val matchesCategory = categories.isEmpty() ||
                categories.any { item.category.equals(it, ignoreCase = true) }

            val matchesStatus = effectiveStatuses.isEmpty() || effectiveStatuses.any { st ->
                when (st) {
                    StatusFilter.ALL -> true
                    StatusFilter.ALERT_ALL ->
                        info.status == ExpiryStatus.EXPIRED || info.status == ExpiryStatus.CRITICAL
                    StatusFilter.EXPIRED -> info.status == ExpiryStatus.EXPIRED
                    StatusFilter.CRITICAL -> info.status == ExpiryStatus.CRITICAL
                    StatusFilter.WARNING -> info.status == ExpiryStatus.WARNING
                    StatusFilter.OPTIMAL -> info.status == ExpiryStatus.OPTIMAL
                    StatusFilter.NONE -> info.status == ExpiryStatus.NONE
                }
            }

            matchesQuery && matchesCategory && matchesStatus
        }

        return when (sortOption) {
            SortOption.EXPIRY_ASC -> filtered.sortedBy {
                getExpiryInfo(it, today, expiredDays = expiredDays, criticalDays = criticalDays, warningDays = warningDays).daysLeft
            }
            SortOption.EXPIRY_DESC -> filtered.sortedByDescending {
                val d = getExpiryInfo(it, today, expiredDays = expiredDays, criticalDays = criticalDays, warningDays = warningDays).daysLeft
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

    /**
     * Extracts expiry date (YYYY-MM-DD) and optional batch/lot from GS1-128 / GS1 DataMatrix
     * Application Identifiers: (17)YYMMDD or 17YYMMDD, (10)BATCH, or explicit EXP/CAD patterns.
     */
    fun extractGs1ExpiryAndBatch(rawCode: String): Pair<String?, String?> {
        val cleaned = rawCode.trim().replace("\u001D", " ")
        var expiryIso: String? = null
        var batch: String? = null

        // Pattern 1: (17)YYMMDD or AI 17 after GTIN (01)14digits17YYMMDD
        val gs1Paren = Regex("\\(17\\)(\\d{2})(\\d{2})(\\d{2})").find(cleaned)
        val gs1Raw = if (gs1Paren == null) {
            Regex("(?:^01\\d{14}|^\\x1d?)17(\\d{2})(\\d{2})(\\d{2})").find(cleaned)
        } else null

        val match = gs1Paren ?: gs1Raw
        if (match != null) {
            val yy = match.groupValues[1].toIntOrNull()
            val mm = match.groupValues[2].toIntOrNull()
            val dd = match.groupValues[3].toIntOrNull()
            if (yy != null && mm != null && mm in 1..12 && dd != null) {
                val year = 2000 + yy
                val safeDay = if (dd == 0) {
                    runCatching { LocalDate.of(year, mm, 1).lengthOfMonth() }.getOrDefault(28)
                } else {
                    dd.coerceIn(1, runCatching { LocalDate.of(year, mm, 1).lengthOfMonth() }.getOrDefault(28))
                }
                expiryIso = runCatching { LocalDate.of(year, mm, safeDay).format(isoFormatter) }.getOrNull()
            }
        }

        // Pattern 2: Explicit ISO date inside QR/text
        if (expiryIso == null) {
            val iso = Regex("(?:EXP|CAD|BEST|VENC)[:=\\s-]*(\\d{4}-\\d{2}-\\d{2})", RegexOption.IGNORE_CASE).find(cleaned)
            if (iso != null) expiryIso = iso.groupValues[1]
        }

        // Pattern 3: GS1 Batch/Lot (10)
        val lotParen = Regex("\\(10\\)([A-Za-z0-9._-]{1,20})").find(cleaned)
        if (lotParen != null) {
            batch = lotParen.groupValues[1]
        } else {
            val lotExplicit = Regex("(?:LOT|LOTE|BATCH)[:=\\s-]*([A-Za-z0-9._-]{1,20})", RegexOption.IGNORE_CASE).find(cleaned)
            if (lotExplicit != null) batch = lotExplicit.groupValues[1]
        }

        return expiryIso to batch
    }

    /**
     * Estimates a realistic pantry shelf life in days from product name/category tags
     * when a retail barcode (EAN-13) does not encode an expiration date.
     */
    fun estimateShelfLifeDays(name: String, category: String, nutritionFlags: List<String> = emptyList()): Long {
        val text = "$name $category".lowercase()
        return when {
            Regex("conserva|lata|atún|atun|sardina|legumbre|lenteja|garbanzo|alubia|canned|tin").containsMatchIn(text) -> 730L
            Regex("agua|water|potabiliz").containsMatchIn(text) -> 365L
            Regex("arroz|pasta|harina|avena|cereal|macarr|espagueti|rice|flour|oats").containsMatchIn(text) -> 365L
            Regex("leche|uht|milk").containsMatchIn(text) -> 150L
            Regex("yogur|queso|lácteo|dairy|cheese|huevo|egg").containsMatchIn(text) -> 21L
            Regex("carne|pollo|pescado|salmón|meat|fish|chicken").containsMatchIn(text) -> 5L
            Regex("fruta|verdura|ensalada|fruit|vegetable").containsMatchIn(text) -> 10L
            Regex("barrita|galleta|chocolate|miel|snack|biscuit").containsMatchIn(text) -> 180L
            category.equals(ItemCategory.WATER.storageKey, ignoreCase = true) -> 365L
            category.equals(ItemCategory.MATERIALS.storageKey, ignoreCase = true) -> 365L
            else -> 180L
        }
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

    /**
     * When an item tracks multiple expiry batches ("tandadas"), syncs total qty to the sum
     * of batch quantities and sets primary expiryDate to the soonest valid batch date.
     */
    fun normalizeWithBatches(item: InventoryItem): InventoryItem {
        if (item.batches.isEmpty()) return item
        val totalQty = item.batches.sumOf { it.qty.coerceAtLeast(0.0) }
        val soonest = item.batches
            .mapNotNull { b ->
                runCatching { LocalDate.parse(b.expiryDate.trim(), isoFormatter) }.getOrNull()?.let { it to b.expiryDate.trim() }
            }
            .minByOrNull { it.first }
            ?.second
            ?: item.expiryDate
        return item.copy(
            qty = (totalQty * 100.0).roundToInt() / 100.0,
            expiryDate = if (item.noExpiry) "" else soonest
        )
    }
}
