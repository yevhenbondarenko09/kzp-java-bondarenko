package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Перевіряє записи варіанта 4 й формує один звіт для консолі та файла. */
public final class InventoryReport {
    private InventoryReport() { }

    /**
     * Обчислює показники інвентаризації; хибні рядки не змінюють стан звіту.
     * @param lines рядки CSV без заголовка
     * @return звіт із діагностикою у вихідному порядку
     */
    public static String generate(List<String> lines) {
        Analysis analysis = analyze(lines);
        List<SparePart> parts = analysis.parts();
        List<String> errors = analysis.errors();
        StockValue total = analysis.total();
        return format(parts, errors, total);
    }

    /** Розбирає вхід один раз для старого звіту й нових предметних операцій. */
    static Analysis analyze(List<String> lines) {
        List<SparePart> parts = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        StockValue total = new StockValue(0, 0);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (index == 0 && line != null && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            try {
                SparePart part = SparePart.fromCsv(line);
                double itemValue = part.stock() * part.unitPrice();
                double nextValue = total.value() + itemValue;
                if (!Double.isFinite(itemValue) || !Double.isFinite(nextValue)) {
                    throw new IllegalArgumentException("вартість запасу виходить за числовий діапазон");
                }
                long nextStock = Math.addExact(total.stock(), part.stock());
                StockValue next = new StockValue(nextStock, nextValue);
                parts.add(part);
                total = next;
            } catch (IllegalArgumentException | ArithmeticException exception) {
                String reason = exception instanceof ArithmeticException
                        ? "загальна кількість деталей виходить за числовий діапазон"
                        : exception.getMessage();
                errors.add(String.format(Locale.ROOT, "Рядок %d: %s", index + 1, reason));
            }
        }
        return new Analysis(parts, errors, total);
    }

    /** Зберігає прийняті записи окремо від множини унікальних деталей. */
    record Analysis(List<SparePart> parts, List<String> errors, StockValue total) {
        Analysis {
            parts = List.copyOf(parts);
            errors = List.copyOf(errors);
        }

        int uniqueCount() { return new HashSet<>(parts).size(); }

        /** Сума резервів на одиницю за всіма записами, включно з повторами. */
        double warrantyCost() {
            double result = 0;
            for (SparePart part : parts) {
                result += part.warrantyCost();
                if (!Double.isFinite(result)) {
                    throw new IllegalArgumentException("гарантійна вартість виходить за числовий діапазон");
                }
            }
            return result;
        }
    }

    private static String format(List<SparePart> parts, List<String> errors, StockValue total) {
        SparePart highest = null;
        for (SparePart part : parts) {
            if (highest == null || part.unitPrice() > highest.unitPrice()) {
                highest = part;
            }
        }
        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT,
                "Склад автозапчастин — звіт%nКоректних записів: %d%nЗагальна кількість деталей: %d%nВартість запасу: %.2f грн%n",
                parts.size(), total.stock(), total.value()));
        if (highest == null) {
            report.append("Найдорожча деталь: немає коректних записів").append(System.lineSeparator());
        } else {
            report.append(String.format(Locale.ROOT, "Найдорожча деталь: %s — %s; ціна: %.2f грн%n",
                    highest.sku(), highest.name(), highest.unitPrice()));
        }
        report.append(String.format(Locale.ROOT, "Помилок: %d%n", errors.size()));
        for (String error : errors) {
            report.append(error).append(System.lineSeparator());
        }
        return report.toString();
    }
}
