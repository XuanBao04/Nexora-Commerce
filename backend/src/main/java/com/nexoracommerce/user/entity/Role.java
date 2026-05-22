package com.nexoracommerce.user.entity;

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
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roles_id_seq")
    @SequenceGenerator(name = "roles_id_seq", sequenceName = "roles_id_seq", allocationSize = 1)
    private Long id;

    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name must not exceed 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String name; // e.g., "ROLE_CUSTOMER", "ROLE_ADMIN"
}
