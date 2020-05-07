package com.crowdin.util;

import java.util.concurrent.Callable;

public final class RetryUtil {

    private static final Integer DEFAULT_RETRIES = 3;

    private RetryUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T> T retry(Callable<T> func) throws Exception {
        return retry(func, DEFAULT_RETRIES);
    }

    public static <T> T retry(Callable<T> func, int retries) throws Exception {
        for (int i = 0; i < retries; i++) {
            try {
                return func.call();
            } catch (Exception e) {
                if (i + i == retries) {
                    throw e;
                }
                Thread.sleep(100);
            }
        }
        return null;
    }
}
