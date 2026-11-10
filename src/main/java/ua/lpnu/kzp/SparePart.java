package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/** Незмінна сутність запису складу автозапчастин. */
public abstract class SparePart {
    private final String sku;
    private final String name;
    private final int stock;
    private final double unitPrice;
    private final String supplier;
    private final PartKind kind;

    /** Створює деталь, перевіряючи її інваріанти.
     * @param sku артикул
     * @param name назва
     * @param stock невід'ємний запас
     * @param unitPrice скінченна невід'ємна ціна
     * @param supplier постачальник
     * @param kind категорія деталі
     */
    protected SparePart(String sku, String name, int stock, double unitPrice, String supplier, PartKind kind) {
        this(validate(sku, name, stock, unitPrice, supplier, kind));
    }

    private SparePart(Validated state) {
        this.sku = state.sku();
        this.name = state.name();
        this.stock = state.stock();
        this.unitPrice = state.unitPrice();
        this.supplier = state.supplier();
        this.kind = state.kind();
    }

    private record Validated(String sku, String name, int stock, double unitPrice, String supplier, PartKind kind) { }

    // Валідація до виклику конструктора запобігає CT_CONSTRUCTOR_THROW у відкритій ієрархії.
    private static Validated validate(String sku, String name, int stock, double unitPrice, String supplier, PartKind kind) {
        String checkedSku = requiredText(sku, "sku");
        String checkedName = requiredText(name, "name");
        String checkedSupplier = requiredText(supplier, "supplier");
        Objects.requireNonNull(kind, "kind");
        if (stock < 0) {
            throw new IllegalArgumentException("stock не може бути від'ємним");
        }
        if (!Double.isFinite(unitPrice) || unitPrice < 0) {
            throw new IllegalArgumentException("unitPrice має бути скінченним невід'ємним числом");
        }
        return new Validated(checkedSku, checkedName, stock, unitPrice == 0 ? 0 : unitPrice, checkedSupplier, kind);
    }

    /** Розбирає CSV-рядок виду sku;name;stock;unitPrice;supplier[;kind].
     * Без kind створює OriginalPart; явна категорія має бути ORIGINAL або REPLACEMENT.
     * @param line CSV-рядок
     * @return валідована деталь
     */
    public static SparePart fromCsv(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        String[] fields = line.split(";", -1);
        if (fields.length != 5 && fields.length != 6) {
            throw new IllegalArgumentException("очікується 5 або 6 полів sku;name;stock;unitPrice;supplier[;kind]");
        }
        String[] names = {"sku", "name", "stock", "unitPrice", "supplier", "kind"};
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
        PartKind kind = PartKind.ORIGINAL;
        if (fields.length == 6) {
            try {
                kind = PartKind.valueOf(fields[5]);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("kind має бути ORIGINAL або REPLACEMENT", exception);
            }
        }
        return switch (kind) {
            case ORIGINAL -> new OriginalPart(fields[0], fields[1], stock, price, fields[4]);
            case REPLACEMENT -> new ReplacementPart(fields[0], fields[1], stock, price, fields[4]);
        };
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
    public final String sku() { return sku; }
    /** Повертає назву деталі.
     * @return назва
     */
    public final String name() { return name; }
    /** Повертає кількість деталей на складі.
     * @return кількість
     */
    public final int stock() { return stock; }
    /** Повертає ціну за одиницю.
     * @return ціна
     */
    public final double unitPrice() { return unitPrice; }
    /** Повертає назву постачальника.
     * @return постачальник
     */
    public final String supplier() { return supplier; }

    /** Повертає категорію деталі.
     * @return категорія
     */
    public final PartKind kind() { return kind; }

    /** Обчислює гарантійну вартість на одиницю незалежно від запасу.
     * @return гарантійна вартість у гривнях
     */
    public abstract double warrantyCost();

    /** Визначає логічний дублікат за конкретним підтипом і артикулом.
     * @param other інший об'єкт
     * @return чи збігаються підтип і артикул
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        return sku.equals(((SparePart) other).sku);
    }

    /** Повертає хеш підтипу та артикулу.
     * @return хеш-код, узгоджений з equals
     */
    @Override
    public final int hashCode() { return Objects.hash(getClass(), sku); }

    /** Повертає стабільне текстове подання деталі з десятковою крапкою.
     * @return форматований опис
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s — %s; запас: %d; ціна: %.2f грн; постачальник: %s",
                sku, name, stock, unitPrice, supplier);
    }
}
