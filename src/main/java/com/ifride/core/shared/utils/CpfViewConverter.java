package com.ifride.core.shared.utils;

public class CpfViewConverter {

    private static final String SEARCH_PATTERN = "(\\d{3})(\\d{3})(\\d{3})(\\d{2})";
    private static final String REPLACEMENT_PATTERN = "$1.$2.$3-$4";

    public static String convert(String unformattedCpf) {
        return unformattedCpf.replaceAll(SEARCH_PATTERN, REPLACEMENT_PATTERN);
    }

    public static String convertFormatted(String formattedCpf) {
        return formattedCpf.replace(".", "").replace("-", "");
    }
}
