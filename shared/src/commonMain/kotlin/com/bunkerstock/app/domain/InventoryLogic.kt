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
import com.bunkerstock.app.platform.BunkerDate
import com.bunkerstock.app.platform.formatOneDecimal
import com.bunkerstock.app.platform.todayBunkerDate
import com.bunkerstock.app.platform.urlDecodeUtf8
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object InventoryLogic {

    const val BATCH_QR_PER_PAGE = 24 // 4 cols x 6 rows (~30mm QRs per A4 sheet)

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun batchQrPageCount(itemCount: Int): Int =
        if (itemCount <= 0) 0 else ceil(itemCount.toDouble() / BATCH_QR_PER_PAGE).toInt()

    fun relativeDateString(daysOffset: Long, today: BunkerDate = todayBunkerDate()): String =
        today.plusDays(daysOffset).toIso()

    fun getDefaultDemoData(today: BunkerDate = todayBunkerDate()): List<InventoryItem> = listOf(
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
        today: BunkerDate = todayBunkerDate(),
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

        val expDate = BunkerDate.parseIso(item.expiryDate.trim())
            ?: return ExpiryInfo(
                status = ExpiryStatus.NONE,
                daysLeft = Long.MAX_VALUE,
                formattedDate = notApplicableLabel
            )

        val daysLeft = today.daysUntil(expDate)
        val formatted = expDate.toDisplayDdMmYyyy()

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
        today: BunkerDate = todayBunkerDate(),
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
        categoryFilter: String?,
        statusFilter: StatusFilter,
        sortOption: SortOption,
        today: BunkerDate = todayBunkerDate(),
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
        categories: Set<String>,
        statuses: Set<StatusFilter>,
        sortOption: SortOption,
        today: BunkerDate = todayBunkerDate(),
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

    data class Gs1ParsedCode(
        val barcode: String,
        val expiryIso: String? = null,
        val batch: String? = null,
        val isGs1DigitalLink: Boolean = false
    )

    private fun parseGs1YyMmDd(yyStr: String, mmStr: String, ddStr: String): String? {
        val yy = yyStr.toIntOrNull() ?: return null
        val mm = mmStr.toIntOrNull() ?: return null
        val dd = ddStr.toIntOrNull() ?: return null
        if (mm !in 1..12) return null
        val year = 2000 + yy
        val maxDay = BunkerDate.lengthOfMonth(year, mm)
        val safeDay = if (dd == 0) maxDay else dd.coerceIn(1, maxDay)
        return BunkerDate.ofOrNull(year, mm, safeDay)?.toIso()
    }

    private fun normalizeGtinToBarcode(gtin: String): String {
        val digits = gtin.filter { it.isDigit() }
        // Standard GS1 GTIN-14 for EAN-13 has a leading '0' (e.g. 08421610357742 -> 8421610357742)
        return if (digits.length == 14 && digits.startsWith("0")) digits.substring(1) else digits
    }

    /**
     * Parses GS1 Digital Link URLs (e.g. Mercadona:
     * https://qrtrack.mercadona.es/01/08421610357742/10/323414199?17=261127&16=261123)
     * as well as classic GS1-128 / GS1 DataMatrix codes into a normalized barcode (EAN-13/GTIN),
     * ISO expiry date, and batch/lot number — completely offline without hitting retailer redirects.
     */
    fun parseGs1Code(rawCode: String): Gs1ParsedCode {
        val trimmed = rawCode.trim()
        val cleaned = trimmed.replace("\u001D", " ")

        // 1. GS1 Digital Link URI (https://.../01/<gtin>[/10/<lot>][?17=YYMMDD&15=YYMMDD&16=YYMMDD&10=<lot>])
        val dlGtinMatch = Regex("(?:https?://[^\\s?#]+)?/01/(\\d{8,14})(?:/|\\?|#|$)", RegexOption.IGNORE_CASE).find(trimmed)
        if (dlGtinMatch != null) {
            val normalizedEan = normalizeGtinToBarcode(dlGtinMatch.groupValues[1])
            val pathPart = trimmed.substringBefore("?").substringBefore("#")
            val queryPart = trimmed.substringAfter("?", "").substringBefore("#")
            val queryMap = LinkedHashMap<String, String>()
            if (queryPart.isNotEmpty()) {
                queryPart.split("&").forEach { pair ->
                    val eq = pair.indexOf('=')
                    if (eq > 0) {
                        val k = pair.substring(0, eq).trim()
                        val v = urlDecodeUtf8(pair.substring(eq + 1).trim())
                        if (k.isNotEmpty() && v.isNotEmpty()) queryMap[k] = v
                    }
                }
            }

            // Expiry AI priority: 17 (Expiration Date) -> 15 (Best Before) -> 16 (Sell-By)
            var expiryIso: String? = null
            for (ai in listOf("17", "15", "16")) {
                val rawDate = queryMap[ai]
                    ?: Regex("/$ai/(\\d{6})(?:/|$)").find(pathPart)?.groupValues?.get(1)
                if (rawDate != null && rawDate.length == 6 && rawDate.all { it.isDigit() }) {
                    expiryIso = parseGs1YyMmDd(rawDate.substring(0, 2), rawDate.substring(2, 4), rawDate.substring(4, 6))
                    if (expiryIso != null) break
                }
            }

            val batch = Regex("/10/([^/?#]+)").find(pathPart)?.groupValues?.get(1)?.let(::urlDecodeUtf8)
                ?: queryMap["10"]

            return Gs1ParsedCode(
                barcode = normalizedEan.ifBlank { trimmed },
                expiryIso = expiryIso,
                batch = batch?.take(32),
                isGs1DigitalLink = true
            )
        }

        // 2. Classic GS1-128 / GS1 DataMatrix with (01)/(17)/(15)/(10) or unbracketed 01+17
        var barcode = trimmed
        val parenGtin = Regex("\\(01\\)(\\d{8,14})").find(cleaned)
        if (parenGtin != null) {
            barcode = normalizeGtinToBarcode(parenGtin.groupValues[1])
        } else {
            val rawGtin = Regex("^01(\\d{14})(?:17|15|10)").find(cleaned)
            if (rawGtin != null) {
                barcode = normalizeGtinToBarcode(rawGtin.groupValues[1])
            }
        }

        var expiryIso: String? = null
        var batch: String? = null

        val gs1Paren = Regex("\\((?:17|15)\\)(\\d{2})(\\d{2})(\\d{2})").find(cleaned)
        val gs1Raw = if (gs1Paren == null) {
            Regex("(?:^01\\d{14}|^\\x1d?)(?:17|15)(\\d{2})(\\d{2})(\\d{2})").find(cleaned)
        } else null

        val match = gs1Paren ?: gs1Raw
        if (match != null) {
            expiryIso = parseGs1YyMmDd(match.groupValues[1], match.groupValues[2], match.groupValues[3])
        }

        if (expiryIso == null) {
            val iso = Regex("(?:EXP|CAD|BEST|VENC)[:=\\s-]*(\\d{4}-\\d{2}-\\d{2})", RegexOption.IGNORE_CASE).find(cleaned)
            if (iso != null) expiryIso = iso.groupValues[1]
        }

        val lotParen = Regex("\\(10\\)([A-Za-z0-9._-]{1,20})").find(cleaned)
        if (lotParen != null) {
            batch = lotParen.groupValues[1]
        } else {
            val lotExplicit = Regex("(?:LOT|LOTE|BATCH)[:=\\s-]*([A-Za-z0-9._-]{1,20})", RegexOption.IGNORE_CASE).find(cleaned)
            if (lotExplicit != null) batch = lotExplicit.groupValues[1]
        }

        return Gs1ParsedCode(barcode = barcode, expiryIso = expiryIso, batch = batch, isGs1DigitalLink = false)
    }

    fun extractGs1ExpiryAndBatch(rawCode: String): Pair<String?, String?> {
        val parsed = parseGs1Code(rawCode)
        return parsed.expiryIso to parsed.batch
    }

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
        today: BunkerDate = todayBunkerDate(),
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
            formatOneDecimal(rounded)
        }
    }

    fun normalizeWithBatches(item: InventoryItem): InventoryItem {
        if (item.batches.isEmpty()) return item
        val totalQty = item.batches.sumOf { it.qty.coerceAtLeast(0.0) }
        val soonest = item.batches
            .mapNotNull { b ->
                BunkerDate.parseIso(b.expiryDate.trim())?.let { it to b.expiryDate.trim() }
            }
            .minByOrNull { it.first }
            ?.second
            ?: item.expiryDate
        return item.copy(
            qty = (totalQty * 100.0).roundToInt() / 100.0,
            expiryDate = if (item.noExpiry) "" else soonest
        )
    }

    fun isLikelyRawCode(input: String): Boolean {
        val t = input.trim()
        if (t.isEmpty()) return false
        if (t.startsWith("bunkerstock://", ignoreCase = true) || t.contains("#join?", ignoreCase = true)) return true
        if (t.startsWith("{") && t.endsWith("}")) return true
        if (t.contains("|")) return true
        if (Regex("^BNK-[A-Za-z0-9_-]+$", RegexOption.IGNORE_CASE).matches(t)) return true
        if (Regex("^\\d{6,18}$").matches(t)) return true
        if (t.contains("(01)") || t.contains("(17)")) return true
        if (Regex("(?:https?://[^\\s?#]+)?/01/\\d{8,14}(?:/|\\?|#|$)", RegexOption.IGNORE_CASE).containsMatchIn(t)) return true
        return false
    }
}
