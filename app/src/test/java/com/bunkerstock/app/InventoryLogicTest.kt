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
import com.bunkerstock.app.data.UpdateChecker
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.platform.BunkerDate
import com.bunkerstock.app.scanner.QrCodeUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLogicTest {

    private val fixedToday = BunkerDate(2026, 11, 19)

    @Test
    fun expiryInfoThresholdsMatchOriginal() {
        val expired = InventoryItem(id = "1", name = "A", expiryDate = "2026-11-14")
        val todayItem = InventoryItem(id = "2", name = "B", expiryDate = "2026-11-19")
        val critical = InventoryItem(id = "3", name = "C", expiryDate = "2026-12-19")
        val warning = InventoryItem(id = "4", name = "D", expiryDate = "2027-01-15")
        val optimal = InventoryItem(id = "5", name = "E", expiryDate = "2027-06-01")
        val durable = InventoryItem(id = "6", name = "F", noExpiry = true)

        assertEquals(ExpiryStatus.EXPIRED, InventoryLogic.getExpiryInfo(expired, fixedToday).status)
        assertEquals(-5L, InventoryLogic.getExpiryInfo(expired, fixedToday).daysLeft)

        val todayInfo = InventoryLogic.getExpiryInfo(todayItem, fixedToday)
        assertEquals(ExpiryStatus.CRITICAL, todayInfo.status)
        assertTrue(todayInfo.isToday)

        assertEquals(ExpiryStatus.CRITICAL, InventoryLogic.getExpiryInfo(critical, fixedToday).status)
        assertEquals(ExpiryStatus.WARNING, InventoryLogic.getExpiryInfo(warning, fixedToday).status)
        assertEquals(ExpiryStatus.OPTIMAL, InventoryLogic.getExpiryInfo(optimal, fixedToday).status)
        assertEquals(ExpiryStatus.NONE, InventoryLogic.getExpiryInfo(durable, fixedToday).status)
    }

    @Test
    fun dashboardMetricsMatchesDemoCalculations() {
        val demo = InventoryLogic.getDefaultDemoData(fixedToday)
        val metrics = InventoryLogic.computeDashboardMetrics(demo, persons = 2, litersPerPersonDay = 3, today = fixedToday)

        assertEquals(9, metrics.totalRefs)
        assertEquals(71.0, metrics.totalUnits, 0.01)
        assertEquals(1, metrics.expiredCount)
        assertEquals(2, metrics.criticalCount)
        assertEquals(2, metrics.warningCount)
        assertEquals(4, metrics.optimalOrNoneCount)
        assertEquals(63.0, metrics.waterLiters, 0.01)
        assertEquals(10, metrics.waterDays)
        assertEquals(47.0, metrics.foodUnits, 0.01)
        assertEquals(67, metrics.healthyPercent)
    }

    @Test
    fun structuredQrParsesJsonAndPipeFormats() {
        val jsonQr = """{"nombre":"Lentejas","categoria":"Alimentos","caducidad":"2027-05-10","codigo":"BNK-123"}"""
        val parsedJson = InventoryLogic.parseStructuredQr(jsonQr)
        assertNotNull(parsedJson)
        assertEquals("Lentejas", parsedJson?.name)
        assertEquals(ItemCategory.FOOD.storageKey, parsedJson?.category)
        assertEquals("2027-05-10", parsedJson?.expiryDate)
        assertEquals("BNK-123", parsedJson?.barcode)

        val pipeQr = "Agua 5L|Agua|2028-01-01|8410000810015"
        val parsedPipe = InventoryLogic.parseStructuredQr(pipeQr)
        assertNotNull(parsedPipe)
        assertEquals("Agua 5L", parsedPipe?.name)
        assertEquals(ItemCategory.WATER.storageKey, parsedPipe?.category)
        assertEquals("2028-01-01", parsedPipe?.expiryDate)
        assertEquals("8410000810015", parsedPipe?.barcode)

        assertNull(InventoryLogic.parseStructuredQr("8410000810015"))
    }

    @Test
    fun jsonRoundTripAndCsvExportWork() {
        val demo = InventoryLogic.getDefaultDemoData(fixedToday)
        val json = InventoryLogic.exportToJson(demo)
        val restored = InventoryLogic.importFromJson(json).getOrThrow()
        assertEquals(demo.size, restored.size)
        assertEquals(demo.first().name, restored.first().name)

        val csv = InventoryLogic.exportToCsv(demo, fixedToday) { it.status.name }
        assertTrue(csv.startsWith("\uFEFFCodigo_Barras_QR;Nombre;"))
        assertEquals(10, csv.lines().size)
    }

    @Test
    fun filterAndSortOrdersBySoonestExpiry() {
        val demo = InventoryLogic.getDefaultDemoData(fixedToday)
        val alerts = InventoryLogic.filterAndSort(
            items = demo,
            query = "",
            categoryFilter = null,
            statusFilter = StatusFilter.ALERT_ALL,
            sortOption = SortOption.EXPIRY_ASC,
            today = fixedToday
        )
        assertEquals(3, alerts.size)
        assertEquals("item-3", alerts.first().id)
    }

    @Test
    fun gs1ExpiryAndShelfLifeEstimationWork() {
        val (exp, batch) = InventoryLogic.extractGs1ExpiryAndBatch("(01)08410076472199(17)270515(10)LOTE42B")
        assertEquals("2027-05-15", exp)
        assertEquals("LOTE42B", batch)

        assertEquals(730L, InventoryLogic.estimateShelfLifeDays("Latas de Atún en Conserva", "Alimentos"))
        assertEquals(365L, InventoryLogic.estimateShelfLifeDays("Agua Mineral 5L", "Agua"))
        assertEquals(21L, InventoryLogic.estimateShelfLifeDays("Yogur Natural", "Alimentos"))
    }

    @Test
    fun healthScoreAndUpdateVersionComparisonWork() {
        val (goodScore, goodFlags) = OpenFoodFactsClient.computeHealthScore(
            nutriScoreGrade = "A",
            novaGroup = 1,
            sugars = 2.0,
            satFat = 0.5,
            salt = 0.2,
            protein = 18.0,
            fiber = 7.0,
            additivesCount = 0
        )
        assertTrue(goodScore >= 90)
        assertTrue(goodFlags.contains("HIGH_PROTEIN"))
        assertTrue(goodFlags.contains("HIGH_FIBER"))

        val (badScore, badFlags) = OpenFoodFactsClient.computeHealthScore(
            nutriScoreGrade = "E",
            novaGroup = 4,
            sugars = 32.0,
            satFat = 9.0,
            salt = 2.1,
            protein = 2.0,
            fiber = 0.5,
            additivesCount = 8
        )
        assertTrue(badScore <= 25)
        assertTrue(badFlags.contains("ULTRA_PROCESSED"))
        assertTrue(badFlags.contains("HIGH_SUGAR"))

        assertTrue(UpdateChecker.isNewerVersion("v2.6", "2.5"))
        assertTrue(!UpdateChecker.isNewerVersion("v2.5", "2.5"))
    }

    @Test
    fun familySyncEncryptionInviteAndMergeWork() = runBlocking {
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
        assertTrue(!cipher.contains("Agua"))
        val decrypted = FamilySyncClient.decryptSnapshot(cipher, secret)
        assertNotNull(decrypted)
        assertEquals("Casa", decrypted?.groupName)
        assertEquals(2, decrypted?.items?.size)

        val cfg = FamilyGroupConfig(roomId = "room123", secretKey = secret, groupName = "Casa")
        val link = FamilySyncClient.buildInviteLink(cfg)
        val parsed = FamilySyncClient.parseInviteLink(link)
        assertEquals("room123", parsed?.roomId)
        assertEquals(secret, parsed?.secretKey)
        assertEquals("Casa", parsed?.groupName)

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
    fun customExpiryThresholdsMultiFilterAndBatchQrPagingWork() {
        val demo = InventoryLogic.getDefaultDemoData(fixedToday)
        val item5 = demo.first { it.id == "item-5" }
        val item1 = demo.first { it.id == "item-1" }
        assertEquals(
            ExpiryStatus.CRITICAL,
            InventoryLogic.getExpiryInfo(item5, fixedToday, expiredDays = 0, criticalDays = 60, warningDays = 200).status
        )
        assertEquals(
            ExpiryStatus.WARNING,
            InventoryLogic.getExpiryInfo(item1, fixedToday, expiredDays = 0, criticalDays = 60, warningDays = 200).status
        )
        val metrics = InventoryLogic.computeDashboardMetrics(
            items = demo,
            today = fixedToday,
            expiredDays = 0,
            criticalDays = 60,
            warningDays = 200
        )
        assertEquals(3, metrics.criticalCount)

        val multi = InventoryLogic.filterAndSortMulti(
            items = demo,
            query = "",
            categories = setOf(ItemCategory.FOOD.storageKey, ItemCategory.WATER.storageKey),
            statuses = setOf(StatusFilter.EXPIRED, StatusFilter.CRITICAL),
            sortOption = SortOption.EXPIRY_ASC,
            today = fixedToday
        )
        assertEquals(2, multi.size)

        assertEquals(0, QrCodeUtils.batchPageCount(0))
        assertEquals(1, QrCodeUtils.batchPageCount(1))
        assertEquals(1, QrCodeUtils.batchPageCount(24))
        assertEquals(2, QrCodeUtils.batchPageCount(25))
        assertEquals(3, QrCodeUtils.batchPageCount(49))

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
        assertEquals(ItemCategory.OTHER, ItemCategory.fromKey("Otro"))
    }
}
