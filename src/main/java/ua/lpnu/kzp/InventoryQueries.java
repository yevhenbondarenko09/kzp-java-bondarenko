package ua.lpnu.kzp;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Чисті потокові запити до поліморфного списку деталей; повтори зберігаються. */
public final class InventoryQueries {
    private InventoryQueries() { }

    /**
     * Відбирає деталі із запасом строго нижче порогу в початковому порядку.
     * @param parts список деталей без null
     * @param threshold невід'ємний поріг
     * @return незмінний список відібраних деталей
     * @throws IllegalArgumentException якщо поріг від'ємний
     */
    public static List<SparePart> lowStock(List<SparePart> parts, int threshold) {
        Objects.requireNonNull(parts, "parts");
        if (threshold < 0) {
            throw new IllegalArgumentException("Поріг не може бути від'ємним");
        }
        return parts.stream().filter(part -> part.stock() < threshold).toList();
    }

    /**
     * Повертає артикули в початковому порядку, включно з повторами.
     * @param parts список деталей без null
     * @return незмінний список артикулів
     */
    public static List<String> skus(List<SparePart> parts) {
        return Objects.requireNonNull(parts, "parts").stream().map(SparePart::sku).toList();
    }

    /**
     * Групує суму stock * unitPrice за виробником у порядку назв ключів.
     * За домовленістю ЛР4 п'яте поле CSV supplier використовується як виробник.
     * Повторені записи враховуються окремо.
     * @param parts список деталей без null
     * @return нова карта виробник → вартість запасу
     * @throws IllegalArgumentException якщо добуток або сума виходить за діапазон double
     */
    public static Map<String, Double> stockValueByManufacturer(List<SparePart> parts) {
        Map<String, Double> result = Objects.requireNonNull(parts, "parts").stream()
                .collect(Collectors.groupingBy(SparePart::supplier, TreeMap::new,
                        Collectors.summingDouble(part -> part.stock() * part.unitPrice())));
        if (result.values().stream().anyMatch(value -> !Double.isFinite(value))) {
            throw new IllegalArgumentException("вартість запасу виробника виходить за числовий діапазон");
        }
        return result;
    }

    /**
     * Обчислює count, sum, min, max і average ціни одиниці без зважування запасом.
     * Для порожнього списку count/sum = 0, min/max мають стандартні нескінченні значення.
     * @param parts список деталей без null
     * @return новий об'єкт статистики
     * @throws IllegalArgumentException якщо сума цін виходить за діапазон double
     */
    public static DoubleSummaryStatistics priceStatistics(List<SparePart> parts) {
        DoubleSummaryStatistics result = Objects.requireNonNull(parts, "parts").stream()
                .collect(Collectors.summarizingDouble(SparePart::unitPrice));
        if (!Double.isFinite(result.getSum())) {
            throw new IllegalArgumentException("сума цін виходить за числовий діапазон");
        }
        return result;
    }

    /**
     * Впорядковує за спаданням ціни, потім за зростанням назви й обмежує результат.
     * Повні нічиї зберігають вхідний порядок; завелике N повертає всі записи.
     * @param parts список деталей без null
     * @param n невід'ємна максимальна кількість результатів; для top-5 передати 5
     * @return незмінний впорядкований список
     * @throws IllegalArgumentException якщо N від'ємне
     */
    public static List<SparePart> topByPrice(List<SparePart> parts, int n) {
        Objects.requireNonNull(parts, "parts");
        if (n < 0) {
            throw new IllegalArgumentException("Ліміт не може бути від'ємним");
        }
        return parts.stream()
                .sorted(Comparator.comparingDouble(SparePart::unitPrice).reversed()
                        .thenComparing(SparePart::name))
                .limit(n).toList();
    }

    /**
     * Шукає перший запис за точним артикулом незалежно від підтипу.
     * @param parts список деталей без null
     * @param sku артикул, не null
     * @return перша деталь або Optional.empty(), якщо збігу немає
     */
    public static Optional<SparePart> findBySku(List<SparePart> parts, String sku) {
        Objects.requireNonNull(sku, "sku");
        return Objects.requireNonNull(parts, "parts").stream()
                .filter(part -> part.sku().equals(sku)).findFirst();
    }
}
