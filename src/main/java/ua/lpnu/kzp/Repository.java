package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Типобезпечне сховище в пам'яті, яке зберігає порядок і повтори.
 * @param <T> тип запису
 */
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    /** Створює порожнє сховище. */
    public Repository() { }

    /** Додає запис.
     * @param item запис, не null
     */
    public void add(T item) { items.add(Objects.requireNonNull(item, "item")); }

    /** Повертає незмінний знімок, незалежний від наступних додавань.
     * @return записи у порядку додавання
     */
    public List<T> all() { return List.copyOf(items); }

    /** Відбирає записи без зміни сховища.
     * @param condition умова для T або його супертипу, не null
     * @return незмінний список збігів
     */
    public List<T> find(Predicate<? super T> condition) {
        return items.stream().filter(Objects.requireNonNull(condition, "condition")).toList();
    }
}
