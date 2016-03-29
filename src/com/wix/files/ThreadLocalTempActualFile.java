package com.wix.files;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.wix.utils.FileUtils;
import com.wix.utils.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class ThreadLocalTempActualFile extends ThreadLocal<RelativeFile> {
    private final String baseName;
    private final String extension;
    private final VirtualFile file;
    private final Project project;
    private static final Logger LOG = Logger.getInstance(Util.LOG_ID);

    private static final String TEMP_DIR_NAME = "intellij-stylint-temp";

    ThreadLocalTempActualFile(@NotNull PsiFile psiFile) {
        this.file = psiFile.getVirtualFile();
        this.project = psiFile.getProject();
        this.baseName = file.getNameWithoutExtension();
        this.extension = FileUtils.getExtensionWithDot(file);
    }

    @Nullable
    RelativeFile getOrCreateFile() {
        RelativeFile path = super.get();
        if (path != null) {
            if (path.file.isFile()) {
                return path;
            }
        }
        RelativeFile file = createFile();
        if (file != null) {
            set(file);
            return file;
        }
        return null;
    }

    @Nullable
    private static File getOrCreateTempDir() {
        File tmpDir = new File(FileUtil.getTempDirectory());
        File dir = new File(tmpDir, TEMP_DIR_NAME);
        if (dir.isDirectory() || dir.mkdirs()) {
            return dir;
        }
        try {
            return FileUtil.createTempDirectory(tmpDir, TEMP_DIR_NAME, null);
        } catch (IOException ignored) {
            LOG.warn("Can't create '" + TEMP_DIR_NAME + "' temporary directory.");
        }
        return null;
    }

    private void copyJSCS(File temp, VirtualFile currentFile) throws IOException {
        VirtualFile jscs = currentFile.findChild(".jscsrc");
        File tempJscs = new File(temp, ".jscsrc");
        if (jscs == null) {
            if (tempJscs.exists()) {
                boolean r = tempJscs.delete();
                if (!r) {
                    LOG.warn("Can not delete file " + tempJscs.getAbsolutePath());
                }
            }
        } else {
            FileUtil.copy(new File(jscs.getPath()), tempJscs);
            tempJscs.deleteOnExit();
        }
        if (currentFile.getParent() == null || currentFile.equals(project.getBaseDir())) {
            return;
        }
        copyJSCS(temp.getParentFile(), currentFile.getParent());
    }

    @Nullable
    private RelativeFile createFile() {
        // try to create a temp file in temp folder
        File dir = getOrCreateTempDir();
        if (dir == null) {
            return null;
        }
        File tempParent = new File(dir, FileUtils.relativePath(project, this.file.getParent()));
        File file = new File(tempParent, this.file.getName());
        boolean ret = FileUtil.createParentDirs(file);
        if (!ret) {
            LOG.warn("Can not createParentDirs " + file.getAbsolutePath());
        }
        try {
            copyJSCS(file.getParentFile(), this.file.getParent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        RelativeFile relativeFile = new RelativeFile(dir, file);
        boolean created = false;
        if (!file.exists()) {
            try {
                created = file.createNewFile();
            } catch (IOException ignored) {
                LOG.warn("Can not create " + file.getAbsolutePath());
            }
        }
        if (!created) {
            try {
                file = FileUtil.createTempFile(dir, this.baseName, this.extension);
            } catch (IOException e) {
                LOG.warn("Can not create temp file", e);
                return null;
            }
        }
        file.deleteOnExit();
        return relativeFile;
    }

}
