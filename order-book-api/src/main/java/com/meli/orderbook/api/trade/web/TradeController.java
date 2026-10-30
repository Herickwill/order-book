package com.meli.orderbook.api.trade.web;

import com.meli.orderbook.api.trade.ListTradesRequest;
import com.meli.orderbook.api.trade.ListTradesResponse;
import com.meli.orderbook.api.trade.ListTradesUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint do histórico de trades. Chama um único UseCase e só traduz o resultado para HTTP. */
@RestController
@RequestMapping("/trades")
public class TradeController {

    private final ListTradesUseCase listTradesUseCase;

    public TradeController(ListTradesUseCase listTradesUseCase) {
        this.listTradesUseCase = listTradesUseCase;
    }

    /**
     * {@code GET /trades?userId=&page=&size=}: o histórico do usuário.
     *
     * @param request o usuário e a paginação, da query
     * @return 200 com a página de trades
     */
    @GetMapping
    public ResponseEntity<ListTradesResponse> list(@Valid ListTradesRequest request) {
        var response = listTradesUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
