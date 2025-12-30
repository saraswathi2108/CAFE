package com.anasol.cafe.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.anasol.cafe.entity.Product;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

	List<Product> findByCategoryId(Long categoryId, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Product p WHERE p.id = :id")
	Optional<Product> findByIdWithLock(@Param("id") Long id);

	Page<Product> findByIsActiveTrue(Pageable pageable);

	Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);}