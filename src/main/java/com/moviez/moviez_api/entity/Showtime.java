package com.moviez.moviez_api.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "showtimes")
@Data
public class Showtime {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "movie_id", nullable = false)
	@JsonIgnore
	private Movie movie;

	@Column(nullable = false)
	private LocalDateTime startTime;

	@Column(nullable = false)
	private LocalDateTime endTime;

	@OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Reservation> reservations;
}
