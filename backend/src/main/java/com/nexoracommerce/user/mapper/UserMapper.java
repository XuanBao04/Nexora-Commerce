package com.nexoracommerce.user.mapper;

import com.nexoracommerce.user.dto.response.UserAddressResponse;
import com.nexoracommerce.user.dto.response.UserProfileResponse;
import com.nexoracommerce.user.entity.Role;
import com.nexoracommerce.user.entity.User;
import com.nexoracommerce.user.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @org.mapstruct.Mapping(target = "active", constant = "true")
    @org.mapstruct.Mapping(target = "role", expression = "java(user.getRoles() != null && !user.getRoles().isEmpty() ? user.getRoles().iterator().next().getName() : \"ROLE_CUSTOMER\")")
    UserProfileResponse toProfileResponse(User user);

    UserAddressResponse toAddressResponse(UserAddress address);

    List<UserAddressResponse> toAddressResponseList(List<UserAddress> addresses);

    Set<UserAddressResponse> toAddressResponseSet(Set<UserAddress> addresses);

    default String map(Role role) {
        if (role == null) {
            return null;
        }
        return role.getName();
    }
}
