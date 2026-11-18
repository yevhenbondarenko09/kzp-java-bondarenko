package ua.lpnu.kzp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Задає заголовок CSV-поля, доступний рефлексії під час виконання. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    /** Повертає текст заголовка.
     * @return непорожня назва колонки
     */
    String value();
}
