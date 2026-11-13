package com.campus.marathon.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务校验异常，携带字段级错误信息，供表单回显。
 */
public class ValidationException extends Exception {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super("报名信息校验未通过");
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
