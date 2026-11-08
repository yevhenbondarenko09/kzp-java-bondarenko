package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Перевіряє записи варіанта 4 й формує один звіт для консолі та файла. */
public final class InventoryReport {
    private InventoryReport() {
    }

    /**
     * Обчислює кількість записів, запас, вартість та найдорожчу деталь.
     * Хибний рядок не змінює показників і не зупиняє обробку.
     * За однакової максимальної ціни вибирається перший коректний запис.
     *
     * @param lines рядки CSV без заголовка
     * @return звіт із системними перенесеннями рядків і діагностикою
     */
    public static String generate(List<String> lines) {
        int validCount = 0;
        long totalStock = 0;
        double totalValue = 0;
        double highestPrice = -1;
        String highestSku = "";
        String highestName = "";
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (index == 0 && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            try {
                String[] fields = fields(line);
                int stock = parseStock(fields[2]);
                double price = parsePrice(fields[3]);
                double nextValue = totalValue + stock * price;
                if (!Double.isFinite(nextValue)) {
                    throw new IllegalArgumentException("вартість запасу виходить за числовий діапазон");
                }
                validCount++;
                totalStock += stock;
                totalValue = nextValue;
                if (price > highestPrice) {
                    highestPrice = price;
                    highestSku = fields[0];
                    highestName = fields[1];
                }
            } catch (IllegalArgumentException exception) {
                errors.add(String.format(Locale.ROOT, "Рядок %d: %s", index + 1, exception.getMessage()));
            }
        }
        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT,
                "Склад автозапчастин — звіт%nКоректних записів: %d%nЗагальна кількість деталей: %d%nВартість запасу: %.2f грн%n",
                validCount, totalStock, totalValue));
        if (validCount == 0) {
            report.append(String.format(Locale.ROOT, "Найдорожча деталь: немає коректних записів%n"));
        } else {
            report.append(String.format(Locale.ROOT, "Найдорожча деталь: %s — %s; ціна: %.2f грн%n",
                    highestSku, highestName, highestPrice));
        }
        report.append(String.format(Locale.ROOT, "Помилок: %d%n", errors.size()));
        for (String error : errors) {
            report.append(error).append(System.lineSeparator());
        }
        return report.toString();
    }

    /** Перевіряє структуру й непорожні поля, зберігаючи порожнє останнє поле. */
    private static String[] fields(String line) {
        if (line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("очікується 5 полів sku;name;stock;unitPrice;supplier");
        }
        String[] names = {"sku", "name", "stock", "unitPrice", "supplier"};
        for (int index = 0; index < fields.length; index++) {
            fields[index] = fields[index].strip();
            if (fields[index].isEmpty()) {
                throw new IllegalArgumentException("порожнє поле %s".formatted(names[index]));
            }
        }
        return fields;
    }

    /** Перетворює запас на невід'ємне int із поясненням помилки. */
    private static int parseStock(String text) {
        final int stock;
        try {
            stock = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("stock має бути цілим числом у діапазоні int", exception);
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock не може бути від'ємним");
        }
        return stock;
    }

    /** Перетворює ціну, відхиляючи від'ємні значення, NaN та нескінченність. */
    private static double parsePrice(String text) {
        final double price;
        try {
            price = Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("unitPrice має бути числом із десятковою крапкою", exception);
        }
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("unitPrice має бути скінченним невід'ємним числом");
        }
        return price == 0 ? 0 : price;
    }
}
