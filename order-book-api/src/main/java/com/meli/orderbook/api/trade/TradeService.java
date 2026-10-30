package com.meli.orderbook.api.trade;

import com.meli.orderbook.api.common.Page;
import com.meli.orderbook.api.trade.repository.TradeRepository;
import com.meli.orderbook.api.trade.repository.UserTradeRow;
import org.springframework.stereotype.Service;

/** Leitura do histórico de trades. Quem escreve em {@code trades} é só o Worker. */
@Service
public class TradeService {

    private final TradeRepository tradeRepository;

    public TradeService(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    /**
     * Uma página dos trades do usuário, do mais recente para o mais antigo.
     *
     * @param userId o usuário
     * @param page   a página, começando em zero
     * @param size   o tamanho da página
     * @return a página de trades
     */
    public Page<UserTradeRow> findPage(String userId, int page, int size) {
        var offset = Page.offsetOf(page, size);
        var limit = Page.limitWithLookahead(size);

        var trades = tradeRepository.findByUser(userId, limit, offset);

        return Page.fromLookahead(trades, page, size);
    }
}
