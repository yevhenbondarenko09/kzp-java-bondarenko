package ua.lpnu.kzp;

/** Незмінна пара сумарного запасу та його вартості.
 * @param stock загальна кількість деталей
 * @param value загальна вартість запасу
 */
public record StockValue(long stock, double value) {
    /** Перевіряє допустимість підсумкових значень.
     * @param stock невід'ємна кількість
     * @param value скінченна невід'ємна вартість
     */
    public StockValue {
        if (stock < 0) {
            throw new IllegalArgumentException("stock не може бути від'ємним");
        }
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("value має бути скінченним невід'ємним числом");
        }
        value = value == 0 ? 0 : value;
    }
}
