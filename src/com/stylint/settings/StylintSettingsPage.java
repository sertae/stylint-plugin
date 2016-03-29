package com.stylint.settings;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TextFieldWithHistory;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.util.ui.SwingHelper;
import com.intellij.util.ui.UIUtil;
import com.stylint.StylintBundle;
import com.stylint.utils.StylintFinder;
import com.stylint.utils.StylintRunner;
import com.wix.settings.ValidationUtils;
import com.wix.settings.Validator;
import com.wix.ui.PackagesNotificationPanel;
import com.wix.utils.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;

public class StylintSettingsPage implements Configurable {
    private final Project project;

    private JCheckBox pluginEnabledCheckbox;
    private JPanel panel;
    private JPanel errorPanel;
    private TextFieldWithHistoryWithBrowseButton stylintConfigFile;
    private JRadioButton searchForConfigInRadioButton;
    private JRadioButton useSpecificConfigRadioButton;
    private HyperlinkLabel usageLink;
    private JLabel StylintConfigFilePathLabel;
    private JCheckBox treatAllIssuesCheckBox;
    private JCheckBox highlightWholeLineCheckBox;
    private JCheckBox showColumnNumberCheckBox;
    private JLabel versionLabel;
    private JLabel stylintExeLabel;
    private TextFieldWithHistoryWithBrowseButton stylintExeField;
    private final PackagesNotificationPanel packagesNotificationPanel;

    public StylintSettingsPage(@NotNull final Project project) {
        this.project = project;
        configESLintBinField();
        configStylintConfigField();
        this.packagesNotificationPanel = new PackagesNotificationPanel(project);
        errorPanel.add(this.packagesNotificationPanel.getComponent(), BorderLayout.CENTER);
    }

