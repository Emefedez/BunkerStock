package com.bunkerstock.app.data

import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.PlatformHttp
import com.bunkerstock.app.platform.isWebPlatform
import com.bunkerstock.app.platform.urlEncodeUtf8
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

object OpenFoodFactsClient {

    private val barcodeRegex = Regex("^\\d{8,14}$")
    private val waterRegex = Regex("agua|water|mineral water|eau", RegexOption.IGNORE_CASE)

    fun computeHealthScore(
        nutriScoreGrade: String?,
        novaGroup: Int?,
        sugars: Double?,
        satFat: Double?,
        salt: Double?,
        protein: Double?,
        fiber: Double?,
        additivesCount: Int?
    ): Pair<Int, List<String>> {
        var score = when (nutriScoreGrade?.uppercase()) {
            "A" -> 90
            "B" -> 76
            "C" -> 58
            "D" -> 38
            "E" -> 20
            else -> 60
        }

        val flags = mutableListOf<String>()

        if (novaGroup != null) {
            when (novaGroup) {
                1 -> score += 6
                3 -> score -= 4
                4 -> {
                    score -= 14
                    flags.add("ULTRA_PROCESSED")
                }
            }
        }

        if (sugars != null && sugars >= 15.0) {
            score -= 10
            flags.add("HIGH_SUGAR")
        }
        if (satFat != null && satFat >= 5.0) {
            score -= 8
            flags.add("HIGH_SAT_FAT")
        }
        if (salt != null && salt >= 1.5) {
            score -= 8
            flags.add("HIGH_SALT")
        }
        if (additivesCount != null && additivesCount >= 5) {
            score -= 6
        }
        if (protein != null && protein >= 10.0) {
            score += 6
            flags.add("HIGH_PROTEIN")
        }
        if (fiber != null && fiber >= 5.0) {
            score += 6
            flags.add("HIGH_FIBER")
        }

        return score.coerceIn(0, 100) to flags
    }

    /**
     * Parses a product object from Open Food Facts (works across v0 product, v2 search, cgi/search.pl,
     * and search.openfoodfacts.org Search-a-licious hits). Recycles the exact YsiFoods nutrition/health model.
     */
    fun parseProductObject(product: JsonObject, fallbackBarcode: String = ""): OpenFoodFactsResult? {
        fun pStr(key: String): String {
            val el = product[key] ?: return ""
            return when (el) {
                is JsonPrimitive -> el.contentOrNull?.trim().orEmpty()
                is JsonArray -> el.firstOrNull()?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                else -> ""
            }
        }

        val code = pStr("code").ifEmpty { fallbackBarcode }
        val productName = pStr("product_name_es")
            .ifEmpty { pStr("product_name") }
            .ifEmpty { pStr("generic_name_es") }
            .ifEmpty { pStr("generic_name") }
        if (productName.isEmpty()) return null

        val brand = pStr("brands").split(",").firstOrNull()?.trim().orEmpty()
        val fullName = if (brand.isNotEmpty() && !productName.contains(brand, ignoreCase = true)) {
            "$productName ($brand)"
        } else {
            productName
        }

        val tags = (product["categories_tags"] as? JsonArray)
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.joinToString(" ")
            .orEmpty()
        val combined = "${pStr("categories")} $tags $fullName"
        val isWater = waterRegex.containsMatchIn(combined)

        val gradeRaw = pStr("nutriscore_grade").uppercase()
        val grade = gradeRaw.takeIf { it in listOf("A", "B", "C", "D", "E") }
        val nova = (product["nova_group"] as? JsonPrimitive)?.intOrNull
        val additivesN = (product["additives_n"] as? JsonPrimitive)?.intOrNull
            ?: (product["additives_tags"] as? JsonArray)?.size

        val imageUrl = pStr("image_front_url")
            .ifEmpty { pStr("image_url") }
            .ifEmpty { pStr("image_front_small_url") }
            .ifEmpty { pStr("image_small_url") }
            .ifEmpty { null }

        val nutriments = product["nutriments"] as? JsonObject
        fun nutr(key: String): Double? =
            (nutriments?.get(key) as? JsonPrimitive)?.doubleOrNull?.takeIf { it >= 0.0 && it.isFinite() }

        // Handle energy-kcal_100g, energy-kcal, and kJ->kcal conversion fallback
        val kcal = nutr("energy-kcal_100g")
            ?: nutr("energy-kcal")
            ?: (nutr("energy-kj_100g") ?: nutr("energy_100g"))?.let { kj ->
                if (kj > 0.0) ((kj / 4.184) * 10.0).roundToInt() / 10.0 else 0.0
            }
        val protein = nutr("proteins_100g") ?: nutr("proteins")
        val carbs = nutr("carbohydrates_100g") ?: nutr("carbohydrates")
        val sugars = nutr("sugars_100g") ?: nutr("sugars")
        val fat = nutr("fat_100g") ?: nutr("fat")
        val satFat = nutr("saturated-fat_100g") ?: nutr("saturated-fat")
        val fiber = nutr("fiber_100g") ?: nutr("fiber")
        val salt = nutr("salt_100g") ?: nutr("salt")
            ?: nutr("sodium_100g")?.let { ((it * 2.5) * 100.0).roundToInt() / 100.0 }

        val hasAnyNutrition = grade != null || nova != null || kcal != null || protein != null || sugars != null || fat != null || carbs != null
        val nutrition = if (hasAnyNutrition) {
            val (score, flags) = computeHealthScore(
                nutriScoreGrade = grade,
                novaGroup = nova,
                sugars = sugars,
                satFat = satFat,
                salt = salt,
                protein = protein,
                fiber = fiber,
                additivesCount = additivesN
            )
            ProductNutrition(
                nutriScoreGrade = grade,
                novaGroup = nova,
                healthScore = score,
                energyKcal = kcal,
                proteinGrams = protein,
                carbsGrams = carbs,
                sugarsGrams = sugars,
                fatGrams = fat,
                satFatGrams = satFat,
                fiberGrams = fiber,
                saltGrams = salt,
                additivesCount = additivesN,
                imageUrl = imageUrl,
                safetyFlags = flags
            )
        } else if (imageUrl != null) {
            ProductNutrition(imageUrl = imageUrl)
        } else {
            null
        }

        return OpenFoodFactsResult(
            name = fullName,
            category = if (isWater) ItemCategory.WATER.storageKey else ItemCategory.FOOD.storageKey,
            unit = if (isWater) ItemUnit.LITERS.storageKey else ItemUnit.UNITS.storageKey,
            nutrition = nutrition,
            barcode = code
        )
    }

