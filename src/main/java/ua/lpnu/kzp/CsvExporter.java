package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Експортує плоскі анотовані поля довільного класу в CSV UTF-8. */
public final class CsvExporter {
    private CsvExporter() { }

    /**
     * Записує заголовок і записи; поля сортуються за Java-іменами, включно з базовими класами.
     * Схема та значення перевіряються до відкриття файла на перезапис.
     * @param <T> спільний тип записів
     * @param path вихідний шлях
     * @param type клас зі схемою, потрібний і для порожнього списку
     * @param records записи, без null; null-значення поля експортується порожнім
     * @throws DataStorageException якщо схема, доступ, записи або запис файла некоректні
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(records, "records");
        List<Field> fields = annotatedFields(type);
        StringBuilder csv = new StringBuilder(fields.stream()
                .map(field -> escape(field.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(","))).append('\n');
        for (T record : records) {
            if (record == null || !type.isInstance(record)) {
                throw new DataStorageException("Некоректний тип або null-запис для CSV");
            }
            List<String> values = new ArrayList<>();
            for (Field field : fields) {
                try {
                    values.add(escape(field.get(record)));
                } catch (IllegalAccessException | IllegalArgumentException exception) {
                    throw new DataStorageException("Не вдалося прочитати поле %s".formatted(field.getName()),
                            exception);
                }
            }
            csv.append(String.join(",", values)).append('\n');
        }
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, csv, StandardCharsets.UTF_8);
        } catch (IOException | SecurityException exception) {
            throw new DataStorageException("Не вдалося записати CSV: %s".formatted(path), exception);
        }
    }

    private static List<Field> annotatedFields(Class<?> type) throws DataStorageException {
        List<Field> fields = new ArrayList<>();
        Set<String> headers = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                CsvColumn column = field.getAnnotation(CsvColumn.class);
                if (column == null) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers()) || column.value().isBlank()
                        || !headers.add(column.value()) || !names.add(field.getName())) {
                    throw new DataStorageException("Некоректна CSV-колонка: %s".formatted(field.getName()));
                }
                try {
                    if (!field.trySetAccessible()) {
                        throw new IllegalAccessException(field.getName());
                    }
                } catch (IllegalAccessException | SecurityException | InaccessibleObjectException exception) {
                    throw new DataStorageException("Немає доступу до поля %s".formatted(field.getName()), exception);
                }
                fields.add(field);
            }
        }
        if (fields.isEmpty()) {
            throw new DataStorageException("Клас не має CSV-полів");
        }
        fields.sort(Comparator.comparing(Field::getName));
        return fields;
    }

    private static String escape(Object value) {
        String text = Objects.toString(value, "");
        return text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n")
                ? "\"%s\"".formatted(text.replace("\"", "\"\"")) : text;
    }
}
