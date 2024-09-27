package ru.zplugin.zplugin.presentation

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class Notifier {

    fun notifyError(project: Project, message: String) {
        NotificationGroupManager
            .getInstance()
            .getNotificationGroup("Z notification")
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }
}
