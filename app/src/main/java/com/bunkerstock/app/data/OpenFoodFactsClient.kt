package com.bunkerstock.app.data

import com.bunkerstock.app.domain.InventoryLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

object OpenFoodFactsClient {

    private val barcodeRegex = Regex("^\\d{8,14}$")
    private val waterRegex = Regex("agua|water|mineral water|eau", RegexOption.IGNORE_CASE)

    suspend fun lookup(barcode: String): OpenFoodFactsResult? = withContext(Dispatchers.IO) {
        if (!barcodeRegex.matches(barcode)) return@withContext null

        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2800
                readTimeout = 2800
                setRequestProperty("User-Agent", "BunkerStock-Android/2.4")
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
            OpenFoodFactsResult(
                name = fullName,
                category = if (isWater) ItemCategory.WATER.storageKey else ItemCategory.FOOD.storageKey,
                unit = if (isWater) ItemUnit.LITERS.storageKey else ItemUnit.UNITS.storageKey
            )
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
