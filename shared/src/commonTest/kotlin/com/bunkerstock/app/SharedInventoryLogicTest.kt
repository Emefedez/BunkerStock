package com.bunkerstock.app

import com.bunkerstock.app.data.ExpiryBatch
import com.bunkerstock.app.data.ExpiryStatus
import com.bunkerstock.app.data.FamilyGroupConfig
import com.bunkerstock.app.data.FamilySyncClient
import com.bunkerstock.app.data.FamilySyncSnapshot
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.OpenFoodFactsClient
import com.bunkerstock.app.data.SortOption
import com.bunkerstock.app.data.StatusFilter
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.BunkerDate
import com.bunkerstock.app.ui.theme.EnglishStrings
import com.bunkerstock.app.ui.theme.SpanishStrings
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedInventoryLogicTest {

    private val fixedToday = BunkerDate(2026, 11, 19)

    @Test
    fun bunkerDateArithmeticIsExact() {
        assertEquals("2026-11-19", fixedToday.toIso())
        assertEquals("19/11/2026", fixedToday.toDisplayDdMmYyyy())
        assertEquals(4, fixedToday.dayOfWeekIso()) // 2026-11-19 is Thursday
        assertEquals(BunkerDate(2026, 11, 14), fixedToday.plusDays(-5))
        assertEquals(-5L, fixedToday.daysUntil(BunkerDate(2026, 11, 14)))
        assertEquals(29, BunkerDate.lengthOfMonth(2024, 2))
        assertEquals(28, BunkerDate.lengthOfMonth(2026, 2))
        assertEquals(BunkerDate(2026, 11, 19), BunkerDate.parseIso("2026-11-19"))
    }

    @Test
    fun expiryThresholdsAndCompactLabelsWork() {
        val expired = InventoryItem(id = "1", name = "A", expiryDate = "2026-11-13")
        val todayItem = InventoryItem(id = "2", name = "B", expiryDate = "2026-11-19")
        val critical = InventoryItem(id = "3", name = "C", expiryDate = "2026-12-03")
        val durable = InventoryItem(id = "4", name = "D", noExpiry = true)

        val expInfo = InventoryLogic.getExpiryInfo(expired, fixedToday)
        assertEquals(ExpiryStatus.EXPIRED, expInfo.status)
        assertEquals(-6L, expInfo.daysLeft)
        assertEquals("Hace 6 días", SpanishStrings.expiryCompactLabel(expInfo))
        assertEquals("6 days ago", EnglishStrings.expiryCompactLabel(expInfo))

        val todayInfo = InventoryLogic.getExpiryInfo(todayItem, fixedToday)
        assertEquals(ExpiryStatus.CRITICAL, todayInfo.status)
        assertEquals("Caduca HOY", SpanishStrings.expiryCompactLabel(todayInfo))

        val critInfo = InventoryLogic.getExpiryInfo(critical, fixedToday)
        assertEquals("En 14 días", SpanishStrings.expiryCompactLabel(critInfo))

        val noneInfo = InventoryLogic.getExpiryInfo(durable, fixedToday)
        assertEquals("Sin caducidad", SpanishStrings.expiryCompactLabel(noneInfo))
    }

    @Test
    fun dashboardMetricsAndMultiFilterWork() {
        val demo = InventoryLogic.getDefaultDemoData(fixedToday)
        val metrics = InventoryLogic.computeDashboardMetrics(demo, persons = 2, litersPerPersonDay = 3, today = fixedToday)
        assertEquals(9, metrics.totalRefs)
        assertEquals(71.0, metrics.totalUnits, 0.01)
        assertEquals(1, metrics.expiredCount)
        assertEquals(2, metrics.criticalCount)
        assertEquals(63.0, metrics.waterLiters, 0.01)
        assertEquals(10, metrics.waterDays)
        assertEquals(67, metrics.healthyPercent)

        val multi = InventoryLogic.filterAndSortMulti(
            items = demo,
            query = "",
            categories = setOf(ItemCategory.FOOD.storageKey, ItemCategory.WATER.storageKey),
            statuses = setOf(StatusFilter.EXPIRED, StatusFilter.CRITICAL),
            sortOption = SortOption.EXPIRY_ASC,
            today = fixedToday
        )
        assertEquals(2, multi.size)
    }

    @Test
    fun openFoodFactsParserExtractsKcalFallbackAndSearchHits() {
        val json = """
            {
              "code": "8410076472199",
              "product_name_es": "Atún Claro",
              "brands": ["Calvo"],
              "nutriscore_grade": "b",
              "nova_group": 2,
              "nutriments": {
                "energy_100g": 836.8,
                "proteins_100g": 24.0,
                "fat_100g": 10.0,
                "salt_100g": 1.1
              }
            }
        """.trimIndent()
        val obj = InventoryLogic.json.parseToJsonElement(json) as JsonObject
        val parsed = OpenFoodFactsClient.parseProductObject(obj)
        assertNotNull(parsed)
        assertEquals("Atún Claro (Calvo)", parsed.name)
        assertEquals("8410076472199", parsed.barcode)
        assertEquals(200.0, parsed.nutrition?.energyKcal ?: 0.0, 0.5)
        assertEquals("B", parsed.nutrition?.nutriScoreGrade)
        assertTrue(parsed.nutrition?.safetyFlags?.contains("HIGH_PROTEIN") == true)
    }

    @Test
    fun inviteLinksParseBothAndroidSchemeAndWebHash() {
        val cfg = FamilyGroupConfig(roomId = "bs_1234", secretKey = "secKey99", groupName = "Casa")
        val androidLink = FamilySyncClient.buildInviteLink(cfg)
        assertTrue(androidLink.startsWith("bunkerstock://join?"))
        val parsedAndroid = FamilySyncClient.parseInviteLink(androidLink)
        assertEquals("bs_1234", parsedAndroid?.roomId)
        assertEquals("secKey99", parsedAndroid?.secretKey)
        assertEquals("Casa", parsedAndroid?.groupName)

        val webLink = FamilySyncClient.buildInviteLink(cfg, webBaseUrl = "https://emefedez.github.io/BunkerStock")
        assertTrue(webLink.startsWith("https://emefedez.github.io/BunkerStock/#join?"))
        val parsedWeb = FamilySyncClient.parseInviteLink(webLink)
        assertEquals("bs_1234", parsedWeb?.roomId)
        assertEquals("secKey99", parsedWeb?.secretKey)
        assertEquals("Casa", parsedWeb?.groupName)
    }

    @Test
    fun familySyncEncryptDecryptAndMergeWork() = runTest {
        val secret = FamilySyncClient.generateSecretKey()
        val snap = FamilySyncSnapshot(
            groupName = "Casa",
            items = listOf(
                InventoryItem(id = "a", name = "Agua 5L", qty = 10.0, updatedAt = 200L),
                InventoryItem(id = "b", name = "Atún", qty = 4.0, updatedAt = 100L)
            ),
            deletedIds = mapOf("old" to 150L),
            updatedAt = 200L
        )
        val cipher = FamilySyncClient.encryptSnapshot(snap, secret)
        assertTrue(cipher.startsWith("v2:"))
        assertTrue(!cipher.contains("Agua"))
        val decrypted = FamilySyncClient.decryptSnapshot(cipher, secret)
        assertNotNull(decrypted)
        assertEquals("Casa", decrypted.groupName)
        assertEquals(2, decrypted.items.size)

        val local = listOf(
            InventoryItem(id = "a", name = "Agua 5L", qty = 6.0, imageUri = "/data/custom.jpg", updatedAt = 150L),
            InventoryItem(id = "old", name = "Viejo", qty = 1.0, updatedAt = 100L)
        )
        val (merged, _) = FamilySyncClient.mergeSnapshots(local, emptyMap(), snap)
        assertEquals(2, merged.size)
        val a = merged.first { it.id == "a" }
        assertEquals(10.0, a.qty, 0.01)
        assertEquals("/data/custom.jpg", a.imageUri)
    }

    @Test
    fun smartSearchClassifierDistinguishesBarcodesFromNames() {
        assertTrue(InventoryLogic.isLikelyRawCode("8410076472199"))
        assertTrue(InventoryLogic.isLikelyRawCode("BNK-MED-005"))
        assertTrue(InventoryLogic.isLikelyRawCode("bunkerstock://join?r=1&k=2"))
        assertTrue(InventoryLogic.isLikelyRawCode("Agua|Agua|2028-01-01|123"))
        assertTrue(!InventoryLogic.isLikelyRawCode("atun claro"))
        assertTrue(!InventoryLogic.isLikelyRawCode("leche semidesnatada"))
    }

    @Test
    fun gs1DigitalLinkMercadonaQrParsesCleanEanExpiryAndBatch() {
        val mercadonaQr = "https://qrtrack.mercadona.es/01/08421610357742/10/323414199?17=261127&16=261123"
        assertTrue(InventoryLogic.isLikelyRawCode(mercadonaQr))
        val parsed = InventoryLogic.parseGs1Code(mercadonaQr)
        assertTrue(parsed.isGs1DigitalLink)
        assertEquals("8421610357742", parsed.barcode)
        assertEquals("2026-11-27", parsed.expiryIso)
        assertEquals("323414199", parsed.batch)

        // Classic GS1-128 with parentheses also normalizes GTIN-14 -> EAN-13
        val classic = InventoryLogic.parseGs1Code("(01)08410076472199(17)270515(10)LOTE42B")
        assertEquals("8410076472199", classic.barcode)
        assertEquals("2027-05-15", classic.expiryIso)
        assertEquals("LOTE42B", classic.batch)
    }

    @Test
    fun batchesAndStructuredQrWork() {
        val batched = InventoryItem(
            id = "batched-1",
            name = "Conservas",
            category = ItemCategory.OTHER.storageKey,
            qty = 1.0,
            expiryDate = "2028-01-01",
            batches = listOf(
                ExpiryBatch(id = "b1", qty = 4.0, expiryDate = "2027-06-15"),
                ExpiryBatch(id = "b2", qty = 6.0, expiryDate = "2027-02-10")
            )
        )
        val norm = InventoryLogic.normalizeWithBatches(batched)
        assertEquals(10.0, norm.qty, 0.01)
        assertEquals("2027-02-10", norm.expiryDate)
        assertNull(InventoryLogic.parseStructuredQr("8410000810015"))
    }
}
