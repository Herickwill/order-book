package com.meli.orderbook.api.order.web;

import com.meli.orderbook.api.common.CommandHttpStatus;
import com.meli.orderbook.api.order.CancelOrderRequest;
import com.meli.orderbook.api.order.CancelOrderResponse;
import com.meli.orderbook.api.order.CancelOrderUseCase;
import com.meli.orderbook.api.order.CreateOrderRequest;
import com.meli.orderbook.api.order.CreateOrderResponse;
import com.meli.orderbook.api.order.CreateOrderUseCase;
import com.meli.orderbook.api.order.GetOrderRequest;
import com.meli.orderbook.api.order.GetOrderResponse;
import com.meli.orderbook.api.order.GetOrderUseCase;
import com.meli.orderbook.api.order.ListOpenOrdersRequest;
import com.meli.orderbook.api.order.ListOpenOrdersResponse;
import com.meli.orderbook.api.order.ListOpenOrdersUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de {@code /orders}. Cada um chama um único UseCase e só traduz o
 * resultado para HTTP.
 *
 * <p>Os requests de leitura e de cancelamento não têm corpo: o Spring monta o
 * record a partir do path e da query, e as regras de validação ficam no record.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOpenOrdersUseCase listOpenOrdersUseCase;

    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            CancelOrderUseCase cancelOrderUseCase,
            GetOrderUseCase getOrderUseCase,
            ListOpenOrdersUseCase listOpenOrdersUseCase) {

        this.createOrderUseCase = createOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOpenOrdersUseCase = listOpenOrdersUseCase;
    }

    /**
     * {@code POST /orders}: registra uma nova ordem no inbox.
     *
     * @param request a ordem, lida do corpo
     * @return 202 com a ordem nova, ou 200 com a original num reenvio
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        var response = createOrderUseCase.execute(request);
        var httpStatus = CommandHttpStatus.of(response.created());

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * {@code DELETE /orders/{orderId}?userId=}: registra um pedido de cancelamento.
     *
     * @param request a ordem e quem pede o cancelamento
     * @return 202 com o pedido novo, ou 200 com o original num reenvio
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<CancelOrderResponse> cancel(@Valid CancelOrderRequest request) {
        var response = cancelOrderUseCase.execute(request);
        var httpStatus = CommandHttpStatus.of(response.created());

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * {@code GET /orders/{orderId}}: o estado atual de uma ordem.
     *
     * @param request o id da ordem
     * @return 200 com a ordem; 404 se ela não existe
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<GetOrderResponse> getById(@Valid GetOrderRequest request) {
        var response = getOrderUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * {@code GET /orders?side=&userId=&page=&size=}: as ordens abertas de um
     * lado do livro, na ordem em que serão executadas.
     *
     * @param request o lado, o filtro de usuário e a paginação
     * @return 200 com a página de ordens abertas
     */
    @GetMapping
    public ResponseEntity<ListOpenOrdersResponse> listOpen(@Valid ListOpenOrdersRequest request) {
        var response = listOpenOrdersUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
