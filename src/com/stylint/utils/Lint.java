package com.stylint.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class Lint {

    static List<FileResult> parse(String json) {
        GsonBuilder builder = new GsonBuilder();
        Gson g = builder.setPrettyPrinting().create();
        Type listType = new TypeToken<List<FileResult>>() {}.getType();
        return g.fromJson(json, listType);
    }

    static class FileResult {
        public String filePath;
        public List<Issue> messages;
        public int errorCount;
        public int warningCount;
    }

    public static class Issue {
        public String rule;
        public int line;
        public int column;
        public String severity;
        public String message;
    }
}

