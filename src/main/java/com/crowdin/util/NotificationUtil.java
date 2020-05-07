package com.crowdin.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

public final class NotificationUtil {

    private static final NotificationGroup GROUP_DISPLAY_ID_INFO =
            new NotificationGroup("Crowdin",
                    NotificationDisplayType.BALLOON, true);

    private NotificationUtil() {
        throw new UnsupportedOperationException();
    }

    public static void showInformationMessage(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(message, NotificationType.INFORMATION);
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            Notifications.Bus.notify(notification, projects[0]);
        });
    }

    public static void showErrorMessage(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(message, NotificationType.ERROR);
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            Notifications.Bus.notify(notification, projects[0]);
        });
    }

    public static void showWarningMessage(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(message, NotificationType.WARNING);
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            Notifications.Bus.notify(notification, projects[0]);
        });
    }
}
