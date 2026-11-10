package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PartHierarchyTest {
    private SparePart part(PartKind kind, String sku, int stock, double price) {
        return switch (kind) {
            case ORIGINAL -> new OriginalPart(sku, "Фільтр", stock, price, "Постачальник");
            case REPLACEMENT -> new ReplacementPart(sku, "Фільтр", stock, price, "Постачальник");
        };
    }

    @ParameterizedTest
    @EnumSource(PartKind.class)
    void subtypesHaveFixedCategoryAndPolymorphicWarranty(PartKind kind) {
        SparePart value = part(kind, "A", 7, 1000);
        assertEquals(kind, value.kind());
        assertEquals(kind == PartKind.ORIGINAL ? 100 : 50, value.warrantyCost(), 0.00001);
        assertEquals(value.warrantyCost(), part(kind, "A", 0, 1000).warrantyCost());
        assertEquals(0, part(kind, "A", 0, 0).warrantyCost());
        assertTrue(Double.isFinite(part(kind, "A", 0, Double.MAX_VALUE).warrantyCost()));
        assertEquals(kind == PartKind.ORIGINAL ? "оригінал" : "замінник", kind.label());
    }

    @ParameterizedTest
    @EnumSource(PartKind.class)
    void bothSubtypesEnforceSharedInvariants(PartKind kind) {
        assertThrows(NullPointerException.class, () -> part(kind, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> part(kind, " ", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> part(kind, "A", -1, 0));
        for (double price : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> part(kind, "A", 0, price));
        }
        assertThrows(NullPointerException.class, () -> create(kind, null, "S"));
        assertThrows(IllegalArgumentException.class, () -> create(kind, " ", "S"));
        assertThrows(NullPointerException.class, () -> create(kind, "N", null));
        assertThrows(IllegalArgumentException.class, () -> create(kind, "N", " "));
    }

    private SparePart create(PartKind kind, String name, String supplier) {
        return kind == PartKind.ORIGINAL
                ? new OriginalPart("A", name, 0, 0, supplier)
                : new ReplacementPart("A", name, 0, 0, supplier);
    }

    @Test
    void csvCreatesBothSubtypesAndDefaultsOldFormatToOriginal() {
        assertInstanceOf(OriginalPart.class, SparePart.fromCsv("A;N;1;1000;S"));
        assertInstanceOf(OriginalPart.class, SparePart.fromCsv("A;N;1;1000;S; ORIGINAL "));
        assertInstanceOf(ReplacementPart.class, SparePart.fromCsv("A;N;1;1000;S;REPLACEMENT"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> SparePart.fromCsv("A;N;-1;1000;S;UNKNOWN"));
        assertEquals("kind має бути ORIGINAL або REPLACEMENT", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"A;N;1;2;S;", "A;N;1;2;S;unknown", "A;N;1;2;S;ORIGINAL;extra"})
    void invalidCategoryAndFieldCountAreRejected(String csv) {
        assertThrows(IllegalArgumentException.class, () -> SparePart.fromCsv(csv));
    }

    @ParameterizedTest
    @EnumSource(PartKind.class)
    void logicalDuplicatesUseOnlySubtypeAndSku(PartKind kind) {
        SparePart a = SparePart.fromCsv("A;N;1;1000;S;%s".formatted(kind));
        SparePart b = SparePart.fromCsv(" A ;Other;99;5;Other supplier;%s".formatted(kind));
        SparePart c = SparePart.fromCsv("A;Third;0;0;T;%s".formatted(kind));
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(b, c);
        assertEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, null);
        assertNotEquals(a, "A");
        assertNotEquals(a, part(kind, "B", 1, 1000));
        SparePart replacement = part(kind == PartKind.ORIGINAL ? PartKind.REPLACEMENT : PartKind.ORIGINAL,
                "A", 1, 1000);
        assertNotEquals(a, replacement);
        assertNotEquals(replacement, a);
        HashSet<SparePart> unique = new HashSet<>();
        assertTrue(unique.add(a));
        assertFalse(unique.add(b));
        assertEquals(1, unique.size());
        assertTrue(unique.add(replacement));
        assertEquals(2, unique.size());
    }

    @Test
    void mixedCsvKeepsDuplicatesInTotalsAndCountsUniqueSeparately() {
        InventoryReport.Analysis result = InventoryReport.analyze(List.of(
                "SKU-001;Brake pads;1;1000;GrandAuto;ORIGINAL",
                "SKU-002;Brake discs;1;1000;Modern;REPLACEMENT",
                "SKU-001;Brake pads;1;1000;GrandAuto;ORIGINAL",
                "SKU-003;Windshield washer fluid;1;100;UkraAuto;UNKNOWN",
                "SKU-004;Washer reservoir;-1;100;AutoImp;REPLACEMENT"));
        assertEquals(3, result.parts().size());
        assertEquals(2, result.uniqueCount());
        assertEquals(3, result.total().stock());
        assertEquals(3000, result.total().value());
        assertEquals(250, result.warrantyCost());
        assertEquals(2, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Рядок 4:"));
        assertTrue(result.errors().get(1).startsWith("Рядок 5:"));
        assertThrows(UnsupportedOperationException.class, () -> result.parts().clear());
    }

    @Test
    void extendedExampleSeparatesRecordsStockAndUniqueParts() throws IOException {
        List<String> rows;
        try (InputStream csv = getClass().getResourceAsStream("/lab03/input.csv")) {
            rows = new String(csv.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
        InventoryReport.Analysis result = InventoryReport.analyze(rows);
        assertEquals(10, result.parts().size());
        assertEquals(8, result.uniqueCount());
        assertEquals(new StockValue(60, 17500), result.total());
        assertEquals(540, result.warrantyCost());
        assertEquals(4, result.errors().size());
        assertTrue(result.errors().get(2).startsWith("Рядок 12:"));
        assertTrue(result.errors().get(3).startsWith("Рядок 14:"));
        assertEquals(result.parts().get(0), result.parts().get(3));
        assertEquals(result.parts().get(0), result.parts().get(5));
        assertNotEquals(result.parts().get(0), result.parts().get(4));
        assertInstanceOf(OriginalPart.class, result.parts().get(2));
        String report = InventoryReport.generate(rows);
        assertTrue(report.contains("Коректних записів: 10"));
        assertTrue(report.contains("Загальна кількість деталей: 60"));
        assertTrue(report.contains("Вартість запасу: 17500.00 грн"));
        assertTrue(report.contains("Найдорожча деталь: SKU-006 — Oil filter"));
    }

    @Test
    void warrantyOverflowDoesNotCorruptExistingStockReport() {
        String row = "A;N;0;1.7976931348623157e308;S";
        InventoryReport.Analysis result = InventoryReport.analyze(java.util.Collections.nCopies(11, row));
        assertEquals(11, result.parts().size());
        assertEquals(0, result.total().value());
        assertThrows(IllegalArgumentException.class, result::warrantyCost);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void emptyAndEntirelyInvalidInputsHaveZeroDomainTotals(boolean invalid) {
        InventoryReport.Analysis result = InventoryReport.analyze(invalid
                ? List.of("A;N;-1;100;S;ORIGINAL", "B;N;1;100;S;UNKNOWN") : List.of());
        assertTrue(result.parts().isEmpty());
        assertEquals(0, result.uniqueCount());
        assertEquals(new StockValue(0, 0), result.total());
        assertEquals(0, result.warrantyCost());
        assertEquals(invalid ? 2 : 0, result.errors().size());
        assertThrows(UnsupportedOperationException.class, () -> result.errors().clear());
    }

    @Test
    void mixedInputRecoversAfterOverflowAndPreservesFirstMaximum() {
        List<String> rows = List.of("A;First;1;1000;S;REPLACEMENT",
                "X;Invalid;2;1.7976931348623157e308;S;ORIGINAL",
                "B;Second;2;1000;S;ORIGINAL");
        InventoryReport.Analysis result = InventoryReport.analyze(rows);
        assertEquals(List.of("A", "B"), result.parts().stream().map(SparePart::sku).toList());
        assertEquals(new StockValue(3, 3000), result.total());
        assertEquals(2, result.uniqueCount());
        assertEquals(150, result.warrantyCost());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().getFirst().startsWith("Рядок 2:"));
        assertTrue(InventoryReport.generate(rows).contains("Найдорожча деталь: A — First; ціна: 1000.00 грн"));
    }

    @Test
    void lab02BaselineRemainsCompatible() throws IOException {
        List<String> input = Files.readAllLines(Path.of("src", "test", "resources", "lab02", "input.csv"), StandardCharsets.UTF_8);
        String expected = Files.readString(Path.of("src", "test", "resources", "lab02", "report.txt"), StandardCharsets.UTF_8);
        assertEquals(expected.replace("\r\n", "\n"), InventoryReport.generate(input).replace("\r\n", "\n"));
    }
}
