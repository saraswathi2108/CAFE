package com.anasol.cafe.service;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Service
public class S3Service {
	
	@Autowired
	private S3Client s3Client;
	
	@Value("${cloud.aws.s3.bucket-name}")
	private String bucket;
	
	@Autowired
	S3Presigner s3Presigner;
	
	public String uploadFile(MultipartFile imageFile) throws IOException {
		
	    String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
	    String key = "products/" + fileName;

	    PutObjectRequest request = PutObjectRequest.builder()
	            .bucket(bucket)
	            .key(key)
	            .contentType(imageFile.getContentType())
	            .build();

	    s3Client.putObject(request, RequestBody.fromBytes(imageFile.getBytes()));
	    
	    return key; 
	}
	
	
		
	public String getFileUrl(String key) {

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PresignedGetObjectRequest url = s3Presigner.presignGetObject(builder ->
                builder.getObjectRequest(get)
                        .signatureDuration(Duration.ofMinutes(10))
        );

        return url.url().toString();
    }
	
	
	public void deleteFile(String key) {
	    if (key == null || key.isEmpty()) return;
	    
	    try {
	        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
	                .bucket(bucket)
	                .key(key)
	                .build();
	        s3Client.deleteObject(deleteObjectRequest);
	        log.info("Product deleted from s3 {}", key);
	        
	    } catch (Exception e) {
	        System.err.println("Error deleting file from S3: " + e.getMessage());
	    }
	}
		

}
