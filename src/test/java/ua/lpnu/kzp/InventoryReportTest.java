package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InventoryReportTest {
    @Test
    void preservesLab01Baseline() throws IOException {
        List<String> input;
        String expected;
        try (InputStream csv = getClass().getResourceAsStream("/lab01/input.csv");
                InputStream report = getClass().getResourceAsStream("/lab01/report.txt")) {
            input = new String(csv.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            expected = new String(report.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(expected.replace("\r\n", "\n"), InventoryReport.generate(input).replace("\r\n", "\n"));
    }

    @Test
    void calculatesVariantFourAndReportsInvalidRows() {
        String actual = InventoryReport.generate(List.of(
                "SKU-001;Масляний фільтр;10;250.00;АвтоПостач",
                "SKU-002;Гальмівні колодки;4;1200.00;ЗахідАвто",
                "SKU-003;Лампа фари;20;75.00;СвітлоАвто",
                "SKU-004;Свічка;-2;150.00;АвтоПостач",
                "SKU-005;Ремінь;abc;350.00;ЗахідАвто"));
        String expected = String.join(System.lineSeparator(),
                "Склад автозапчастин — звіт",
                "Коректних записів: 3",
                "Загальна кількість деталей: 34",
                "Вартість запасу: 8800.00 грн",
                "Найдорожча деталь: SKU-002 — Гальмівні колодки; ціна: 1200.00 грн",
                "Помилок: 2",
                "Рядок 4: stock не може бути від'ємним",
                "Рядок 5: stock має бути цілим числом у діапазоні int", "");
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "   ", "a;b;1;2", "a;b;1;2;s;extra", ";b;1;2;s", "a; ;1;2;s",
        "a;b;;2;s", "a;b;1;;s", "a;b;1;2;", "a;b;-1;2;s", "a;b;abc;2;s",
        "a;b;2147483648;2;s", "a;b;1;-2;s", "a;b;1;abc;s", "a;b;1;1,25;s",
        "a;b;1;NaN;s", "a;b;1;Infinity;s", "a;b;1;1e309;s", "a;b;2;1e308;s"
    })
    void rejectsInvalidRowWithoutChangingStatistics(String line) {
        String report = InventoryReport.generate(List.of(line, "ok;Деталь;3;2.50;Постачальник"));
        assertTrue(report.contains("Коректних записів: 1"));
        assertTrue(report.contains("Загальна кількість деталей: 3"));
        assertTrue(report.contains("Вартість запасу: 7.50 грн"));
        assertTrue(report.contains("Помилок: 1"));
        assertTrue(report.contains("Рядок 1:"));
    }

    @Test
    void handlesEmptyAndEntirelyInvalidInput() {
        for (List<String> lines : List.of(List.<String>of(), List.of("invalid", ""))) {
            String report = InventoryReport.generate(lines);
            assertTrue(report.contains("Коректних записів: 0"));
            assertTrue(report.contains("Вартість запасу: 0.00 грн"));
            assertTrue(report.contains("немає коректних записів"));
            assertFalse(report.contains("Infinity"));
            assertFalse(report.contains("NaN"));
        }
    }

    @Test
    void supportsBomWhitespaceZeroStockAndFirstMaximumOnTie() {
        String report = InventoryReport.generate(List.of(
                "\uFEFF first ; Українська назва ; 0 ; 100.25 ; Постачальник ",
                "second;Інша назва;2;100.25;Постачальник"));
        assertTrue(report.contains("Коректних записів: 2"));
        assertTrue(report.contains("Вартість запасу: 200.50 грн"));
        assertTrue(report.contains("Найдорожча деталь: first — Українська назва"));
    }

    @Test
    void acceptsZeroPriceAndUsesLongForTotalStock() {
        String report = InventoryReport.generate(List.of(
                "a;Деталь;2147483647;0;П", "b;Деталь;2147483647;0;П"));
        assertTrue(report.contains("Загальна кількість деталей: 4294967294"));
        assertTrue(report.contains("ціна: 0.00 грн"));
    }

    @Test
    void rejectsAccumulationOverflowAndKeepsPreviousRecord() {
        String report = InventoryReport.generate(List.of("a;A;1;1e308;S", "b;B;1;1e308;S"));
        assertTrue(report.contains("Коректних записів: 1"));
        assertTrue(report.contains("Рядок 2: вартість запасу виходить за числовий діапазон"));
    }

    @Test
    @ResourceLock("java.util.Locale.default")
    void formatsWithDecimalPointUnderGermanLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String report = InventoryReport.generate(List.of("a;Деталь;3;1.25;Постачальник"));
            assertTrue(report.contains("3.75 грн"));
            assertTrue(report.contains("1.25 грн"));
            assertFalse(report.contains("3,75"));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
