package com.meli.orderbook.api.order;

import com.meli.orderbook.api.common.Page;
import com.meli.orderbook.api.order.repository.OrderRepository;
import com.meli.orderbook.api.order.repository.OrderRow;
import com.meli.orderbook.shared.order.OrderSide;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** Leitura das ordens já processadas pelo Worker. Quem escreve em {@code orders} é só o Worker. */
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Uma ordem já processada.
     *
     * @param orderId o id da ordem
     * @return a ordem, ou vazio se o Worker ainda não a gravou
     */
    public Optional<OrderRow> findById(UUID orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * Uma página das ordens abertas de um lado do livro, na prioridade de execução.
     *
     * <p>A paginação é por {@code OFFSET}: como o livro muda entre uma página e
     * outra, uma ordem pode aparecer em duas páginas ou em nenhuma. Aceito no
     * MVP; a evolução é paginar por chave ({@code price, seq}).
     *
     * @param side   o lado do livro
     * @param userId filtra por usuário; {@code null} traz todos
     * @param page   a página, começando em zero
     * @param size   o tamanho da página
     * @return a página de ordens abertas
     */
    public Page<OrderRow> findOpenPage(OrderSide side, String userId, int page, int size) {
        var offset = Page.offsetOf(page, size);
        var limit = Page.limitWithLookahead(size);

        var openOrders = orderRepository.findOpen(side, userId, limit, offset);

        return Page.fromLookahead(openOrders, page, size);
    }
}
