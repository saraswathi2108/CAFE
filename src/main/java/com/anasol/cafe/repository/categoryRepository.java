package com.anasol.cafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.anasol.cafe.entity.Category;

@Repository
public interface categoryRepository extends JpaRepository<Category, Long> {

    boolean existsByCategoryName(String categoryName);
}
