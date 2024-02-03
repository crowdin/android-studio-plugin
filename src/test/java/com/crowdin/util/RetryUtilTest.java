package com.crowdin.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class RetryUtilTest {
    @ParameterizedTest
    @MethodSource
    public void testRetry(Callable<String> func, String expected) throws Exception {
        String result = RetryUtil.retry(func);
        Assertions.assertEquals(expected, result);
    }


    public static Stream<Arguments> testRetry() {
        Callable<String> func = () -> "test";
        return Stream.of(
                arguments(func, "test")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testRetryWithException(Callable<String> func) {
        assertThrows(Exception.class, () -> RetryUtil.retry(func));
    }


    public static Stream<Arguments> testRetryWithException() {
        Callable<String> func = () -> {
            throw new Exception();
        };
        return Stream.of(
                arguments(func)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testRetryWithNullReturn(Callable<String> func, int retries, String expected) throws Exception {
        String result = RetryUtil.retry(func, retries);
        Assertions.assertEquals(expected, result);
    }


    public static Stream<Arguments> testRetryWithNullReturn() {
        Callable<String> func = () -> {
            throw new Exception();
        };
        return Stream.of(
                arguments(func, 0, null)
        );
    }
}
