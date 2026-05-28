package com.example.mall.product;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatusAndNameContainingIgnoreCase(Integer status, String keyword);

    List<Product> findByStatusAndCategoryId(Integer status, Long categoryId);

    List<Product> findByStatusAndCategoryIdAndNameContainingIgnoreCase(Integer status, Long categoryId, String keyword);
}
