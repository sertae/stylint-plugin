package com.stylint;

import com.stylint.utils.LintResult;

class StylintAnnotationResult {

    StylintAnnotationResult(StylintAnnotationInput input, LintResult result) {
        this.input = input;
        this.result = result;
    }

    StylintAnnotationResult(StylintAnnotationInput input, LintResult result, String fileLevel) {
        this.input = input;
        this.result = result;
        this.fileLevel = fileLevel;
    }

    final StylintAnnotationInput input;
    final LintResult result;
    String fileLevel;
}
