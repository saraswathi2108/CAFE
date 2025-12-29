package com.anasol.cafe.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anasol.cafe.dto.CategoryRequestDTO;
import com.anasol.cafe.entity.Category;
import com.anasol.cafe.service.categoryService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/category")
public class categoryController {

	@Autowired
	private categoryService categoryService;


	@PostMapping("/addCat")
	public ResponseEntity<?> addCategory(@RequestBody CategoryRequestDTO categoryRequestDTO){

		Category category = categoryService .addCat(categoryRequestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(category);
	}

	@GetMapping("/getAll")
	public List<Category> getAllCat(){

		return categoryService.getAll();
	}

	@PutMapping("/updateCat/{id}")
	public Category updateCate(@PathVariable Long id, @RequestBody CategoryRequestDTO categoryRequestDTO) {
		return categoryService.updateCat(id, categoryRequestDTO);

	}


	@DeleteMapping("/delete/{id}")
	public String deleteCat(@PathVariable long id) {
		categoryService.delelteCat(id);
		return "Category delted succesfully: "+id;
	}

}