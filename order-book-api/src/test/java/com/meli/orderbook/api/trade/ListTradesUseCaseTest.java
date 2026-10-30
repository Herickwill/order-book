package com.meli.orderbook.api.trade;

import com.meli.orderbook.api.common.Page;
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
class ListTradesUseCaseTest {

    @Mock
    private TradeService tradeService;

    @InjectMocks
    private ListTradesUseCase listTradesUseCase;

    @Test
    void should_expose_the_execution_time_when_converting_a_trade() {
        var executedAt = OffsetDateTime.now();
        var orderId = UUID.randomUUID();
        var row = new UserTradeRow(9L, orderId, OrderSide.SELL, new BigDecimal("10.00"), 4L, executedAt);
        when(tradeService.findPage("bob", 0, 20)).thenReturn(Page.fromLookahead(List.of(row), 0, 20));

        var response = listTradesUseCase.execute(new ListTradesRequest("bob", null, null));

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.tradeId()).isEqualTo(9L);
            assertThat(item.orderId()).isEqualTo(orderId);
            assertThat(item.side()).isEqualTo(OrderSide.SELL);
            assertThat(item.executedAt()).isEqualTo(executedAt);
        });
        assertThat(response.hasNext()).isFalse();
    }
}
