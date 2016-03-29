package com.wix.files;

import java.io.File;

class RelativeFile {
    final File root;
    public final File file;

    RelativeFile(File root, File file) {
        this.root = root;
        this.file = file;
    }
}