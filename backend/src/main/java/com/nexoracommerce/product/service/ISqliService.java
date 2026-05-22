package com.nexoracommerce.product.service;

import com.nexoracommerce.product.dto.response.ProductResponse;

import java.util.List;

public interface ISqliService {
    List<ProductResponse> searchVulnerable(String name);

    List<ProductResponse> searchSecure(String name);
}
