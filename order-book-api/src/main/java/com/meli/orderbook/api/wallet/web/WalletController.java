package com.meli.orderbook.api.wallet.web;

import com.meli.orderbook.api.common.CommandHttpStatus;
import com.meli.orderbook.api.wallet.CreateDepositRequest;
import com.meli.orderbook.api.wallet.CreateDepositResponse;
import com.meli.orderbook.api.wallet.CreateDepositUseCase;
import com.meli.orderbook.api.wallet.GetStatementRequest;
import com.meli.orderbook.api.wallet.GetStatementResponse;
import com.meli.orderbook.api.wallet.GetStatementUseCase;
import com.meli.orderbook.api.wallet.GetWalletRequest;
import com.meli.orderbook.api.wallet.GetWalletResponse;
import com.meli.orderbook.api.wallet.GetWalletUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de carteira: depósito, saldo e extrato. Cada um chama um único
 * UseCase e só traduz o resultado para HTTP.
 *
 * <p>O depósito fica em {@code POST /deposits}, com o {@code userId} no corpo,
 * como em {@code POST /orders}: todo comando leva o seu dono no corpo.
 */
@RestController
public class WalletController {

    private final CreateDepositUseCase createDepositUseCase;
    private final GetWalletUseCase getWalletUseCase;
    private final GetStatementUseCase getStatementUseCase;

    public WalletController(
            CreateDepositUseCase createDepositUseCase,
            GetWalletUseCase getWalletUseCase,
            GetStatementUseCase getStatementUseCase) {

        this.createDepositUseCase = createDepositUseCase;
        this.getWalletUseCase = getWalletUseCase;
        this.getStatementUseCase = getStatementUseCase;
    }

    /**
     * {@code POST /deposits}: registra um depósito no inbox. A carteira nasce
     * no primeiro depósito processado pelo Worker.
     *
     * @param request o depósito, lido do corpo
     * @return 202 com o depósito novo, ou 200 com o original num reenvio
     */
    @PostMapping("/deposits")
    public ResponseEntity<CreateDepositResponse> deposit(@Valid @RequestBody CreateDepositRequest request) {
        var response = createDepositUseCase.execute(request);
        var httpStatus = CommandHttpStatus.of(response.created());

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * {@code GET /wallets/{userId}}: os saldos do usuário em todos os ativos.
     *
     * @param request o dono da carteira
     * @return 200 com os saldos; zerados se o usuário não tem depósito processado
     */
    @GetMapping("/wallets/{userId}")
    public ResponseEntity<GetWalletResponse> getByUserId(@Valid GetWalletRequest request) {
        var response = getWalletUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    /**
     * {@code GET /wallets/{userId}/statement?page=&size=}: depósitos
     * processados e trades do usuário, com a variação de cada ativo.
     *
     * @param request o dono da carteira e a paginação
     * @return 200 com a página do extrato; vazia se não há histórico
     */
    @GetMapping("/wallets/{userId}/statement")
    public ResponseEntity<GetStatementResponse> getStatement(@Valid GetStatementRequest request) {
        var response = getStatementUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
