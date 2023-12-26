package com.github.androkc.producer.service.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderSendEvent {
    private String productName;
    private String barCode;
    private int quantity;
    private BigDecimal price;
}
