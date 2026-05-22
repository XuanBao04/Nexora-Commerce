package com.nexoracommerce.brand.entity;

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
@Table(name = "brands")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brands_id_seq")
    @SequenceGenerator(name = "brands_id_seq", sequenceName = "brands_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "Brand name is required")
    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Brand slug is required")
    @Size(max = 100, message = "Brand slug must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String slug;
}
