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


    private static final String TITLE = "Crowdin";

    private NotificationUtil() {
        throw new UnsupportedOperationException();
    }

    public static void showInformationMessage(String message) {
        showMessage(message, NotificationType.INFORMATION);
    }

    public static void showErrorMessage(String message) {
        showMessage(message, NotificationType.ERROR);
    }

    public static void showWarningMessage(String message) {
        showMessage(message, NotificationType.WARNING);
    }

    private static void showMessage(String message, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(TITLE, message, type, null);
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            Notifications.Bus.notify(notification, projects[0]);
        });
    }
}
