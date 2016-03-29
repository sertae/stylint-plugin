package com.wix.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public final class FileUtils {
    private FileUtils() {
    }

    public static String relativePath(Project project, VirtualFile absolutePath) {
        return FileUtil.getRelativePath(new File(project.getBasePath()), new File(absolutePath.getPath()));
    }

    public static String getExtensionWithDot(VirtualFile file){
        String ext = StringUtil.notNullize(file.getExtension());
        if (!ext.startsWith(".")) {
            ext = '.' + ext;
        }
        return ext;
    }

    public static String makeRelative(File project, File absolutePath) {
        return FileUtil.getRelativePath(project, absolutePath);
    }

    @NotNull
    public static List<String> recursiveVisitor(@NotNull File dir, @NotNull FilenameFilter filter) {
        List<String> retList = new ArrayList<>();
        File[] files = dir.listFiles();
        for (final File file : files) {
            if (file.isDirectory()) {
                retList.addAll(recursiveVisitor(file, filter));
            } else {
                if (filter.accept(file.getParentFile(), file.getName())){
                    retList.add(file.getAbsolutePath());
                }
            }
        }
        return retList;
    }

    public static List<String> toAbsolutePath(List<File> newFiles) {
        return ContainerUtil.map(newFiles, File::getAbsolutePath);
    }

    public static ValidationStatus validateProjectPath(Project project, String path, boolean allowEmpty, boolean isFile) {
        if (StringUtils.isEmpty(path)) {
            return allowEmpty ? ValidationStatus.VALID : ValidationStatus.IS_EMPTY;
        }
        File filePath = new File(path);
        if (filePath.isAbsolute()) {
            if (!filePath.exists()) {
                return ValidationStatus.DOES_NOT_EXIST;
            }
            if (isFile) {
                if (!filePath.isFile()) {
                    return ValidationStatus.NOT_A_FILE;
                }
            } else {
                if (!filePath.isDirectory()) {
                    return ValidationStatus.NOT_A_DIRECTORY;
                }
            }
        } else {
            if (project == null) {
                return ValidationStatus.DOES_NOT_EXIST;
            }
            VirtualFile child = project.getBaseDir().findFileByRelativePath(path);
            if (child == null || !child.exists()) {
                return ValidationStatus.DOES_NOT_EXIST;
            }
            if (isFile) {
                if (child.isDirectory()) {
                    return ValidationStatus.NOT_A_FILE;
                }
            } else {
                if (!child.isDirectory()) {
                    return ValidationStatus.NOT_A_DIRECTORY;
                }
            }
        }
        return ValidationStatus.VALID;
    }

    public enum ValidationStatus {
        VALID, IS_EMPTY, DOES_NOT_EXIST, NOT_A_DIRECTORY, NOT_A_FILE
    }
}
