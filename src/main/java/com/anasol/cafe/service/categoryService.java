package com.anasol.cafe.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.exceptions.UserAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.anasol.cafe.dto.CategoryRequestDTO;
import com.anasol.cafe.entity.Category;
import com.anasol.cafe.repository.categoryRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class categoryService {

	@Autowired
	private categoryRepository categoryRepository;

	@Autowired
	private S3Service s3Service;


	public Category addCat(String categoryName, MultipartFile imageFile) throws IOException {

		if (categoryRepository.existsByCategoryName(categoryName)) {
			throw new UserAlreadyExistsException(
					"Category already exist with Name: " + categoryName);
		}

		Category category = new Category();
		category.setCategoryName(categoryName);
		String url = s3Service.uploadFile(imageFile);
		category.setCategoryImage(url);

		log.info("Category Added Suceesfully");
		return categoryRepository.save(category);
	}


	public List<Category> getAll() {

		List<Category> categories = categoryRepository.findAll();

		return categories.stream().map(cat -> {
			if (cat.getCategoryImage() != null) {
				cat.setCategoryImage(s3Service.getFileUrl(cat.getCategoryImage()));
			}
			return cat;
		}).collect(Collectors.toList());
	}


	public Category updateCat(Long id, String categoryName, MultipartFile imageFile) throws IOException {

		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("CategoryId not found to update with Id: " + id));

		if(categoryName != null) {
			category.setCategoryName(categoryName);
		}

		if(imageFile != null) {
			String url = s3Service.uploadFile(imageFile);
			category.setCategoryImage(url);
		}
		return categoryRepository.save(category);

	}


	public void delelteCat(long id) {

		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cannot find category with id to delte: "+id));

		categoryRepository.deleteById(id);

	}

}