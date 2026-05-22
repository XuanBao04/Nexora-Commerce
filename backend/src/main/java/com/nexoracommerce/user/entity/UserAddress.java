package com.nexoracommerce.user.entity;

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
@ToString(exclude = "user")
@Entity
@Table(name = "user_addresses", indexes = {
    @Index(name = "idx_user_addresses_user", columnList = "user_id")
})
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_addresses_id_seq")
    @SequenceGenerator(name = "user_addresses_id_seq", sequenceName = "user_addresses_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must not exceed 100 characters")
    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line must not exceed 255 characters")
    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @NotNull(message = "Default flag is required")
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
}
