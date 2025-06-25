package com.moviez.moviez_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
public class Movie {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Lob
	private String description;

	private String posterImageUrl;

	@Column(nullable = false)
	private String genre;

	@OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Showtime> showtimes;
}
