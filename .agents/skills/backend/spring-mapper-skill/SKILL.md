---
name: spring-mapper-skill
description: Enforce compile-time MapStruct code generation, null-safety, data masking, and partial updates for DTO layers in Spring Boot applications.
---

## Scope & Activation Rules

Activate when:
- Creating or refactoring DTO request or response payloads (Records).
- Defining mapper components or interfaces to map between JPA Entities and DTO Records.
- Integrating MapStruct dependencies or maven-compiler-plugin configurations.
- Implementing partial updates using `@MappingTarget`.
- Configuring null-value mapping behaviors for entities and collections.

## System Directives

### DO
- **Use MapStruct Interfaces with Spring Integration**: Always define mappers as interfaces annotated with `@Mapper(componentModel = "spring")` to enable constructor dependency injection.
- **Convert DTOs to Java 21 Records**: All DTO requests and responses must be immutable Java 21 `record` types to prevent state mutability.
- **Implement Strict Data Masking**: Use `@Mapping(target = "...", ignore = true)` to ensure sensitive data (e.g. `password`, `ssn`) is never leaked from Entity to DTO.
- **Enforce Compile-Time Verification**: Configure `unmappedTargetPolicy = ReportingPolicy.IGNORE` or handle all target attributes explicitly to prevent compilation warnings.
- **Utilize @MappingTarget for Partial Updates**: Use `@MappingTarget` combined with `@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` to allow safe merging of incoming DTO updates without overwriting existing database fields with null.
- **Enable List and Set Mapping Auto-Generation**: Define single-object mapping rules and let MapStruct automatically generate batch list/set collection mappings safely.
- **Handle Property Name Discrepancies Explicitly**: Use `@Mapping(source = "entityField", target = "dtoField")` to resolve name variations.
- **Handle Complex Type Conversions and Relations Safely**: Use `expression = "java(...)"` or define specialized default/named methods within the interface when mapping flat DTO fields (e.g., `categoryId`) to nested relational objects or converting between Strings and Custom Enums.

### DO NOT
- Write manual helper methods for DTO conversion inside Service classes. Delegate all mappings to MapStruct compile-time generated mappers.
- Allow database entities to leak directly into controllers or REST responses. Always return DTO records from the service layer.
- Overwrite existing DB values with `null` when performing partial updates. Always check and use `NullValuePropertyMappingStrategy.IGNORE`.
- Use reflection-based mapping tools (e.g., ModelMapper, BeanUtils) which introduce performance degradation and hide type-safety errors until runtime.
- Modify the MapStruct generated implementation files (`*Impl.java`) manually; all modifications must be done on the mapper interface definition.

## Standard Reference Pattern

### Mapper Interface Template

```java
package com.nexoracommerce.common.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface StandardMapper<Entity, CreateRequest, Response, UpdateRequest> {

    @Mapping(target = "password", ignore = true) // Data Masking
    Response toResponse(Entity entity);

    List<Response> toResponseList(List<Entity> entities);

    Set<Response> toResponseSet(Set<Entity> entities); // Anti-pattern safety for Hibernate Set collections

    @Mapping(target = "id", ignore = true)
    Entity toEntity(CreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateRequest request, @MappingTarget Entity entity);
}