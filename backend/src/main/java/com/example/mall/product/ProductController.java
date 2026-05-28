package com.example.mall.product;

import com.example.mall.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/api/products")
    public ApiResponse<List<Product>> products(@RequestParam(defaultValue = "") String keyword,
                                               @RequestParam(required = false) Long categoryId) {
        List<Product> products;
        if (categoryId != null && !keyword.isBlank()) {
            products = productRepository.findByStatusAndCategoryIdAndNameContainingIgnoreCase(1, categoryId, keyword);
        } else if (categoryId != null) {
            products = productRepository.findByStatusAndCategoryId(1, categoryId);
        } else {
            products = productRepository.findByStatusAndNameContainingIgnoreCase(1, keyword);
        }
        return ApiResponse.success(products);
    }

    @GetMapping("/api/products/{id}")
    public ApiResponse<Product> product(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        return ApiResponse.success(product);
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.success(categoryRepository.findAll());
    }
}
