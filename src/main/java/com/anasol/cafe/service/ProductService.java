package com.anasol.cafe.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.anasol.cafe.dto.ProductResponse;
import com.anasol.cafe.entity.Category;
import com.anasol.cafe.entity.Product;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.repository.ProductRepo;
import com.anasol.cafe.repository.categoryRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {

	@Autowired
	private categoryRepository categoryRepository;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private ProductRepo productRepo;

	public Product addProduct(Long categoryId, String productName, Long quantity, MultipartFile imageFile) throws IOException {

		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new RuntimeException("Category not found to add Product"));

		Product product = new Product();

		product.setCategory(category);
		product.setProductName(productName);
		product.setQuantity(quantity);
		product.setActive(true);

		String url = s3Service.uploadFile(imageFile);
		product.setPImage(url);

		return productRepo.save(product);



	}

	public List<ProductResponse> getAllProducts(org.springframework.data.domain.Pageable pageable) {
		return productRepo.findByIsActiveTrue(pageable)
				.stream()
				.map(p -> {
					String presignedUrl = s3Service.getFileUrl(p.getPImage());

					return new ProductResponse(
							p.getId(),
							p.getProductName(),
							p.getQuantity(),
							presignedUrl,
							p.getCategory().getCategoryName()
					);
				})
				.toList();
	}

	public Product updateProduct(Long itemId, String productName, Long quanity, MultipartFile imageFile) throws IOException {

		Product product = productRepo.findById(itemId)
				.orElseThrow(() -> new RuntimeException("Product not found with id to update: "+ itemId));

		if(productName != null) {
			product.setProductName(productName);
		}

		if(quanity != null) {
			product.setQuantity(quanity);
		}

		if(imageFile != null && !imageFile.isEmpty()) {

			String oldKey = product.getPImage();

			if(oldKey != null) {
				s3Service.deleteFile(oldKey);
			}

			String url = s3Service.uploadFile(imageFile);
			product.setPImage(url);


		}

		return productRepo.save(product);

	}


	public List<ProductResponse> getProductsByCategoryId(Long categoryId, Pageable pageable) {

		Page<Product> pageProducts = productRepo.findByCategoryIdAndIsActiveTrue(categoryId, pageable);

		if (pageProducts.isEmpty()) {
			throw new ResourceNotFoundException("No active products found for category id: " + categoryId);
		}

		return pageProducts.getContent()
				.stream()
				.map(p -> new ProductResponse(
						p.getId(),
						p.getProductName(),
						p.getQuantity(),
						s3Service.getFileUrl(p.getPImage()),
						p.getCategory().getCategoryName()
				))
				.toList();
	}





	public void deleteById(Long itemId, boolean status) {

		Product product = productRepo.findById(itemId)
				.orElseThrow(() -> new RuntimeException("Product not found to delete with id: "+ itemId));

		product.setActive(status);

		productRepo.save(product);
	}


	public Product productById(Long itemId) {

		Product product = productRepo.findById(itemId)
				.orElseThrow(() -> new RuntimeException("Product not found with Id: "+ itemId));

		log.info("Fetching product by Id {}", itemId);
		return product;
	}


}