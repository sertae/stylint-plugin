package com.stylint.utils;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.util.containers.ContainerUtil;
import com.stylint.config.StylintConfigFileType;
import com.wix.nodejs.NodeFinder;
import com.wix.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Set;

public final class StylintFinder {
    private StylintFinder() {
    }

    @NotNull
    public static List<File> findAllStylintExe() {
        Set<File> exes = ContainerUtil.newLinkedHashSet();
        // TODO looks like on windows it only searches system path and not user's
        List<File> fromPath = PathEnvironmentVariableUtil.findAllExeFilesInPath(NodeFinder.getBinName("stylint"));
        exes.addAll(fromPath);
        return ContainerUtil.newArrayList(exes);
    }

    public static List<String> searchForLintConfigFiles(final File projectRoot) {
        FilenameFilter filter = (file, name) -> name.equals(StylintConfigFileType.STYLINTRC);
        List<String> files = FileUtils.recursiveVisitor(projectRoot, filter);
        return ContainerUtil.map(files, curFile -> {
            return FileUtils.makeRelative(projectRoot, new File(curFile));
        });
    }
}