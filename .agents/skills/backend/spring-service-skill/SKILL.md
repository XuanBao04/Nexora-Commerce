---
name: spring-service-skill
description: Enforce best practices for the Service layer in Spring Boot applications using Clean Architecture, strict layer isolation, and interface-driven design.
---

## Scope & Activation Rules

Activate when:
- Creating, refactoring, or reviewing Service layer components.
- Designing interfaces for business logic operations.
- Handling transactions, dependency injection, and data mapping within the Service layer.

## System Directives

### DO
- **Interface-Driven Design:** Always define a Service Interface for business operations and implement it in a separate class (e.g., `OrderService` and `OrderServiceImpl`).
- **Strict Layer Isolation:** The Service layer must strictly communicate with the Controller layer using DTOs (Java 21 Records). Use MapStruct for all conversions between Database Entities and DTOs.
- **Constructor Injection:** Always use Constructor Injection for dependencies by utilizing Lombok's `@RequiredArgsConstructor`.
- **Transaction Management:** Annotate the implementation class with `@Transactional(readOnly = true)` by default. Override this behavior by applying `@Transactional` at the method level only for operations that modify data (insert, update, delete).

### DO NOT
- **Leak Entities to Web Layer:** Never return or accept Database Entities in the Controller layer. The Service layer is the strict boundary where Entities are mapped to DTOs.
- **Use Field Injection:** Absolutely do not use `@Autowired` on class fields.
- **Allow Web Leakage:** Do not import or use any web-specific classes (e.g., `HttpServletRequest`, `HttpServletResponse`, `ResponseEntity`) inside the Service layer. Keep the Service layer completely decoupled from the HTTP transport mechanism.

## Standard Reference Pattern

### Service Interface (`OrderService.java`)
```java
package com.nexoracommerce.order.service;

import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;

public interface OrderService {
    
    OrderResponse getOrderById(Long id);
    
    OrderResponse createOrder(OrderRequest request);
    
    void cancelOrder(Long id);
}
```

### Service Implementation (`OrderServiceImpl.java`)
```java
package com.nexoracommerce.order.service.impl;
import lombok.extern.slf4j.Slf4j;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.mapper.OrderMapper;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        Order newOrder = orderMapper.toEntity(request);
        // Business logic here
        Order savedOrder = orderRepository.save(newOrder);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        // Cancellation logic here
        orderRepository.delete(order);
    }
}
```
