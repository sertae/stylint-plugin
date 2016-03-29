package com.stylint.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

@State(name = "StylintProjectComponent", storages = {@Storage("stylintPlugin.xml") })
public class Settings implements PersistentStateComponent<Settings> {
    public String stylintConfigFile = "";
    public String stylintExecutable = "";
    public boolean treatAllIssuesAsWarnings;
    public boolean highlightWholeLine;
    public boolean showColumnNumber;
    public boolean pluginEnabled;

    public static Settings getInstance(Project project) {
        return ServiceManager.getService(project, Settings.class);
    }

    @Nullable
    @Override
    public Settings getState() {
        return this;
    }

    @Override
    public void loadState(Settings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getVersion() {
        return stylintExecutable + stylintConfigFile + treatAllIssuesAsWarnings;
    }
}
