package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class InventoryQueriesTest {
    private static List<SparePart> parts() throws IOException {
        try (var input = InventoryQueriesTest.class.getResourceAsStream("/lab04/input.csv")) {
            return InventoryReport.analyze(new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .lines().toList()).parts();
        }
    }

    @Test
    void filtersStrictlyBelowThresholdAndMapsWithoutRemovingDuplicates() throws IOException {
        List<SparePart> input = parts();
        assertEquals(List.of("A", "D", "E", "A", "A"),
                InventoryQueries.skus(InventoryQueries.lowStock(input, 5)));
        assertEquals(List.of("A", "B", "C", "D", "E", "F", "A", "A"), InventoryQueries.skus(input));
        assertTrue(InventoryQueries.lowStock(input, 0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> InventoryQueries.lowStock(input, -1));
    }

    @Test
    void summarizesUnitPricesIncludingZeroAndDuplicates() throws IOException {
        DoubleSummaryStatistics result = InventoryQueries.priceStatistics(parts());
        assertEquals(8, result.getCount());
        assertEquals(1200, result.getSum(), 1e-9);
        assertEquals(0, result.getMin(), 1e-9);
        assertEquals(300, result.getMax(), 1e-9);
        assertEquals(150, result.getAverage(), 1e-9);
        assertEquals("150.00", String.format(Locale.ROOT, "%.2f", result.getAverage()));
    }

    @Test
    void groupsStockValueByManufacturerWithStableKeysAndDuplicates() throws IOException {
        Map<String, Double> result = InventoryQueries.stockValueByManufacturer(parts());
        assertEquals(Map.of("Bosch", 1100.0, "Brembo", 3300.0, "Denso", 1200.0), result);
        assertEquals(List.of("Bosch", "Brembo", "Denso"), new ArrayList<>(result.keySet()));
        assertEquals(5600, result.values().stream().mapToDouble(Double::doubleValue).sum(), 1e-9);
    }

    @Test
    void rejectsOverflowingManufacturerProductAndSumButAcceptsZeroStock() {
        SparePart huge = new OriginalPart("A", "A", 1, Double.MAX_VALUE, "S");
        assertThrows(IllegalArgumentException.class,
                () -> InventoryQueries.stockValueByManufacturer(List.of(huge, huge)));
        assertThrows(IllegalArgumentException.class, () -> InventoryQueries.stockValueByManufacturer(
                List.of(new ReplacementPart("B", "B", 2, Double.MAX_VALUE, "T"))));
        assertEquals(Map.of("S", 0.0), InventoryQueries.stockValueByManufacturer(
                List.of(new OriginalPart("A", "A", 0, Double.MAX_VALUE, "S"))));
    }

    @Test
    void sortsBeforeLimitingAndUsesNameForPriceTies() throws IOException {
        List<SparePart> input = parts();
        assertEquals(List.of("C", "B", "F", "A", "A"),
                InventoryQueries.skus(InventoryQueries.topByPrice(input, 5)));
        assertEquals(List.of("C"), InventoryQueries.skus(InventoryQueries.topByPrice(input, 1)));
        assertTrue(InventoryQueries.topByPrice(input, 0).isEmpty());
        assertEquals(8, InventoryQueries.topByPrice(input, 100).size());
        List<SparePart> all = InventoryQueries.topByPrice(input, 100);
        assertSame(input.get(0), all.get(4));
        assertSame(input.get(6), all.get(5));
        assertThrows(IllegalArgumentException.class, () -> InventoryQueries.topByPrice(input, -1));
    }

    @Test
    void findsFirstSkuAcrossDuplicatesAndSubtypes() throws IOException {
        List<SparePart> input = parts();
        assertSame(input.getFirst(), InventoryQueries.findBySku(input, "A").orElseThrow());
        assertSame(input.get(1), InventoryQueries.findBySku(input, "B").orElseThrow());
        assertTrue(InventoryQueries.findBySku(input, "missing").isEmpty());
        assertTrue(InventoryQueries.findBySku(input, "a").isEmpty());
        assertTrue(InventoryQueries.findBySku(input, " A ").isEmpty());
    }

    @Test
    void handlesEmptyInputWithoutInventingPrices() {
        List<SparePart> empty = List.of();
        assertTrue(InventoryQueries.lowStock(empty, 5).isEmpty());
        assertTrue(InventoryQueries.skus(empty).isEmpty());
        assertTrue(InventoryQueries.stockValueByManufacturer(empty).isEmpty());
        assertTrue(InventoryQueries.topByPrice(empty, 5).isEmpty());
        assertTrue(InventoryQueries.findBySku(empty, "A").isEmpty());
        DoubleSummaryStatistics result = InventoryQueries.priceStatistics(empty);
        assertEquals(0, result.getCount());
        assertEquals(0, result.getSum());
        assertEquals(0, result.getAverage());
        assertEquals(Double.POSITIVE_INFINITY, result.getMin());
        assertEquals(Double.NEGATIVE_INFINITY, result.getMax());
    }

    @Test
    void rejectsNullArgumentsAndElements() {
        assertThrows(NullPointerException.class, () -> InventoryQueries.lowStock(null, 5));
        assertThrows(NullPointerException.class, () -> InventoryQueries.skus(null));
        assertThrows(NullPointerException.class, () -> InventoryQueries.stockValueByManufacturer(null));
        assertThrows(NullPointerException.class, () -> InventoryQueries.priceStatistics(null));
        assertThrows(NullPointerException.class, () -> InventoryQueries.topByPrice(null, 5));
        assertThrows(NullPointerException.class, () -> InventoryQueries.findBySku(null, "A"));
        assertThrows(NullPointerException.class, () -> InventoryQueries.findBySku(List.of(), null));
        List<SparePart> invalid = Collections.singletonList(null);
        assertThrows(NullPointerException.class, () -> InventoryQueries.lowStock(invalid, 5));
        assertThrows(NullPointerException.class, () -> InventoryQueries.skus(invalid));
        assertThrows(NullPointerException.class, () -> InventoryQueries.stockValueByManufacturer(invalid));
        assertThrows(NullPointerException.class, () -> InventoryQueries.priceStatistics(invalid));
        assertThrows(NullPointerException.class, () -> InventoryQueries.findBySku(invalid, "A"));
    }

    @Test
    void rejectsOverflowingPriceSumButAcceptsFiniteExtreme() {
        SparePart huge = new OriginalPart("A", "A", 0, Double.MAX_VALUE, "S");
        assertEquals(Double.MAX_VALUE, InventoryQueries.priceStatistics(List.of(huge)).getSum());
        assertThrows(IllegalArgumentException.class,
                () -> InventoryQueries.priceStatistics(List.of(huge, huge)));
    }

    @Test
    void independentQueriesDoNotMutateMutableInputAndReturnUnmodifiableLists() throws IOException {
        List<SparePart> input = new ArrayList<>(parts());
        List<SparePart> before = List.copyOf(input);
        InventoryQueries.lowStock(input, 5);
        InventoryQueries.priceStatistics(input);
        InventoryQueries.stockValueByManufacturer(input);
        InventoryQueries.topByPrice(input, 5);
        InventoryQueries.findBySku(input, "A");
        assertEquals(InventoryQueries.skus(input), InventoryQueries.skus(input));
        for (int i = 0; i < input.size(); i++) {
            assertSame(before.get(i), input.get(i));
        }
        assertThrows(UnsupportedOperationException.class, () -> InventoryQueries.skus(input).clear());
        assertThrows(UnsupportedOperationException.class, () -> InventoryQueries.lowStock(input, 5).clear());
        assertThrows(UnsupportedOperationException.class, () -> InventoryQueries.topByPrice(input, 5).clear());
    }

    @Test
    void streamsMatchLegacyLoopsOnControlData() throws IOException {
        List<SparePart> input = parts();
        List<String> skus = new ArrayList<>();
        List<SparePart> lowStock = new ArrayList<>();
        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        Map<String, Double> manufacturerValues = new TreeMap<>();
        double warranty = 0;
        for (SparePart part : input) {
            skus.add(part.sku());
            if (part.stock() < 5) {
                lowStock.add(part);
            }
            statistics.accept(part.unitPrice());
            manufacturerValues.merge(part.supplier(), part.stock() * part.unitPrice(), Double::sum);
            warranty += part.warrantyCost();
        }
        assertEquals(skus, InventoryQueries.skus(input));
        assertEquals(lowStock, InventoryQueries.lowStock(input, 5));
        assertEquals(statistics.getSum(), InventoryQueries.priceStatistics(input).getSum(), 1e-9);
        assertEquals(manufacturerValues, InventoryQueries.stockValueByManufacturer(input));
        var analysis = new InventoryReport.Analysis(input, List.of(), new StockValue(28, 5600));
        assertEquals(new HashSet<>(input).size(), analysis.uniqueCount());
        assertEquals(warranty, analysis.warrantyCost(), 1e-9);
        List<SparePart> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparingDouble(SparePart::unitPrice).reversed().thenComparing(SparePart::name));
        assertEquals(sorted.subList(0, 5), InventoryQueries.topByPrice(input, 5));
    }

    @Test
    void controlFixturePreservesOldReportAndPolymorphicTotals() throws IOException {
        List<SparePart> input = parts();
        var analysis = new InventoryReport.Analysis(input, List.of(), new StockValue(28, 5600));
        assertEquals(7, analysis.uniqueCount());
        assertEquals(85, analysis.warrantyCost(), 1e-9);
        String report = InventoryReport.generate(input.stream().map(part -> String.format(Locale.ROOT,
                "%s;%s;%d;%.2f;%s;%s", part.sku(), part.name(), part.stock(), part.unitPrice(),
                part.supplier(), part.kind())).toList());
        assertEquals(String.join(System.lineSeparator(), "Склад автозапчастин — звіт",
                "Коректних записів: 8", "Загальна кількість деталей: 28", "Вартість запасу: 5600.00 грн",
                "Найдорожча деталь: B — Колодки; ціна: 300.00 грн", "Помилок: 0", ""), report);
    }
}
