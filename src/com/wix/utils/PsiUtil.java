package com.wix.utils;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public final class PsiUtil {
    private PsiUtil() {
    }

    @NotNull
    public static PsiElement getElementAtOffset(@NotNull PsiFile file, int offset) {
        PsiElement elt = file.findElementAt(offset);
        if (elt == null && offset > 0) {
            elt = file.findElementAt(offset - 1);
        }
        if (elt == null) {
            return file;
        }
        return elt;
    }

    public static int calcErrorStartOffsetInDocument(@NotNull Document document, int lineStartOffset, int lineEndOffset, int column, int tabSize) {
        if (tabSize <= 1) {
            if (column < 0) {
                return lineStartOffset;
            }
            if (lineStartOffset + column <= lineEndOffset) {
                return lineStartOffset + column;
            }
            return lineEndOffset;
        }
        CharSequence docText = document.getCharsSequence();
        int offset = lineStartOffset;
        int col = 0;
        while (offset < lineEndOffset && col < column) {
            col += docText.charAt(offset) == '\t' ? tabSize : 1;
            offset++;
        }
        return offset;
    }
}
