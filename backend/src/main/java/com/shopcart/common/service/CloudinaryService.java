package com.shopcart.common.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload a file to Cloudinary under the 'shopcart/products' folder
     * @param file the multipart file to upload
     * @return map with details containing 'url' and 'public_id'
     * @throws IOException if network or file reading fails
     */
    public Map upload(MultipartFile file) throws IOException {
        log.info("Uploading file to Cloudinary: name={}, size={}", file.getOriginalFilename(), file.getSize());
        Map options = ObjectUtils.asMap(
                "folder", "shopcart/products",
                "overwrite", true,
                "resource_type", "image"
        );
        return cloudinary.uploader().upload(file.getBytes(), options);
    }

    /**
     * Delete an asset from Cloudinary using its public ID
     * @param publicId the public ID of the asset
     * @return map with the deletion results
     * @throws IOException if API call fails
     */
    public Map delete(String publicId) throws IOException {
        log.info("Deleting file from Cloudinary with publicId: {}", publicId);
        if (publicId == null || publicId.trim().isEmpty()) {
            return ObjectUtils.emptyMap();
        }
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
