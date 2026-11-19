package com.bunkerstock.app

import com.bunkerstock.app.data.ExpiryStatus
import com.bunkerstock.app.data.InventoryItem
import com.bunkerstock.app.data.ItemCategory
import com.bunkerstock.app.data.ItemUnit
import com.bunkerstock.app.data.SortOption
import com.bunkerstock.app.data.StatusFilter
import com.bunkerstock.app.domain.InventoryLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InventoryLogicTest {

    private val fixedToday = LocalDate.of(2026, 11, 19)

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
        assertEquals(63.0, metrics.waterLiters, 0.01) // 12*5L + 3 boxes*1L
        assertEquals(10, metrics.waterDays) // floor(63 / 6)
        assertEquals(47.0, metrics.foodUnits, 0.01) // 8 + 15 + 10 + 14
        assertEquals(67, metrics.healthyPercent) // round((4 + 2) / 9 * 100)
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
        assertEquals(10, csv.lines().size) // header + 9 items
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
        assertEquals("item-3", alerts.first().id) // expired (-5d) first
    }
}
