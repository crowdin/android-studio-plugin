package com.crowdin.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

public final class NotificationUtil {

    private static final NotificationGroup GROUP_DISPLAY_ID_INFO =
            new NotificationGroup("Crowdin",
                    NotificationDisplayType.BALLOON, true);


    private static final String TITLE = "Crowdin";

    private NotificationUtil() {
        throw new UnsupportedOperationException();
    }

    public static void showInformationMessage(Project project, String message) {
        showMessage(project, message, NotificationType.INFORMATION);
    }

    public static void showErrorMessage(Project project, String message) {
        showMessage(project, message, NotificationType.ERROR);
    }

    public static void showWarningMessage(Project project, String message) {
        showMessage(project, message, NotificationType.WARNING);
    }

    private static void showMessage(Project project, String message, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(TITLE, message, type, null);
            Notifications.Bus.notify(notification, project);
        });
    }
}
