package com.stylint.config;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class StylintConfigFileChangeTracker {
    private final AtomicBoolean TRACKING = new AtomicBoolean(false);
    private final Project project;

    public StylintConfigFileChangeTracker(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public static StylintConfigFileChangeTracker getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, StylintConfigFileChangeTracker.class);
    }

    public void startIfNeeded() {
        if (TRACKING.compareAndSet(false, true))
            ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
                VirtualFileManager.getInstance().addVirtualFileListener(new StylintConfigFileVfsListener(), StylintConfigFileChangeTracker.this.project);
                EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
                multicaster.addDocumentListener(new StylintConfigFileDocumentListener(), StylintConfigFileChangeTracker.this.project);
            }));
    }

    private void onChange(@NotNull VirtualFile file) {
        if (StylintConfigFileType.STYLINTRC.equals(file.getName()) && !project.isDisposed()) {
            restartCodeAnalyzerIfNeeded();
        }
    }

    private void restartCodeAnalyzerIfNeeded() {
        com.stylint.StylintProjectComponent component = project.getComponent(com.stylint.StylintProjectComponent.class);
        if (component.isEnabled()) {
            DaemonCodeAnalyzer.getInstance(project).restart();
        }
    }

    private final class StylintConfigFileDocumentListener extends DocumentAdapter {
        private StylintConfigFileDocumentListener() {
        }

        public void beforeDocumentChange(DocumentEvent event) {
        }

        public void documentChanged(DocumentEvent event) {
            VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
            if (file != null) {
                StylintConfigFileChangeTracker.this.onChange(file);
            }
        }
    }

    private final class StylintConfigFileVfsListener extends VirtualFileAdapter {
        private StylintConfigFileVfsListener() {
        }

        public void fileCreated(@NotNull VirtualFileEvent event) {
            StylintConfigFileChangeTracker.this.onChange(event.getFile());
        }

        public void fileDeleted(@NotNull VirtualFileEvent event) {
            StylintConfigFileChangeTracker.this.onChange(event.getFile());
        }

        public void fileMoved(@NotNull VirtualFileMoveEvent event) {
            StylintConfigFileChangeTracker.this.onChange(event.getFile());
        }

        public void fileCopied(@NotNull VirtualFileCopyEvent event) {
            StylintConfigFileChangeTracker.this.onChange(event.getFile());
            StylintConfigFileChangeTracker.this.onChange(event.getOriginalFile());
        }
    }
}

