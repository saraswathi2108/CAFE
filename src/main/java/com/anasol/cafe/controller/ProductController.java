package com.anasol.cafe.controller;

import java.io.IOException;
import java.util.List;

import com.anasol.cafe.entity.NetWeight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/cafe/product")
public class ProductController {

	@Autowired
	private ProductService productService;

	@PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
	@PostMapping(value = "/add/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String addProd(@PathVariable Long categoryId,
						  @RequestParam("productName") String productName,
						  @RequestParam("quantity") Double quantity,
						  @RequestParam( "unit")NetWeight unit,
						  @RequestParam("imageFile") MultipartFile imageFile) throws IOException {

		productService.addProduct(categoryId, productName, quantity,unit, imageFile);

		log.info("New Product added Succesfully {} ", productName);
		return "Product Added Succsfully";
	}


	@PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN','GODOWN_MANAGER')")
	@GetMapping("/Allprod")
	public ResponseEntity<List<ProductResponse>> getAll(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(productService.getAllProducts(pageable));
	}

	@PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
	@PutMapping(value = "/update/{itemId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public String updateProduct(@PathVariable Long itemId,
								@RequestParam(value = "productName", required = false) String productName,
								@RequestParam(value = "quantity", required = false) Double quanity,
								@RequestParam(value = "unit", required = false)NetWeight unit,
								@RequestParam(value = "imageFile", required = false) MultipartFile imageFile) throws IOException {

		productService.updateProduct(itemId, productName, quanity, unit ,imageFile);
		return "Product updated Siccesfulyy";

	}
	@PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN','GODOWN_MANAGER')")
	@GetMapping("/bycategoryid")
	public List<ProductResponse> productByCat(@RequestParam Long categoryId,
											  @RequestParam(defaultValue = "0") int page,
											  @RequestParam(defaultValue = "10") int size) {

		Pageable pageable = PageRequest.of(page, size);
		return productService.getProductsByCategoryId(categoryId, pageable);

	}

	@PreAuthorize("hasAnyRole('ADMIN','GODOWN_MANAGER')")
	@PutMapping("/softdelete/{itemId}")
	public String deleteProduct(@PathVariable Long itemId, @RequestParam boolean status) {

		productService.deleteById(itemId, status);

		log.info("Item deleted Succesfully");
		return "Product deleted Succesfully";
	}
	@PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN','GODOWN_MANAGER')")
	@GetMapping("/inactive")
	public List<ProductResponse> getInactiveProducts() {
		return productService.getAllInactiveProducts();
	}

	@PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN','GODOWN_MANAGER')")
	@GetMapping("/search")
	public List<ProductResponse> search(@RequestParam String name){
		List<ProductResponse> products = productService.searchByName(name);
		log.info("searching by name {}", name);
		return products;
	}


	@PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN','GODOWN_MANAGER')")
	@GetMapping("/out-of-stock")
	public List<ProductResponse> getOutOfStockProducts() {
		log.info("Fetching out-of-stock products");
		return productService.getOutOfStockProducts();
	}

	@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
	@PutMapping("/{productId}/change-category/{categoryId}")
	public ResponseEntity<String> changeProductCategory(
			@PathVariable Long productId,
			@PathVariable Long categoryId) {

		log.info("Changing category for productId={} to categoryId={}", productId, categoryId);

		return ResponseEntity.ok(
				productService.changeProductCategory(productId, categoryId)
		);
	}
}