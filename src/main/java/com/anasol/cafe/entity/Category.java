package com.anasol.cafe.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Category {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    private String categoryName;

	private String categoryImage;

	@Column(nullable = false)
	private boolean active = true;

}
