package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class Film {
    public static final LocalDate EARLIEST_RELEASE_DATE =
            LocalDate.of(1895, 12, 28);

    private int id;

    @NotBlank(message = "Название фильма не может быть пустым.")
    private String name;

    @Size(min = 0, max = 200, message = "Описание фильма не может быть больше 200 символов.")
    private String description;

    private LocalDate releaseDate;

    @Positive
    private int duration;

    public Film(String name, String description, String releaseDate, int duration) {
        this.name = name;
        this.description = description;
        this.releaseDate = LocalDate.parse(releaseDate);
        this.duration = duration;
    }
}
