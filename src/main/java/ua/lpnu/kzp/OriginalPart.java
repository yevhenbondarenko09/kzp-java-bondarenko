package ua.lpnu.kzp;

/** Оригінальна автозапчастина з гарантійним резервом 10% ціни. */
public final class OriginalPart extends SparePart {
    /** Створює оригінальну деталь із перевіркою спільних інваріантів.
     * @param sku артикул
     * @param name назва
     * @param stock запас
     * @param unitPrice ціна одиниці
     * @param supplier постачальник
     */
    public OriginalPart(String sku, String name, int stock, double unitPrice, String supplier) {
        super(sku, name, stock, unitPrice, supplier, PartKind.ORIGINAL);
    }

    /** Повертає навчальний гарантійний резерв на одну деталь.
     * @return 10% ціни одиниці
     */
    @Override
    public double warrantyCost() { return unitPrice() * 0.10; }
}
