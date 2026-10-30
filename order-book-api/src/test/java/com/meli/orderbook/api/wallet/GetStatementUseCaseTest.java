package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.common.Page;
import com.meli.orderbook.api.wallet.repository.StatementEntryRow;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.wallet.Asset;
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
class GetStatementUseCaseTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private GetStatementUseCase getStatementUseCase;

    @Test
    void should_convert_each_entry_and_keep_the_order_and_the_paging() {
        var at = OffsetDateTime.parse("2026-10-29T12:00:00Z");
        var trade = new StatementEntryRow(2L, at, OrderSide.BUY, UUID.randomUUID(), new BigDecimal("10.00"), 3L,
                null, null, null);
        var deposit = new StatementEntryRow(1L, at.minusMinutes(1), null, null, null, null, "deposit:d1",
                Asset.BRL, new BigDecimal("100.00"));
        var rows = List.of(trade, deposit, trade);
        when(walletService.findStatementPage("alice", 0, 2)).thenReturn(Page.fromLookahead(rows, 0, 2));

        var response = getStatementUseCase.execute(new GetStatementRequest("alice", 0, 2));

        assertThat(response.userId()).isEqualTo("alice");
        assertThat(response.items()).extracting(StatementEntryItem::type)
                .containsExactly(StatementEntryType.BUY, StatementEntryType.DEPOSIT);
        assertThat(response.hasNext()).isTrue();
    }
}
