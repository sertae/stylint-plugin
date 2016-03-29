package com.stylint;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.stylint.settings.Settings;
import com.wix.utils.FileUtils;
import com.wix.utils.FileUtils.ValidationStatus;
import org.jetbrains.annotations.NotNull;

public class StylintProjectComponent implements ProjectComponent {
    protected Project project;
    protected Settings settings;
    private boolean settingValidStatus;
    private String settingValidVersion;

    private static final Logger LOG = Logger.getInstance(StylintBundle.LOG_ID);

    String stylintConfigFile;
    String stylintExecutable;
    boolean treatAsWarnings;
    boolean showErrorOnWholeLine;
    boolean showColumnNumber;
    public boolean pluginEnabled;

    private static final String PLUGIN_NAME = "Stylint";

    public StylintProjectComponent(Project project) {
        this.project = project;
        settings = Settings.getInstance(project);
    }

    @Override
    public void projectOpened() {
        if (isEnabled()) {
            isSettingsValid();
        }
    }

    @Override
    public void projectClosed() {
    }

    @Override
    public void initComponent() {
        if (isEnabled()) {
            isSettingsValid();
        }
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return com.stylint.StylintProjectComponent.class.getName();
    }

    public boolean isEnabled() {
        return Settings.getInstance(project).pluginEnabled;
    }

    boolean isSettingsValid() {
        if (!settings.getVersion().equals(settingValidVersion)) {
            validateSettings();
            settingValidVersion = settings.getVersion();
        }
        return settingValidStatus;
    }

    public void validateSettings() {
        settingValidStatus = isValid();
        if (!settingValidStatus) {
            return;
        }

        stylintExecutable = settings.stylintExecutable;
        stylintConfigFile = settings.stylintConfigFile;
        treatAsWarnings = settings.treatAllIssuesAsWarnings;
        showErrorOnWholeLine = settings.highlightWholeLine;
        showColumnNumber = settings.showColumnNumber;
        pluginEnabled = settings.pluginEnabled;
    }

    private boolean isValid() {
        return !settings.pluginEnabled || validateField("Stylint bin", settings.stylintExecutable, false, false, true);
    }

    private boolean validateField(String fieldName, String value, boolean shouldBeAbsolute, boolean allowEmpty, boolean isFile) {
        ValidationStatus r = FileUtils.validateProjectPath(shouldBeAbsolute ? null : project, value, allowEmpty, isFile);
        if (r == ValidationStatus.IS_EMPTY && !allowEmpty) {
            String msg = StylintBundle.message("stylint.path.is.empty", fieldName);
            validationFailed(msg);
            return false;
        }
        if (isFile) {
            if (r == ValidationStatus.NOT_A_FILE) {
                String msg = StylintBundle.message("stylint.file.is.not.a.file", fieldName, value);
                validationFailed(msg);
                return false;
            }
        } else {
            if (r == ValidationStatus.NOT_A_DIRECTORY) {
                String msg = StylintBundle.message("stylint.directory.is.not.a.dir", fieldName, value);
                validationFailed(msg);
                return false;
            }
        }
        if (r == ValidationStatus.DOES_NOT_EXIST) {
            String msg = StylintBundle.message("stylint.file.does.not.exist", fieldName, value);
            validationFailed(msg);
            return false;
        }
        return true;
    }

    private void validationFailed(String msg) {
        NotificationListener notificationListener = (notification, event) -> StylintInspection.showSettings(project);
        String errorMessage = msg + StylintBundle.message("stylint.settings.fix");
        showInfoNotification(errorMessage, NotificationType.WARNING, notificationListener);
        LOG.debug(msg);
        settingValidStatus = false;
    }

    void showInfoNotification(String content, NotificationType type) {
        Notification errorNotification = new Notification(PLUGIN_NAME, PLUGIN_NAME, content, type);
        Notifications.Bus.notify(errorNotification, this.project);
    }

    private void showInfoNotification(String content, NotificationType type, NotificationListener notificationListener) {
        Notification errorNotification = new Notification(PLUGIN_NAME, PLUGIN_NAME, content, type, notificationListener);
        Notifications.Bus.notify(errorNotification, this.project);
    }

    static void showNotification(String content, NotificationType type) {
        Notification errorNotification = new Notification(PLUGIN_NAME, PLUGIN_NAME, content, type);
        Notifications.Bus.notify(errorNotification);
    }
}
