package com.ecommerce.main.admin;

public record CategoryDto(
        Long id,
        String name,
        Long parentId,
        String parentName,
        long productCount
) {}
