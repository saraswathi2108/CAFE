package com.anasol.cafe.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.anasol.cafe.dto.ProductResponse;
import com.anasol.cafe.service.ProductService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/product")
public class ProductController {

	@Autowired
	private ProductService productService;


	@PostMapping(value = "/add/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String addProd(@PathVariable Long categoryId,
						  @RequestParam("productName") String productName,
						  @RequestParam("quantity") Long quantity,
						  @RequestParam("imageFile") MultipartFile imageFile) throws IOException {

		productService.addProduct(categoryId, productName, quantity, imageFile);

		log.info("New Product added Succesfully {} ", productName);
		return "Product Added Succsfully";
	}

	@GetMapping("/Allprod")
	public ResponseEntity<List<ProductResponse>> getAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(productService.getAllProducts(pageable));
	}


	@PutMapping(value = "/update/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String updateProduct(@PathVariable Long itemId,
								@RequestParam(value = "productName", required = false) String productName,
								@RequestParam(value = "quantity", required = false) Long quanity,
								@RequestParam(value = "imageFile", required = false) MultipartFile imageFile) throws IOException {

		productService.updateProduct(itemId, productName, quanity, imageFile);
		return "Product updated Siccesfulyy";

	}

	@GetMapping("/bycategoryid")
	public List<ProductResponse> productByCat(@RequestParam Long categoryId,
											  @RequestParam(defaultValue = "0") int page,
											  @RequestParam(defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		return productService.getProductsByCategoryId(categoryId, pageable);

	}


	@PutMapping("/softdelete/{itemId}")
	public String deleteProduct(@PathVariable Long itemId, @RequestParam boolean status) {

		productService.deleteById(itemId, status);

		log.info("Item deleted Succesfully");
		return "Product deleted Succesfully";
	}
}