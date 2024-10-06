package ru.zplugin.zplugin.presentation

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

internal class ZSvgLongPathNotifier {

    fun notifyInfo(project: Project, message: String) {
        notify(
            project = project,
            message = message,
            type = NotificationType.INFORMATION
        )
    }

    fun notifyError(project: Project, message: String) {
        notify(
            project = project,
            message = message,
            type = NotificationType.ERROR
        )
    }

    private fun notify(
        project: Project,
        message: String,
        type: NotificationType
    ) {
        NotificationGroupManager
            .getInstance()
            .getNotificationGroup("Z notification")
            .createNotification(message, type)
            .notify(project)
    }
}
