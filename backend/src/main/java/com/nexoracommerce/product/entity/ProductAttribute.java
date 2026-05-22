package com.nexoracommerce.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "product_attributes")
public class ProductAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_attributes_id_seq")
    @SequenceGenerator(name = "product_attributes_id_seq", sequenceName = "product_attributes_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "Attribute name is required")
    @Size(max = 100, message = "Attribute name must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
