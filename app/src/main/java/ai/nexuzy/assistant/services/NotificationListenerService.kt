package ai.nexuzy.assistant.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * NotificationListenerService: Reads active notifications.
 * AI can summarize or act on notifications (e.g., read WhatsApp messages).
 * 
 * ⚠️ User must grant permission in: Settings > Notifications > NexuzyAI
 */
class NotificationListenerService : NotificationListenerService() {

    companion object {
        val recentNotifications = mutableListOf<String>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let {
            val pkg = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title") ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            if (title.isNotEmpty() || text.isNotEmpty()) {
                recentNotifications.add(0, "[$pkg] $title: $text")
                if (recentNotifications.size > 20) recentNotifications.removeLast()
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
