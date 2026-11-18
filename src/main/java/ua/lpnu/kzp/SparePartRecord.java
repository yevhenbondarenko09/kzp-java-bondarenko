package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Плоский запис варіанта 4; рівність і хеш враховують усі п'ять полів.
 * @param article артикул
 * @param name назва
 * @param quantity невід'ємна кількість
 * @param price скінченна невід'ємна ціна одиниці
 * @param manufacturer виробник
 */
public record SparePartRecord(
        @CsvColumn("артикул") String article,
        @CsvColumn("назва") String name,
        @CsvColumn("кількість") int quantity,
        @CsvColumn("ціна") double price,
        @CsvColumn("виробник") String manufacturer) {
    private static final List<String> HEADER = List.of("артикул", "виробник", "назва", "ціна", "кількість");

    /** Перевіряє поля без обрізання значень тексту.
     * @param article артикул
     * @param name назва
     * @param quantity кількість
     * @param price ціна
     * @param manufacturer виробник
     */
    public SparePartRecord {
        if (article == null || article.isBlank() || name == null || name.isBlank()
                || manufacturer == null || manufacturer.isBlank()) {
            throw new IllegalArgumentException("Артикул, назва й виробник не можуть бути порожніми");
        }
        if (quantity < 0 || !Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("Кількість і ціна мають бути невід'ємними, ціна має бути скінченною");
        }
        price = price == 0 ? 0 : price;
    }

    /** Створює проєкцію старої моделі без зміни її типу й стану.
     * @param part деталь; supplier використовується як виробник за домовленістю ЛР4
     * @return плоский запис без інформації про підтип
     */
    public static SparePartRecord fromPart(SparePart part) {
        Objects.requireNonNull(part, "part");
        return new SparePartRecord(part.sku(), part.name(), part.stock(), part.unitPrice(), part.supplier());
    }

    /** Розбирає один логічний запис CSV, який може містити quoted multiline-поля.
     * @param text один запис без заголовка
     * @return відновлений запис
     * @throws DataStorageException якщо записів не один або дані некоректні
     */
    public static SparePartRecord fromCsv(String text) throws DataStorageException {
        List<List<String>> rows = CsvParser.parseDocument(text);
        if (rows.size() != 1) {
            throw new DataStorageException("Очікується один CSV-запис");
        }
        return fromFields(rows.getFirst());
    }

    /** Відновлює запис із полів у порядку article, manufacturer, name, price, quantity.
     * @param fields п'ять полів
     * @return валідований запис
     * @throws DataStorageException якщо кількість полів або значення некоректні; cause збережено
     */
    public static SparePartRecord fromFields(List<String> fields) throws DataStorageException {
        if (fields == null || fields.size() != 5 || fields.stream().anyMatch(Objects::isNull)) {
            throw new DataStorageException("Очікується 5 непорожніх посилань на CSV-поля");
        }
        try {
            return new SparePartRecord(fields.get(0), fields.get(2), Integer.parseInt(fields.get(4)),
                    Double.parseDouble(fields.get(3)), fields.get(1));
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException("Некоректні поля деталі: %s".formatted(exception.getMessage()), exception);
        }
    }

    /** Читає UTF-8 CSV із точним заголовком; порожній файл і header-only дають порожній список.
     * @param path вхідний файл
     * @return незмінний список у порядку файла
     * @throws DataStorageException якщо заголовок, записи або файл некоректні
     */
    public static List<SparePartRecord> read(Path path) throws DataStorageException {
        Objects.requireNonNull(path, "path");
        final String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException | SecurityException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: %s".formatted(path), exception);
        }
        List<List<String>> rows = CsvParser.parseDocument(text);
        if (rows.isEmpty()) {
            return List.of();
        }
        if (!rows.getFirst().equals(HEADER)) {
            throw new DataStorageException("Некоректний CSV-заголовок деталі");
        }
        List<SparePartRecord> records = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            try {
                records.add(fromFields(rows.get(index)));
            } catch (DataStorageException exception) {
                throw new DataStorageException("CSV-запис %d: %s".formatted(index + 1, exception.getMessage()), exception);
            }
        }
        return List.copyOf(records);
    }
}
