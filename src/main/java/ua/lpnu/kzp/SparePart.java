package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/** Незмінна сутність запису складу автозапчастин. */
public final class SparePart {
    private final String sku;
    private final String name;
    private final int stock;
    private final double unitPrice;
    private final String supplier;

    /** Створює деталь, перевіряючи її інваріанти.
     * @param sku артикул
     * @param name назва
     * @param stock невід'ємний запас
     * @param unitPrice скінченна невід'ємна ціна
     * @param supplier постачальник
     */
    public SparePart(String sku, String name, int stock, double unitPrice, String supplier) {
        this.sku = requiredText(sku, "sku");
        this.name = requiredText(name, "name");
        this.supplier = requiredText(supplier, "supplier");
        if (stock < 0) {
            throw new IllegalArgumentException("stock не може бути від'ємним");
        }
        if (!Double.isFinite(unitPrice) || unitPrice < 0) {
            throw new IllegalArgumentException("unitPrice має бути скінченним невід'ємним числом");
        }
        this.stock = stock;
        this.unitPrice = unitPrice == 0 ? 0 : unitPrice;
    }

    /** Розбирає CSV-рядок виду sku;name;stock;unitPrice;supplier.
     * @param line CSV-рядок
     * @return валідована деталь
     */
    public static SparePart fromCsv(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("очікується 5 полів sku;name;stock;unitPrice;supplier");
        }
        String[] names = {"sku", "name", "stock", "unitPrice", "supplier"};
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].strip();
            if (fields[i].isEmpty()) {
                throw new IllegalArgumentException("порожнє поле %s".formatted(names[i]));
            }
        }
        final int stock;
        try {
            stock = Integer.parseInt(fields[2]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("stock має бути цілим числом у діапазоні int", exception);
        }
        final double price;
        try {
            price = Double.parseDouble(fields[3]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("unitPrice має бути числом із десятковою крапкою", exception);
        }
        return new SparePart(fields[0], fields[1], stock, price, fields[4]);
    }

    private static String requiredText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException("порожнє поле %s".formatted(field));
        }
        return value.strip();
    }

    /** Повертає артикул деталі.
     * @return артикул
     */
    public String sku() { return sku; }
    /** Повертає назву деталі.
     * @return назва
     */
    public String name() { return name; }
    /** Повертає кількість деталей на складі.
     * @return кількість
     */
    public int stock() { return stock; }
    /** Повертає ціну за одиницю.
     * @return ціна
     */
    public double unitPrice() { return unitPrice; }
    /** Повертає назву постачальника.
     * @return постачальник
     */
    public String supplier() { return supplier; }

    /** Повертає стабільне текстове подання деталі з десятковою крапкою.
     * @return форматований опис
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s — %s; запас: %d; ціна: %.2f грн; постачальник: %s",
                sku, name, stock, unitPrice, supplier);
    }
}
