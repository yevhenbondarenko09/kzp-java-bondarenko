package ua.lpnu.kzp;

import java.util.ArrayList;
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
