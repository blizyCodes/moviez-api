package com.moviez.moviez_api.repository;

import com.moviez.moviez_api.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
