package com.stylint.utils;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.wix.nodejs.NodeRunner;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class StylintRunner {
    private StylintRunner() {
    }

    private static final Logger LOG = Logger.getInstance(StylintRunner.class);
    private static final int TIME_OUT = (int) TimeUnit.SECONDS.toMillis(120L);
    private static final int FILES_NOT_FOUND = 66;

    public static class StylintSettings {
        public StylintSettings() {
        }

        StylintSettings(String config, String cwd, String targetFile, String stylintExe) {
            this.config = config;
            this.cwd = cwd;
            this.targetFile = targetFile;
            this.stylintExe = stylintExe;
        }

        public String config;
        public String cwd;
        String targetFile;
        public String stylintExe;
    }

    public static StylintSettings buildSettings(@NotNull String cwd, @NotNull String path, @NotNull String stylintExe, @Nullable String config) {
        return new StylintSettings(config, cwd, path, stylintExe);
    }

    public static LintResult runLint(@NotNull String cwd, @NotNull String file, @NotNull String stylintExe, @Nullable String config) {
        LintResult result = new LintResult();

        try {
            ProcessOutput out = lint(cwd, file, stylintExe, config);
            result.errorOutput = out.getStderr();
            try {
                if (out.getExitCode() != FILES_NOT_FOUND) {
                    List<Lint.FileResult> fileResults = Lint.parse(out.getStdout());
                    if (fileResults != null && !fileResults.isEmpty()) {
                        result.lint = fileResults.get(0).messages;
                    }
                }
            } catch (Exception e) {
                result.errorOutput = out.getStdout();
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.errorOutput = e.toString();
        }

        return result;
    }

    @NotNull
    public static ProcessOutput lint(@NotNull String cwd, @NotNull String file, @NotNull String stylintExe, @Nullable String config) throws ExecutionException {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setWorkDirectory(cwd);
        commandLine.setExePath(stylintExe);

        commandLine.addParameter(file);
        commandLine.addParameter("--reporter");
        commandLine.addParameter("stylint-json-reporter");
        if (StringUtils.isNotEmpty(config)) {
            commandLine.addParameter("--config");
            commandLine.addParameter(config);
        }

        return NodeRunner.execute(commandLine, TIME_OUT);
    }

    @NotNull
    private static ProcessOutput version(@NotNull StylintSettings settings) throws ExecutionException {
        GeneralCommandLine commandLine = createCommandLine(settings);
        commandLine.addParameter("-v");

        return NodeRunner.execute(commandLine, TIME_OUT);
    }

    @NotNull
    public static String runVersion(@NotNull StylintSettings settings) throws ExecutionException {
        if (!new File(settings.stylintExe).exists()) {
            LOG.warn("Calling version with invalid stylintExe exe " + settings.stylintExe);
            return "";
        }

        ProcessOutput out = version(settings);
        if (out.getExitCode() == 0) {
            return out.getStdout().trim();
        }

        return "";
    }

    @NotNull
    private static GeneralCommandLine createCommandLine(@NotNull StylintSettings settings) {
        GeneralCommandLine commandLine = new GeneralCommandLine();
        commandLine.setWorkDirectory(settings.cwd);
        commandLine.setExePath(settings.stylintExe);

        return commandLine;
    }
}