package ua.lpnu.kzp;

/** Автозапчастина-замінник з гарантійним резервом 5% ціни. */
public final class ReplacementPart extends SparePart {
    /** Створює замінник із перевіркою спільних інваріантів.
     * @param sku артикул
     * @param name назва
     * @param stock запас
     * @param unitPrice ціна одиниці
     * @param supplier постачальник
     */
    public ReplacementPart(String sku, String name, int stock, double unitPrice, String supplier) {
        super(sku, name, stock, unitPrice, supplier, PartKind.REPLACEMENT);
    }

    /** Повертає навчальний гарантійний резерв на одну деталь.
     * @return 5% ціни одиниці
     */
    @Override
    public double warrantyCost() { return unitPrice() * 0.05; }
}
