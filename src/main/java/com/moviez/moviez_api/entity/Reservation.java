package com.moviez.moviez_api.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reservations")
@Data
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "showtime_id", nullable = false)
	private Showtime showtime;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "reservation_seats", joinColumns = @JoinColumn(name = "reservation_id"))
	@Column(name = "seat_number")
	private List<Integer> reservedSeats;

	private LocalDateTime bookingTime;
}
