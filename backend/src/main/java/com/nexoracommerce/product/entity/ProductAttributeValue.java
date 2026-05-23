package com.nexoracommerce.product.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "attribute")
@Entity
@Table(name = "product_attribute_values", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"attribute_id", "value"})
}, indexes = {
    @Index(name = "idx_attribute_values_attribute", columnList = "attribute_id")
})
public class ProductAttributeValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product attribute is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", nullable = false)
    private ProductAttribute attribute;

    @NotBlank(message = "Attribute value is required")
    @Size(max = 100, message = "Attribute value must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String value;
}
