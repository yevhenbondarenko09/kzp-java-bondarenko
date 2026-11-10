package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SparePartTest {
    @Test
    void storesAllFieldsAndFormatsWithRootLocale() {
        SparePart part = SparePart.fromCsv(" SKU-1 ; Brake pad ; 2147483647 ; 12.5 ; Shop ");
        assertEquals("SKU-1", part.sku());
        assertEquals("Brake pad", part.name());
        assertEquals(2147483647, part.stock());
        assertEquals(12.5, part.unitPrice());
        assertEquals("Shop", part.supplier());
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("SKU-1 — Brake pad; запас: 2147483647; ціна: 12.50 грн; постачальник: Shop",
                    part.toString());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void constructorAllowsZerosAndNormalizesNegativeZero() {
        SparePart part = new SparePart("x", "y", 0, -0.0, "z");
        assertEquals(0, part.stock());
        assertEquals(0.0, part.unitPrice());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void constructorRejectsBlankText(String text) {
        assertThrows(IllegalArgumentException.class, () -> new SparePart(text, "n", 0, 0, "s"));
        assertThrows(IllegalArgumentException.class, () -> new SparePart("x", text, 0, 0, "s"));
        assertThrows(IllegalArgumentException.class, () -> new SparePart("x", "n", 0, 0, text));
    }

    @Test
    void constructorRejectsNullAndInvalidNumbers() {
        assertThrows(NullPointerException.class, () -> new SparePart(null, "n", 0, 0, "s"));
        assertThrows(NullPointerException.class, () -> new SparePart("x", null, 0, 0, "s"));
        assertThrows(NullPointerException.class, () -> new SparePart("x", "n", 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new SparePart("x", "n", -1, 0, "s"));
        assertThrows(IllegalArgumentException.class, () -> new SparePart("x", "n", 0, -1, "s"));
        assertThrows(IllegalArgumentException.class, () -> new SparePart("x", "n", 0, Double.NaN, "s"));
        assertThrows(IllegalArgumentException.class, () -> new SparePart("x", "n", 0, Double.POSITIVE_INFINITY, "s"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", ";n;1;2;s", "x;;1;2;s", "x;n;1;2;", "x;n;;2;s",
        "x;n;1;;s", "x;n;1;2;s;extra", "x;n;-1;2;s", "x;n;2147483648;2;s",
        "x;n;one;2;s", "x;n;1;-1;s", "x;n;1;NaN;s", "x;n;1;Infinity;s", "x;n;1;abc;s"})
    void factoryRejectsMalformedCsv(String line) {
        assertThrows(IllegalArgumentException.class, () -> SparePart.fromCsv(line));
    }

    @Test
    void factoryRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> SparePart.fromCsv(null));
    }

    @Test
    void factoryChecksNumericFormatBeforeDomainValidity() {
        IllegalArgumentException format = assertThrows(IllegalArgumentException.class,
                () -> SparePart.fromCsv("a;b;-1;abc;s"));
        assertEquals("unitPrice має бути числом із десятковою крапкою", format.getMessage());
        IllegalArgumentException validity = assertThrows(IllegalArgumentException.class,
                () -> SparePart.fromCsv("a;b;-1;2;s"));
        assertEquals("stock не може бути від'ємним", validity.getMessage());
    }
}
