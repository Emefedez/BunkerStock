package com.bunkerstock.app.data

import com.bunkerstock.app.domain.InventoryLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

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

    suspend fun lookup(barcode: String): OpenFoodFactsResult? = withContext(Dispatchers.IO) {
        if (!barcodeRegex.matches(barcode)) return@withContext null

        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "BunkerStock-Android/3.2")
            }
            if (conn.responseCode !in 200..299) return@withContext null

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = InventoryLogic.json.parseToJsonElement(body) as? JsonObject ?: return@withContext null
            val status = root["status"]?.jsonPrimitive?.intOrNull ?: 0
            if (status != 1) return@withContext null

            val product = root["product"] as? JsonObject ?: return@withContext null
            fun pStr(key: String): String = product[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

            val productName = pStr("product_name_es")
                .ifEmpty { pStr("product_name") }
                .ifEmpty { pStr("generic_name_es") }
                .ifEmpty { pStr("generic_name") }
            if (productName.isEmpty()) return@withContext null

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

            // Parse YsiFoods-style nutrition & health/safety scoring
            val gradeRaw = pStr("nutriscore_grade").uppercase()
            val grade = gradeRaw.takeIf { it in listOf("A", "B", "C", "D", "E") }
            val nova = product["nova_group"]?.jsonPrimitive?.intOrNull
            val additivesN = product["additives_n"]?.jsonPrimitive?.intOrNull
            val imageUrl = pStr("image_front_url")
                .ifEmpty { pStr("image_url") }
                .ifEmpty { pStr("image_front_small_url") }
                .ifEmpty { null }

            val nutriments = product["nutriments"] as? JsonObject
            fun nutr(key: String): Double? =
                nutriments?.get(key)?.jsonPrimitive?.doubleOrNull?.takeIf { it >= 0.0 && it.isFinite() }

            val kcal = nutr("energy-kcal_100g")
            val protein = nutr("proteins_100g")
            val carbs = nutr("carbohydrates_100g")
            val sugars = nutr("sugars_100g")
            val fat = nutr("fat_100g")
            val satFat = nutr("saturated-fat_100g")
            val fiber = nutr("fiber_100g")
            val salt = nutr("salt_100g")

            val hasAnyNutrition = grade != null || nova != null || kcal != null || protein != null || sugars != null
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
            } else {
                null
            }

            OpenFoodFactsResult(
                name = fullName,
                category = if (isWater) ItemCategory.WATER.storageKey else ItemCategory.FOOD.storageKey,
                unit = if (isWater) ItemUnit.LITERS.storageKey else ItemUnit.UNITS.storageKey,
                nutrition = nutrition
            )
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
