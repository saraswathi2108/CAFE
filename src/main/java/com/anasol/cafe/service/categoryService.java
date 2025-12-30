package com.anasol.cafe.service;

import java.util.List;

import com.anasol.cafe.exceptions.UserAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.anasol.cafe.dto.CategoryRequestDTO;
import com.anasol.cafe.entity.Category;
import com.anasol.cafe.repository.categoryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class categoryService {

	@Autowired
	private categoryRepository categoryRepository;


	public Category addCat(CategoryRequestDTO categoryRequestDTO) {

		if(categoryRepository.existsByCategoryName(categoryRequestDTO.getCategoryName())) {
			throw new UserAlreadyExistsException("Category already exist with Name: "+categoryRequestDTO.getCategoryName());
		}

		Category category = new Category();
		category.setCategoryName(categoryRequestDTO.getCategoryName());

		log.info("Category Added Suceesfully");
		return categoryRepository.save(category);
	}


	public List<Category> getAll() {
		return categoryRepository.findAll();
	}


	public Category updateCat(Long id, CategoryRequestDTO categoryRequestDTO) {

		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("CategoryId not found with Id: "+id));

		category.setCategoryName(categoryRequestDTO.getCategoryName());
		return categoryRepository.save(category);

	}


	public void delelteCat(long id) {

		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Cannot find category with id to delte: "+id));

		categoryRepository.deleteById(id);

	}

}