package com.campus.marathon.service;

import com.campus.marathon.model.MarathonRunner;

/**
 * 参赛者登录校验结果。
 */
public class LoginResult {

    private final MarathonRunner runner;
    private final String errorMessage;

    private LoginResult(MarathonRunner runner, String errorMessage) {
        this.runner = runner;
        this.errorMessage = errorMessage;
    }

    public static LoginResult success(MarathonRunner runner) {
        return new LoginResult(runner, null);
    }

    public static LoginResult failure(String message) {
        return new LoginResult(null, message);
    }

    public boolean isSuccess() {
        return runner != null;
    }

    public MarathonRunner getRunner() {
        return runner;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
