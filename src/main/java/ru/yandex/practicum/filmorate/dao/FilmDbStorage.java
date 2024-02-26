package ru.yandex.practicum.filmorate.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.FilmCreatePreviouslyException;
import ru.yandex.practicum.filmorate.exception.FilmNotFountException;
import ru.yandex.practicum.filmorate.model.film.Film;
import ru.yandex.practicum.filmorate.model.film.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component("Film_DAO")
@Qualifier("Film_DAO")
@Primary
public class FilmDbStorage implements FilmStorage {
    private final Logger log = LoggerFactory.getLogger(FilmDbStorage.class);
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Film findFilmById(Integer filmId) throws SQLException, IOException {
        String sql = "SELECT " +
                "f.id_film, " +
                "f.name, " +
                "f.description, " +
                "f.release_date, " +
                "f.duration, " +
                "f.rate, " +
                "f.id_mpa, " +
                "ml.mpa " +
                "FROM film f " +
                "JOIN mpa_list ml ON f.id_mpa = ml.id_mpa " +
                "WHERE f.id_film = ?";
        List<Film> result = jdbcTemplate.query(sql, this::makeFilm, filmId);
        if (result.isEmpty()) {
            log.debug(MessageFormat.format("Фильм с id: {0} не найден", filmId) +
                    "\n" + getAllFilm());
            return null;
        } else {
            log.info("Найден фильм: {} {} ", result.get(0).getId(), result.get(0).getName());
            return result.get(0);
        }
    }

    @Override
    public List<Film> getAllFilm() {
        String sql = "SELECT " +
                "f.id_film, " +
                "f.name, " +
                "f.description, " +
                "f.release_date, " +
                "f.duration, " +
                "f.rate, " +
                "f.id_mpa, " +
                "ml.mpa ml_name " +
                "FROM film f " +
                "JOIN mpa_list ml ON f.id_mpa = ml.id_mpa ORDER BY f.id_film";

        return jdbcTemplate.query(sql, this::makeFilm);
    }

    @Override
    public Film createFilm(Film film) throws SQLException, IOException {
        if (findFilmById(film.getId()) == null) {
            SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate)
                    .withTableName("film")
                    .usingGeneratedKeyColumns("id_film");

            Map<String, Object> values = new HashMap<>();
            values.put("name", film.getName());
            values.put("description", film.getDescription());
            values.put("release_date", film.getReleaseDate());
            values.put("duration", film.getDuration());
            values.put("id_mpa", film.getMpa().getId());
            film.setId(insert.executeAndReturnKey(values).intValue());

            return film;
        } else {
            throw new FilmCreatePreviouslyException(MessageFormat.format("Фильм с id: {0} уже зарегистрирован",
                    film.getId()));
        }
    }

    @Override
    public Film updateFilm(Film film) throws SQLException, IOException {
        if (findFilmById(film.getId()) != null) {
            String sql =
                    "UPDATE film SET " +
                            "name = ?, " +
                            "description= ?, " +
                            "release_date = ?, " +
                            "duration = ?, id_mpa = ? " +
                            "WHERE id_film = ?";
            jdbcTemplate.update(sql,
                    film.getName(),
                    film.getDescription(),
                    film.getReleaseDate(),
                    film.getDuration(),
                    film.getMpa().getId(),
                    film.getId());

            return findFilmById(film.getId());
        } else {
            throw new FilmNotFountException(MessageFormat.format("Фильм с id: {0} не найден", film.getId()));
        }
    }

    @Override
    public void saveLikes(Film film) {
        jdbcTemplate.update("DELETE FROM film_likes WHERE id_film = ?", film.getId());
        String sql = "INSERT INTO film_likes (id_film, id_user) VALUES(?, ?)";
        Set<Integer> likes = film.getLikes();
        for (var like : likes) {
            jdbcTemplate.update(sql, film.getId(), like);
        }
    }

    private void loadLikes(Film film) {
        String sql = "SELECT id_user FROM film_likes WHERE id_film = ?";
        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(sql, film.getId());
        while (sqlRowSet.next()) {
            film.setLike(sqlRowSet.getInt("id_user"));
        }
    }

    private Film makeFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film(
                rs.getInt("id_film"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("release_date"),
                rs.getInt("duration"),
                rs.getDouble("rate"),
                new Mpa(rs.getInt("id_mpa"), rs.getString("mpa"))
        );
        loadLikes(film);
        return film;
    }
}
