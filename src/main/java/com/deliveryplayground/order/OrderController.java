package com.deliveryplayground.order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        Order order = orderService.createOrder(
                request.itemName(), request.quantity(), request.totalPrice());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderResponse(order.getId()));
    }

    record OrderRequest(String itemName, int quantity, int totalPrice) {
    }

    record OrderResponse(Long orderId) {
    }
}
