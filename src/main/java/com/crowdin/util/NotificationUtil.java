package com.crowdin.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class NotificationUtil {

    private static final NotificationGroup GROUP_DISPLAY_ID_INFO =
            new NotificationGroup("Crowdin",
                    NotificationDisplayType.BALLOON, true);

    private static final NotificationGroup GROUP_DISPLAY_ID_INFO_LOG =
        new NotificationGroup("Crowdin",
            NotificationDisplayType.NONE, true);
    private static boolean isDebug = false;
    private static SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    private static final String TITLE = "Crowdin";

    private NotificationUtil() {
        throw new UnsupportedOperationException();
    }

    public static void showInformationMessage(@NotNull Project project, @NotNull String message) {
        showMessage(project, message, NotificationType.INFORMATION);
    }

    public static void showErrorMessage(@NotNull Project project, @NotNull String message) {
        showMessage(project, message, NotificationType.ERROR);
    }

    public static void showWarningMessage(@NotNull Project project, @NotNull String message) {
        showMessage(project, message, NotificationType.WARNING);
    }

    private static void showMessage(Project project, String message, NotificationType type) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(TITLE, message, type, null);
            Notifications.Bus.notify(notification, project);
        });
    }

    public static void setLogDebugLevel(boolean isDebug) {
        NotificationUtil.isDebug = isDebug;
    }

    public static void logDebugMessage(@NotNull Project project, @NotNull String message) {
        logMessage(project, message, NotificationType.INFORMATION, "DEBUG");
    }

    public static void logErrorMessage(@NotNull Project project, @NotNull Exception e) {
        logMessage(project, ExceptionUtils.getStackTrace(ExceptionUtils.getRootCause(e)), NotificationType.ERROR, "ERROR");
    }

    private static void logMessage(@NotNull Project project, @NotNull String message, @NotNull NotificationType type, String level) {
        if (isDebug) {
            String formattedMessage = String.format("%s %s : %s", logDateFormat.format(new Date()), level, message);
            ApplicationManager.getApplication().invokeLater(() -> {
                Notification notification = GROUP_DISPLAY_ID_INFO_LOG.createNotification(TITLE, formattedMessage, type, null);
                Notifications.Bus.notify(notification, project);
            });
        }
    }
}
