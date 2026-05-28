package com.example.mall.order;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(@NotBlank String receiverName,
                                 @NotBlank String receiverPhone,
                                 @NotBlank String receiverAddress) {
}
