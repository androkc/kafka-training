package com.github.androkc.consumer.service.impl;

import com.github.androkc.consumer.domain.Order;
import com.github.androkc.consumer.domain.Status;
import com.github.androkc.consumer.domain.repository.OrderRepository;
import com.github.androkc.consumer.service.dto.OrderDto;
import com.github.androkc.consumer.service.messaging.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final static String FAILED_TO_SAVE = "Failed to save the order with barCode %s .";

    @Override
    public Order save(OrderDto orderDto) {
        Order order = Order.builder()
                .amount(new BigDecimal(orderDto.getQuantity()).multiply(orderDto.getPrice()))
                .productName(orderDto.getProductName())
                .quantity(orderDto.getQuantity())
                .price(orderDto.getPrice())
                .orderDate(LocalDateTime.now())
                .status(Status.APPROVED)
                .barCode(orderDto.getBarCode())
                .build();

        return Optional.of(orderRepository.save(order))
                .orElseThrow(() -> new DataIntegrityViolationException(FAILED_TO_SAVE.formatted(orderDto.getBarCode())));
    }
}
