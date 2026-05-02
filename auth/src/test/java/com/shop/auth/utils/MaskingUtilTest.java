package com.shop.auth.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaskingUtil")
class MaskingUtilTest {

    // ── Happy path ──────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "john.doe@example.com,   j***@e***.com",
        "user@gmail.com,         u***@g***.com",
        "admin@shop.com,         a***@s***.com",
        "a@b.org,                a***@b***.org",
        "test@subdomain.co.uk,   t***@s***.uk"
    })
    @DisplayName("Should mask email — local part and domain name, keep TLD")
    void shouldMaskEmailCorrectly(String input, String expected) {
        assertThat(MaskingUtil.maskEmail(input.trim())).isEqualTo(expected.trim());
    }

    // ── Edge cases ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return *** for null email")
    void shouldReturnMaskedPlaceholderForNull() {
        assertThat(MaskingUtil.maskEmail(null)).isEqualTo("***");
    }

    @ParameterizedTest
    @ValueSource(strings = {"notanemail", "missing-at-sign", "noatsymbol.com"})
    @DisplayName("Should return *** when email has no @ symbol")
    void shouldReturnMaskedPlaceholderWhenNoAtSymbol(String input) {
        assertThat(MaskingUtil.maskEmail(input)).isEqualTo("***");
    }

    @Test
    @DisplayName("Should handle single-character local part")
    void shouldHandleSingleCharLocalPart() {
        String result = MaskingUtil.maskEmail("a@example.com");
        assertThat(result).isEqualTo("a***@e***.com");
    }

    @Test
    @DisplayName("Masked email must never contain the original local part beyond first char")
    void maskedEmailMustNotLeakLocalPart() {
        String original = "john.doe@example.com";
        String masked   = MaskingUtil.maskEmail(original);

        assertThat(masked)
            .doesNotContain("john.doe")
            .doesNotContain("ohn")
            .startsWith("j***@");
    }

    @NullSource
    @ParameterizedTest
    @DisplayName("Should never throw for null input")
    void shouldNeverThrowForNull(String input) {
        assertThat(MaskingUtil.maskEmail(input)).isEqualTo("***");
    }

    @Test
    @DisplayName("Masked output length should always be shorter than original")
    void maskedOutputShouldBeShorterOrEqualToOriginal() {
        String original = "verylongemail@verylongdomain.com";
        String masked   = MaskingUtil.maskEmail(original);
        assertThat(masked.length()).isLessThan(original.length());
    }
}
