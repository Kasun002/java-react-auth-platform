package com.org.auth.utils;

public final class MaskingUtil {

    private MaskingUtil() {}

    /**
     * Masks an email address for safe logging.
     * john.doe@example.com → j***@e***.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        String local  = parts[0];
        String domain = parts[1];

        String maskedLocal  = local.charAt(0) + "***";
        String maskedDomain = maskDomain(domain);

        return maskedLocal + "@" + maskedDomain;
    }

    private static String maskDomain(String domain) {
        int dotIndex = domain.lastIndexOf('.');
        if (dotIndex <= 0) {
            return "***";
        }
        String domainName = domain.substring(0, dotIndex);
        String tld        = domain.substring(dotIndex);
        return domainName.charAt(0) + "***" + tld;
    }
}
