package com.moviez.moviez_api.repository;

import com.moviez.moviez_api.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}