    suspend fun lookup(barcode: String): OpenFoodFactsResult? {
        val clean = barcode.trim()
        if (!barcodeRegex.matches(clean)) return null
        val headers = if (isWebPlatform) emptyMap() else mapOf("User-Agent" to "BunkerStock-Android/3.4")
        val body = PlatformHttp.getText(
            url = "https://world.openfoodfacts.org/api/v0/product/$clean.json",
            headers = headers,
            timeoutMs = 5000
        ) ?: return null

        return runCatching {
            val root = InventoryLogic.json.parseToJsonElement(body) as? JsonObject ?: return null
            val status = root["status"]?.jsonPrimitive?.intOrNull ?: 0
            if (status != 1) return null
            val product = root["product"] as? JsonObject ?: return null
            parseProductObject(product, fallbackBarcode = clean)
        }.getOrNull()
    }

    /**
     * YsiFoods-style product name search against Open Food Facts.
     * On Android: uses Search-a-licious first (fast, ES/EN ranked) with cgi/search.pl fallback.
     * On Web: uses CORS-enabled es/world.openfoodfacts.org/cgi/search.pl directly.
     */
    suspend fun searchByName(
        query: String,
        language: AppLanguage = AppLanguage.ES,
        limit: Int = 12
    ): List<OpenFoodFactsResult> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        val encoded = urlEncodeUtf8(clean)
        val fields = "code,product_name,product_name_es,generic_name,brands,categories,categories_tags,nutriscore_grade,nova_group,additives_n,additives_tags,image_front_url,image_front_small_url,image_url,image_small_url,nutriments"
        val headers = if (isWebPlatform) emptyMap() else mapOf("User-Agent" to "BunkerStock-Android/3.4")

        val urls = buildList {
            if (!isWebPlatform) {
                val lang = if (language == AppLanguage.ES) "es" else "en"
                add("https://search.openfoodfacts.org/search?q=$encoded&langs=$lang&page_size=$limit&fields=$fields")
            }
            if (language == AppLanguage.ES) {
                add("https://es.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=$limit&fields=$fields")
                add("https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=$limit&fields=$fields")
            } else {
                add("https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=$limit&fields=$fields")
                add("https://es.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=$limit&fields=$fields")
            }
        }

        for (url in urls) {
            val body = PlatformHttp.getText(url = url, headers = headers, timeoutMs = 5500) ?: continue
            val parsed = runCatching {
                val root = InventoryLogic.json.parseToJsonElement(body) as? JsonObject ?: return@runCatching emptyList()
                val arr = (root["hits"] as? JsonArray) ?: (root["products"] as? JsonArray) ?: return@runCatching emptyList()
                val seenCodes = HashSet<String>()
                arr.mapNotNull { el ->
                    val obj = el as? JsonObject ?: return@mapNotNull null
                    val hit = parseProductObject(obj) ?: return@mapNotNull null
                    val dedupKey = hit.barcode.ifBlank { hit.name.lowercase() }
                    if (seenCodes.add(dedupKey)) hit else null
                }
            }.getOrDefault(emptyList())
            if (parsed.isNotEmpty()) return parsed
        }
        return emptyList()
    }
}
