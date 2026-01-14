package com.anasol.cafe.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.anasol.cafe.dto.CategoryRequestDTO;
import com.anasol.cafe.entity.Category;
import com.anasol.cafe.service.categoryService;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/category")
public class categoryController {

	@Autowired
	private categoryService categoryService;


	@PostMapping("/addCat")
	public ResponseEntity<?> addCategory(@RequestParam(value = "categoryName", required = false) String categoryName,
										 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) throws IOException{

		Category category = categoryService .addCat(categoryName, imageFile);
		return ResponseEntity.status(HttpStatus.CREATED).body(category);
	}

	@GetMapping("/getAll")
	public List<Category> getAllCat(){

		return categoryService.getAll();
	}


	@PutMapping(value = "/updateCat/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Category updateCate(@PathVariable Long id,
							   @RequestParam (value = "categoryName", required = false) String categoryName,
							   @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) throws IOException {

		return categoryService.updateCat(id, categoryName, imageFile);

	}


	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	@DeleteMapping("/delete/{id}")
	public String deleteCat(@PathVariable long id) {
		categoryService.delelteCat(id);
		return "Category delted succesfully: "+id;
	}

}