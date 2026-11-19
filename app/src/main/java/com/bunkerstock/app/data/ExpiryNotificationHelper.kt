package com.bunkerstock.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bunkerstock.app.MainActivity
import com.bunkerstock.app.R
import com.bunkerstock.app.domain.InventoryLogic
import com.bunkerstock.app.ui.theme.stringsFor
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object ExpiryNotificationHelper {

    const val CHANNEL_ID = "bunker_expiry_alerts"
    const val EXTRA_OPEN_ALERTS = "extra_open_alerts"
    private const val NOTIFICATION_ID = 3001
    private const val WORK_NAME = "bunkerstock_expiry_check"

    fun ensureChannel(context: Context, language: AppLanguage) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val name = if (language == AppLanguage.ES) "Alertas de caducidad" else "Expiry Alerts"
        val desc = if (language == AppLanguage.ES) {
            "Avisa cuando un producto entra en rango peligroso de caducidad (≤30 días)"
        } else {
            "Notifies when an item enters the dangerous expiry range (≤30 days)"
        }
        val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = desc
        }
        manager.createNotificationChannel(channel)
    }

    fun schedulePeriodicCheck(context: Context) {
        val req = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }

    suspend fun evaluateAndNotify(context: Context, repository: BunkerRepository) {
        val settings = repository.settingsFlow.first()
        if (!settings.expiryNotificationsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val items = repository.inventoryFlow.first()
        val notifiedKeys = repository.notifiedExpiryKeysFlow.first()

        // Only confirmed items with an explicit expiry date entering CRITICAL (<=30d) or EXPIRED
        val dangerous = items.mapNotNull { item ->
            if (item.noExpiry || item.expiryDate.isBlank()) return@mapNotNull null
            val info = InventoryLogic.getExpiryInfo(
                item,
                expiredDays = settings.expiredThresholdDays,
                criticalDays = settings.criticalThresholdDays,
                warningDays = settings.warningThresholdDays
            )
            if (info.status == ExpiryStatus.CRITICAL || info.status == ExpiryStatus.EXPIRED) {
                Triple(item, info, "${item.id}:${info.status.name}")
            } else {
                null
            }
        }

        val activeKeys = dangerous.map { it.third }.toSet()
        val newlyDangerous = dangerous.filter { it.third !in notifiedKeys }

        // Keep notified set clean so if an item is later updated to a fresh date it can notify again in the future
        val updatedKeys = (notifiedKeys.intersect(activeKeys)) + newlyDangerous.map { it.third }
        if (updatedKeys != notifiedKeys) {
            repository.saveNotifiedExpiryKeys(updatedKeys)
        }

        if (newlyDangerous.isEmpty()) return

        ensureChannel(context, settings.language)
        val strings = stringsFor(settings.language)

        val title = if (settings.language == AppLanguage.ES) {
            if (newlyDangerous.size == 1) "Alerta de caducidad: ${newlyDangerous.first().first.name}"
            else "${newlyDangerous.size} productos en rango peligroso de caducidad"
        } else {
            if (newlyDangerous.size == 1) "Expiry alert: ${newlyDangerous.first().first.name}"
            else "${newlyDangerous.size} items in dangerous expiry range"
        }

        val body = newlyDangerous.take(4).joinToString("\n") { (item, info, _) ->
            "• ${item.name} (${strings.expiryLabel(info)} · ${strings.expirySublabel(info)})"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ALERTS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bunker_shield)
            .setContentTitle(title)
            .setContentText(body.lines().firstOrNull().orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}

class ExpiryCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = BunkerRepository(applicationContext)
        // If connected to a family group, pull latest state before evaluating alerts
        val family = repo.familyGroupFlow.first()
        if (family.isJoined) {
            val localItems = repo.inventoryFlow.first()
            val localDeleted = repo.deletedIdsFlow.first()
            FamilySyncClient.syncGroup(family, localItems, localDeleted).onSuccess { (items, deleted, cfg) ->
                repo.saveInventory(items)
                repo.saveDeletedIds(deleted)
                repo.saveFamilyGroup(cfg)
            }
        }
        ExpiryNotificationHelper.evaluateAndNotify(applicationContext, repo)
        return Result.success()
    }
}
