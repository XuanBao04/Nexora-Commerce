package com.nexoracommerce.product.service.impl;

import com.nexoracommerce.product.dto.response.ProductResponse;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.mapper.ProductMapper;
import com.nexoracommerce.product.service.ISqliService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class SQLiService implements ISqliService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ProductMapper productMapper;
    
    @SuppressWarnings("unchecked")
    @Override
    public List<ProductResponse> searchVulnerable(String name) {
        String sql = "SELECT * FROM products WHERE name = '" + name + "'";
        List<Product> products = entityManager.createNativeQuery(sql, Product.class).getResultList();
        return productMapper.toResponseList(products);
    }

   
    @SuppressWarnings("unchecked")
    @Override
    public List<ProductResponse> searchSecure(String name) {
        String sql = "SELECT * FROM products WHERE name = :name";
        List<Product> products = entityManager.createNativeQuery(sql, Product.class)
                .setParameter("name", name)
                .getResultList();
        return productMapper.toResponseList(products);
    }
}
