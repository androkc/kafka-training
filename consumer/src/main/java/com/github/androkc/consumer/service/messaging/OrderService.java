package com.github.androkc.consumer.service.messaging;

import com.github.androkc.consumer.domain.Order;
import com.github.androkc.consumer.service.dto.OrderDto;

public interface OrderService {
    Order save(OrderDto orderDto);
}
