package com.joaocarlos.order_service.service;

import com.joaocarlos.order_service.domain.Order;
import com.joaocarlos.order_service.domain.OrderItem;
import com.joaocarlos.order_service.domain.OrderStatus;
import com.joaocarlos.order_service.dto.*;
import com.joaocarlos.order_service.exception.OrderNotFoundException;
import com.joaocarlos.order_service.exception.OrderStateException;
import com.joaocarlos.order_service.messaging.OrderProducer;
import com.joaocarlos.order_service.messaging.dto.OrderCreatedEvent;
import com.joaocarlos.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    public OrderService(OrderRepository orderRepository, OrderProducer orderProducer) {
        this.orderRepository = orderRepository;
        this.orderProducer = orderProducer;
    }

    @Transactional
    public OrderCreationResult createOrder(String idempotencyKey, CreateOrderRequest request) {
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder.isPresent()) {
            logger.info("Pedido já existe para a chave de idempotência {}. Retornando pedido existente.", idempotencyKey);
            return new OrderCreationResult(toResponse(existingOrder.get()), false);
        }

        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setCustomerId(request.customerId());
        order.setDeliveryAddress(request.deliveryAddress());
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        List<OrderItem> items = request.items().stream().map(itemRequest -> {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemRequest.productId());
            item.setProductName(itemRequest.productName());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            return item;
        }).toList();

        order.setItems(items);

        BigDecimal totalValue = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalValue(totalValue);

        Order saved = orderRepository.save(order);
        logger.info("Pedido {} criado com sucesso.", saved.getId());

        // Publica evento Kafka para iniciar a Saga
        List<OrderCreatedEvent.OrderItemEvent> eventItems = saved.getItems().stream()
                .map(i -> new OrderCreatedEvent.OrderItemEvent(i.getProductId(), i.getQuantity()))
                .toList();
        OrderCreatedEvent event = new OrderCreatedEvent(
                saved.getId(),
                saved.getCustomerId(),
                saved.getTotalValue().doubleValue(),
                saved.getDeliveryAddress(),
                saved.getIdempotencyKey(),
                eventItems,
                Instant.now()
        );
        orderProducer.publishOrderCreated(event);

        return new OrderCreationResult(toResponse(saved), true);
    }

    @Transactional
    public OrderResponse updateOrder(String orderId, UpdateOrderRequest request) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new OrderStateException("O pedido já está em preparo ou finalizado e não pode ser editado.");
        }

        order.setDeliveryAddress(request.deliveryAddress());
        order.getItems().clear();

        List<OrderItem> newItems = request.items().stream().map(itemRequest -> {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemRequest.productId());
            item.setProductName(itemRequest.productName());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            return item;
        }).toList();

        order.getItems().addAll(newItems);

        BigDecimal totalValue = newItems.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalValue(totalValue);
        order.setUpdatedAt(LocalDateTime.now());

        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public void cancelOrder(String orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new OrderStateException("O pedido já está em preparo ou finalizado e não pode ser cancelado.");
        }

        order.setStatus(OrderStatus.FAILURE);
        order.setFailureReason("Cancelado pelo cliente");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        logger.info("Pedido {} cancelado pelo cliente.", orderId);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return toResponse(findOrderOrThrow(orderId));
    }

    @Transactional
    public void updateOrderStatus(String orderId, OrderStatus newStatus, String failureReason) {
        Order order = findOrderOrThrow(orderId);
        order.setStatus(newStatus);
        if (failureReason != null) {
            order.setFailureReason(failureReason);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        logger.info("Status do pedido {} atualizado para {}.", orderId, newStatus);
    }

    private Order findOrderOrThrow(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                )).toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getTotalValue(),
                order.getDeliveryAddress(),
                order.getStatus().name(),
                order.getFailureReason(),
                order.getIdempotencyKey(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemResponses
        );
    }

    public record OrderCreationResult(OrderResponse response, boolean isNew) {}
}
