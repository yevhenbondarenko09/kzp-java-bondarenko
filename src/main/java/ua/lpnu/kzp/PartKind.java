package ua.lpnu.kzp;

/** Категорії автозапчастин. */
public enum PartKind {
    /** Оригінальна деталь. */
    ORIGINAL("оригінал"),
    /** Деталь-замінник. */
    REPLACEMENT("замінник");

    private final String label;

    PartKind(String label) { this.label = label; }

    /** Повертає підпис категорії.
     * @return український підпис
     */
    public String label() { return label; }
}
