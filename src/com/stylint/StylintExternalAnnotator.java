package com.stylint;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.SeverityRegistrar;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.*;
import com.stylint.config.StylintConfigFileChangeTracker;
import com.stylint.config.StylintConfigFileType;
import com.stylint.settings.StylintSettingsPage;
import com.stylint.utils.Lint;
import com.stylint.utils.LintResult;
import com.stylint.utils.StylintRunner;
import com.wix.files.ThreadLocalTempActualFile;
import com.wix.annotator.AnnotatorUtils;
import com.wix.files.ActualFileManager;
import com.wix.files.BaseActualFile;
import com.wix.files.TempFile;
import com.wix.utils.PsiUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.stylus.StylusFileType;
import org.jetbrains.plugins.stylus.psi.StylusFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StylintExternalAnnotator extends ExternalAnnotator<StylintAnnotationInput, StylintAnnotationResult> {

    private static final Logger LOG = Logger.getInstance(StylintBundle.LOG_ID);

    @Nullable
    @Override
    public StylintAnnotationInput collectInformation(@NotNull PsiFile file) {
        return collectInformation(file, null);
    }

    @Nullable
    @Override
    public StylintAnnotationInput collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return collectInformation(file, editor);
    }

    @NotNull
    private static HighlightDisplayKey getHighlightDisplayKeyByClass() {
        String id = "Stylint";
        HighlightDisplayKey key = HighlightDisplayKey.find(id);
        return key == null ? new HighlightDisplayKey(id, id) : key;
    }

    @Override
    public void apply(@NotNull PsiFile file, StylintAnnotationResult annotationResult, @NotNull AnnotationHolder holder) {
        if (annotationResult == null) {
            return;
        }
        InspectionProjectProfileManager inspectionProjectProfileManager = InspectionProjectProfileManager.getInstance(file.getProject());
        SeverityRegistrar severityRegistrar = inspectionProjectProfileManager.getSeverityRegistrar();
        HighlightDisplayKey inspectionKey = getHighlightDisplayKeyByClass();
        EditorColorsScheme colorsScheme = annotationResult.input.colorsScheme;

        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return;
        }

        if (annotationResult.fileLevel != null) {
            Annotation annotation = holder.createWarningAnnotation(file, annotationResult.fileLevel);
            annotation.registerFix(new EditSettingsAction(new StylintSettingsPage(file.getProject())));
            annotation.setFileLevelAnnotation(true);
            return;
        }

        // TODO consider adding a fix to edit configuration file
        if (annotationResult.result == null || annotationResult.result.lint == null || annotationResult.result.lint.isEmpty()) {
            return;
        }
        List<Lint.Issue> issues = annotationResult.result.lint;
        if (issues == null) {
            return;
        }
        StylintProjectComponent component = annotationResult.input.project.getComponent(StylintProjectComponent.class);
        int tabSize = 4;
        for (Lint.Issue issue : issues) {
            HighlightSeverity severity = getHighlightSeverity(issue, component.treatAsWarnings);
            TextAttributes forcedTextAttributes = AnnotatorUtils.getTextAttributes(colorsScheme, severityRegistrar, severity);
            createAnnotation(holder, file, document, issue, "Stylint: ", tabSize, severity, forcedTextAttributes, inspectionKey, component);
        }
    }

    private static HighlightSeverity getHighlightSeverity(Lint.Issue warn) {
        return warn.severity.toLowerCase().equals("error") ? HighlightSeverity.ERROR : HighlightSeverity.WARNING;
    }

    private static HighlightSeverity getHighlightSeverity(Lint.Issue issue, boolean treatAsWarnings) {
        return treatAsWarnings ? HighlightSeverity.WARNING : getHighlightSeverity(issue);
    }

    private static int getWhitespaceOffset(@NotNull PsiFile file, int offset) {
        PsiElement elt = file.findElementAt(offset);
        return elt instanceof PsiWhiteSpace ? elt.getTextLength() : 0;
    }

    @Nullable
    private static Annotation createAnnotation(@NotNull AnnotationHolder holder, @NotNull PsiFile file, @NotNull Document document, @NotNull Lint.Issue issue,
                                               @NotNull String messagePrefix, int tabSize, @NotNull HighlightSeverity severity, @Nullable TextAttributes forcedTextAttributes,
                                               @NotNull HighlightDisplayKey inspectionKey, StylintProjectComponent component) {
        int errorLine = issue.line - 1;
        int errorColumn = issue.column;
        boolean showErrorOnWholeLine = component.showErrorOnWholeLine;
        boolean showColumnNumber = component.showColumnNumber;

        if (errorLine < 0 || errorLine >= document.getLineCount()) {
            return null;
        }

        int lineEndOffset = document.getLineEndOffset(errorLine);
        int lineStartOffset = document.getLineStartOffset(errorLine);

        int errorLineStartOffset = PsiUtil.calcErrorStartOffsetInDocument(document, lineStartOffset, lineEndOffset, errorColumn, tabSize);

        if (errorLineStartOffset == -1) {
            return null;
        }

        TextRange range;
        if (showErrorOnWholeLine || errorColumn == -1) {
            int off = getWhitespaceOffset(file, lineStartOffset);
            range = new TextRange(lineStartOffset + off, lineEndOffset);
        } else {
            PsiElement lit = PsiUtil.getElementAtOffset(file, errorLineStartOffset);
            range = lit.getTextRange();
        }

        String message = messagePrefix + issue.message.trim() + " (" + (issue.rule == null ? "none" : issue.rule) + ")";
        if(showColumnNumber) {
            message +=  " [" + (errorLine+1) + ":" + errorColumn + "]";
        }

        Annotation annotation = createAnnotation(holder, severity, forcedTextAttributes, range, message);
        if (annotation != null) {
            annotation.setAfterEndOfLine(errorLineStartOffset == lineEndOffset);
        }
        return annotation;
    }

    @NotNull
    private static Annotation createAnnotation(@NotNull AnnotationHolder holder, @NotNull HighlightSeverity severity, @NotNull TextRange range, @NotNull String message) {
        if (severity.equals(HighlightSeverity.ERROR)) {
            return holder.createErrorAnnotation(range, message);
        }
        return holder.createWarningAnnotation(range, message);
    }

    @Nullable
    private static Annotation createAnnotation(@NotNull AnnotationHolder holder, @NotNull HighlightSeverity severity, @Nullable TextAttributes forcedTextAttributes, @NotNull TextRange range, @NotNull String message) {
        if (forcedTextAttributes != null) {
            Annotation annotation = createAnnotation(holder, severity, range, message);
            annotation.setEnforcedTextAttributes(forcedTextAttributes);
            return annotation;
        }
        return createAnnotation(holder, severity, range, message);
    }

    @Nullable
    private static StylintAnnotationInput collectInformation(@NotNull PsiFile psiFile, @Nullable Editor editor) {
        if (psiFile.getContext() != null) {
            return null;
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            return null;
        }
        if (psiFile.getViewProvider() instanceof MultiplePsiFilesPerDocumentFileViewProvider) {
            return null;
        }
        Project project = psiFile.getProject();
        StylintProjectComponent component = project.getComponent(StylintProjectComponent.class);
        if (!component.isSettingsValid() || !component.isEnabled() || !isStylusFile(psiFile)) {
            return null;
        }
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return null;
        }
        String fileContent = document.getText();
        if (StringUtil.isEmptyOrSpaces(fileContent)) {
            return null;
        }
        EditorColorsScheme colorsScheme = editor == null ? null : editor.getColorsScheme();
        return new StylintAnnotationInput(project, psiFile, fileContent, colorsScheme);
    }

    private static boolean isStylusFile(PsiFile file) {
        return file instanceof StylusFile && file.getFileType().equals(StylusFileType.STYLUS);
    }

    private static final Key<ThreadLocalTempActualFile> TEMP_FILE = Key.create("STYLINT_TEMP_FILE");


    private static void copyConfig(Project project, File temp) throws IOException {
        copyConfigFile(project, temp, StylintConfigFileType.STYLINTRC);
    }

    private static void copyConfigFile(Project project, File temp, String fileName) throws IOException {
        VirtualFile jscs = project.getBaseDir().findChild(fileName);
        File tempJscs = new File(temp, fileName);
        if (jscs != null) {
            FileUtil.copy(new File(jscs.getPath()), tempJscs);
            tempJscs.deleteOnExit();
        }
    }

    @Nullable
    @Override
    public StylintAnnotationResult doAnnotate(StylintAnnotationInput collectedInfo) {
        BaseActualFile actualFile = null;
        try {
            PsiFile file = collectedInfo.psiFile;
            if (!isStylusFile(file)) {
                return null;
            }
            StylintProjectComponent component = file.getProject().getComponent(StylintProjectComponent.class);
            if (!component.isEnabled()) {
                return new StylintAnnotationResult(collectedInfo, null, "Stylint is available for this file but is not configured");
            }
            if (!component.isSettingsValid()) {
                return new StylintAnnotationResult(collectedInfo, null, "Stylint is not configured correctly");
            }

            StylintConfigFileChangeTracker.getInstance(collectedInfo.project).startIfNeeded();
            actualFile = ActualFileManager.getOrCreateActualFile(TEMP_FILE, file, collectedInfo.fileContent);
            if (actualFile instanceof TempFile) {
                copyConfig(file.getProject(), new File(actualFile.getCwd()));
            }
            if (actualFile == null) {
                LOG.warn("Failed to create file for lint");
                return null;
            }
            LintResult result = StylintRunner.runLint(actualFile.getCwd(), actualFile.getPath(), component.stylintExecutable, component.stylintConfigFile);

            if (StringUtils.isNotEmpty(result.errorOutput)) {
                component.showInfoNotification(result.errorOutput, NotificationType.WARNING);
                return null;
            }
            Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
            if (document == null) {
                component.showInfoNotification("Error running Stylint inspection: Could not get document for file " + file.getName(), NotificationType.WARNING);
                LOG.warn("Could not get document for file " + file.getName());
                return null;
            }
            return new StylintAnnotationResult(collectedInfo, result);
        } catch (Exception e) {
            LOG.error("Error running Stylint inspection: ", e);
            StylintProjectComponent.showNotification("Error running Stylint inspection: " + e.getMessage(), NotificationType.ERROR);
        } finally {
            ActualFileManager.dispose(actualFile);
        }
        return null;
    }
}


