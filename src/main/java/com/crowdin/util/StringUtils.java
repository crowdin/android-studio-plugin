package com.crowdin.util;

public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException();
    }

    public static String removeStart(String str, String remove) {
        if (!isEmpty(str) && !isEmpty(remove)) {
            return str.startsWith(remove) ? str.substring(remove.length()) : str;
        } else {
            return str;
        }
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean containsNone(String str, String invalidChars) {
        char[] sourceChars = str != null ? str.toCharArray() : new char[]{};
        for (char sourceChar : sourceChars) {
            for (char invalidChar : invalidChars.toCharArray()) {
                if (invalidChar == sourceChar) {
                    return false;
                }
            }
        }
        return true;
    }

}
