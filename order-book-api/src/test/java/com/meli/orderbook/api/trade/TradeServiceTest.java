package com.meli.orderbook.api.trade;

import com.meli.orderbook.api.trade.repository.TradeRepository;
import com.meli.orderbook.api.trade.repository.UserTradeRow;
import com.meli.orderbook.shared.order.OrderSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TradeService tradeService;

    @Test
    void should_ask_for_one_extra_row_from_the_right_offset() {
        when(tradeRepository.findByUser("alice", 21, 40L)).thenReturn(List.of(trade(3), trade(2)));

        var page = tradeService.findPage("alice", 2, 20);

        assertThat(page.items()).extracting(UserTradeRow::tradeId).containsExactly(3L, 2L);
        assertThat(page.hasNext()).isFalse();
    }

    private static UserTradeRow trade(long id) {
        return new UserTradeRow(id, UUID.randomUUID(), OrderSide.BUY, BigDecimal.TEN, 1L, OffsetDateTime.now());
    }
}
