package com.stylint.config;

import com.intellij.json.JsonLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import icons.StylintIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StylintConfigFileType extends LanguageFileType {
    static final StylintConfigFileType INSTANCE = new StylintConfigFileType();
    private static final String STYLINTRC_EXT = "stylintrc";
    public static final String STYLINTRC = '.' + STYLINTRC_EXT;

    private StylintConfigFileType() {
        super(JsonLanguage.INSTANCE);
    }

    @NotNull
    public String getName() {
        return "Stylint";
    }

    @NotNull
    public String getDescription() {
        return "Stylint configuration file";
    }

    @NotNull
    public String getDefaultExtension() {
        return STYLINTRC_EXT;
    }

    @NotNull
    public Icon getIcon() {
        return StylintIcons.Stylint;
    }
}