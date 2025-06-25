package com.moviez.moviez_api.repository;

import com.moviez.moviez_api.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