    private void addListeners() {
        useSpecificConfigRadioButton.addItemListener(e -> stylintConfigFile.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        pluginEnabledCheckbox.addItemListener(e -> {
            boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
            setEnabledState(enabled);
        });
        DocumentAdapter docAdp = new DocumentAdapter() {
            protected void textChanged(DocumentEvent e) {
                updateLaterInEDT();
            }
        };
        stylintExeField.getChildComponent().getTextEditor().getDocument().addDocumentListener(docAdp);
        stylintConfigFile.getChildComponent().getTextEditor().getDocument().addDocumentListener(docAdp);
    }

    private void updateLaterInEDT() {
        UIUtil.invokeLaterIfNeeded(StylintSettingsPage.this::update);
    }

    private void update() {
        ApplicationManager.getApplication().assertIsDispatchThread();
        validate();
    }

    private void setEnabledState(boolean enabled) {
        searchForConfigInRadioButton.setEnabled(enabled);
        useSpecificConfigRadioButton.setEnabled(enabled);
        stylintConfigFile.setEnabled(enabled && useSpecificConfigRadioButton.isSelected());
        stylintExeField.setEnabled(enabled);
        StylintConfigFilePathLabel.setEnabled(enabled);
        stylintExeLabel.setEnabled(enabled);
        treatAllIssuesCheckBox.setEnabled(enabled);
        highlightWholeLineCheckBox.setEnabled(enabled);
        showColumnNumberCheckBox.setEnabled(enabled);
    }

    private void validate() {
        Validator validator = new Validator();

        TextFieldWithHistory exeComponent = stylintExeField.getChildComponent();
        if (!ValidationUtils.validatePath(project, exeComponent.getText(), false)) {
            validator.add(exeComponent.getTextEditor(), StylintBundle.message("stylint.settings.validate.exe"), StylintBundle.message("stylint.settings.validate.fix"));
        }

        TextFieldWithHistory configComponent = stylintConfigFile.getChildComponent();
        if (!ValidationUtils.validatePath(project, configComponent.getText(), true)) {
            validator.add(configComponent.getTextEditor(), StylintBundle.message("stylint.settings.validate.config"), StylintBundle.message("stylint.settings.validate.fix"));
        }

        if (validator.hasErrors()) {
            versionLabel.setText("n.a.");
        } else {
            updateVersion();
        }

        packagesNotificationPanel.processErrors(validator);
    }

    private StylintRunner.StylintSettings settings;

    private void updateVersion() {
        String stylusExe = stylintExeField.getChildComponent().getText();
        if (settings != null &&
                settings.stylintExe.equals(stylusExe) &&
                settings.cwd.equals(project.getBasePath())) {
            return;
        }
        if (StringUtils.isEmpty(stylusExe)) {
            return;
        }
        getVersion(stylusExe, project.getBasePath());
    }

    private void getVersion(String stylusExe, String cwd) {
        if (StringUtils.isEmpty(stylusExe)) {
            return;
        }
        settings = new StylintRunner.StylintSettings();
        settings.stylintExe = stylusExe;
        settings.cwd = cwd;
        try {
            versionLabel.setText(StylintRunner.runVersion(settings));
        } catch (Exception e) {
            versionLabel.setText("error");
            e.printStackTrace();
        }
    }

    private void configESLintBinField() {
        TextFieldWithHistory textFieldWithHistory = stylintExeField.getChildComponent();
        textFieldWithHistory.setHistorySize(-1);
        textFieldWithHistory.setMinimumAndPreferredWidth(0);

        SwingHelper.addHistoryOnExpansion(textFieldWithHistory, () -> FileUtils.toAbsolutePath(StylintFinder.findAllStylintExe()));
        SwingHelper.installFileCompletionAndBrowseDialog(project, stylintExeField, "Select Stylint Exe", FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
    }

    private void configStylintConfigField() {
        TextFieldWithHistory textFieldWithHistory = stylintConfigFile.getChildComponent();
        textFieldWithHistory.setHistorySize(-1);
        textFieldWithHistory.setMinimumAndPreferredWidth(0);

        SwingHelper.addHistoryOnExpansion(textFieldWithHistory, () -> StylintFinder.searchForLintConfigFiles(new File(project.getBaseDir().getPath())));
        SwingHelper.installFileCompletionAndBrowseDialog(project, stylintConfigFile, "Select Stylint Config", FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());
    }

    @Nls
    @Override
    public String getDisplayName() {
        return StylintBundle.message("stylint.name");
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        loadSettings();
        getVersion(stylintExeField.getChildComponent().getText(), project.getBasePath());
        addListeners();
        return panel;
    }

    @Override
    public boolean isModified() {
        Settings settings = getSettings();
        return pluginEnabledCheckbox.isSelected() != settings.pluginEnabled
                || !stylintExeField.getChildComponent().getText().equals(settings.stylintExecutable)
                || treatAllIssuesCheckBox.isSelected() != settings.treatAllIssuesAsWarnings
                || highlightWholeLineCheckBox.isSelected() != settings.highlightWholeLine
                || showColumnNumberCheckBox.isSelected() != settings.showColumnNumber
                || !getLintConfigFile().equals(settings.stylintConfigFile);
    }

    private String getLintConfigFile() {
        return useSpecificConfigRadioButton.isSelected() ? stylintConfigFile.getChildComponent().getText() : "";
    }

    @Override
    public void apply() throws ConfigurationException {
        saveSettings();
        PsiManager.getInstance(project).dropResolveCaches();
    }

    private void saveSettings() {
        Settings settings = getSettings();
        settings.pluginEnabled = pluginEnabledCheckbox.isSelected();
        settings.stylintExecutable = stylintExeField.getChildComponent().getText();
        settings.stylintConfigFile = getLintConfigFile();
        settings.treatAllIssuesAsWarnings = treatAllIssuesCheckBox.isSelected();
        settings.highlightWholeLine = highlightWholeLineCheckBox.isSelected();
        settings.showColumnNumber = showColumnNumberCheckBox.isSelected();
        project.getComponent(com.stylint.StylintProjectComponent.class).validateSettings();
        DaemonCodeAnalyzer.getInstance(project).restart();
    }

    private void loadSettings() {
        Settings settings = getSettings();
        pluginEnabledCheckbox.setSelected(settings.pluginEnabled);
        stylintExeField.getChildComponent().setText(settings.stylintExecutable);
        stylintConfigFile.getChildComponent().setText(settings.stylintConfigFile);

        boolean hasConfig = StringUtils.isNotEmpty(settings.stylintConfigFile);
        searchForConfigInRadioButton.setSelected(!hasConfig);
        useSpecificConfigRadioButton.setSelected(hasConfig);
        stylintConfigFile.setEnabled(hasConfig);
        treatAllIssuesCheckBox.setSelected(settings.treatAllIssuesAsWarnings);
        highlightWholeLineCheckBox.setSelected(settings.highlightWholeLine);
        showColumnNumberCheckBox.setSelected(settings.showColumnNumber);
        setEnabledState(settings.pluginEnabled);
    }

    @Override
    public void reset() {
        loadSettings();
    }

    @Override
    public void disposeUIResources() {
    }

    protected Settings getSettings() {
        return Settings.getInstance(project);
    }

    private void createUIComponents() {
        usageLink = SwingHelper.createWebHyperlink(StylintBundle.message("stylint.settings.how.to.use"), StylintBundle.message("stylint.settings.how.to.use.link"));
    }

    public void showSettings() {
        String dimensionKey = ShowSettingsUtilImpl.createDimensionKey(this);
        SingleConfigurableEditor singleConfigurableEditor = new SingleConfigurableEditor(project, this, dimensionKey, false);
        singleConfigurableEditor.show();
    }
}
