package com.meli.orderbook.worker.processing;

import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Propriedades sobre sequências aleatórias de comandos, com seed fixa para o
 * resultado ser reproduzível.
 *
 * <p>Os preços variam em centavos numa faixa estreita, para o livro cruzar com
 * frequência; os saldos são pequenos, para a reserva falhar com frequência; e
 * os cancelamentos miram ordens existentes, executadas, de outro dono e
 * inexistentes.
 */
class InvariantPropertyTest {

    private static final long SEED = 20261029L;
    private static final int COMMANDS = 10_000;
    private static final String[] USERS = {"alice", "bob", "carol", "dave", "erin"};

    @Test
    void should_hold_all_five_invariants_after_every_command_when_processing_a_random_sequence() {
        var exchange = TestExchange.empty();
        var checker = new InvariantChecker();
        var random = new Random(SEED);
        var orderIds = new ArrayList<UUID>();

        for (var i = 0; i < COMMANDS; i++) {
            var command = randomCommand(exchange, random, orderIds);
            if (command.type() == CommandType.DEPOSIT) {
                checker.recordDeposit(command.asset(), command.amount());
            }

            var changes = exchange.batch(command);
            checker.recordChanges(changes);

            var violation = checker.firstViolation(exchange.book(), exchange.wallets());
            assertThat(violation).as("comando %d: %s", i, command).isNull();
        }

        assertThat(exchange.trades()).as("a sequência precisa exercitar o matching").hasSizeGreaterThan(1_000);
    }

    @Test
    void should_reach_the_same_state_when_the_same_sequence_is_processed_twice_from_scratch() {
        var commands = generate(new Random(SEED), COMMANDS);

        var oneByOne = TestExchange.empty();
        for (var command : commands) {
            oneByOne.batch(command);
        }

        var inBatches = TestExchange.empty();
        var batchSizes = new Random(SEED + 1);
        var start = 0;
        while (start < commands.size()) {
            var end = Math.min(commands.size(), start + 1 + batchSizes.nextInt(500));
            inBatches.batch(commands.subList(start, end));
            start = end;
        }

        assertThat(snapshot(inBatches)).isEqualTo(snapshot(oneByOne));
    }

    /**
     * Gera a sequência inteira antes de processar, para as duas execuções do
     * teste de determinismo receberem exatamente os mesmos comandos.
     */
    private static List<Command> generate(Random random, int count) {
        var generator = TestExchange.empty();
        var orderIds = new ArrayList<UUID>();
        var commands = new ArrayList<Command>();
        for (var i = 0; i < count; i++) {
            commands.add(randomCommand(generator, random, orderIds));
        }
        return commands;
    }

    private static Command randomCommand(TestExchange exchange, Random random, List<UUID> orderIds) {
        var user = USERS[random.nextInt(USERS.length)];
        var roll = random.nextInt(100);

        if (roll < 12) {
            var cents = 1 + random.nextInt(50_000);
            var amount = BigDecimal.valueOf(cents, 2);
            return exchange.depositCommand(user, Asset.BRL, amount.toPlainString());
        }
        if (roll < 22) {
            var units = 1 + random.nextInt(30);
            return exchange.depositCommand(user, Asset.VIB, Integer.toString(units));
        }
        if (roll < 72) {
            var side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
            var price = BigDecimal.valueOf(950 + random.nextInt(101), 2);
            var quantity = 1 + random.nextInt(15);
            var command = exchange.limitCommand(user, side, price.toPlainString(), quantity);
            orderIds.add(command.orderId());
            return command;
        }
        if (roll < 85) {
            var side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
            var quantity = 1 + random.nextInt(15);
            var command = exchange.marketCommand(user, side, quantity);
            orderIds.add(command.orderId());
            return command;
        }

        var target = randomCancelTarget(random, orderIds);
        return exchange.cancelCommand(user, target);
    }

    private static UUID randomCancelTarget(Random random, List<UUID> orderIds) {
        var unknown = orderIds.isEmpty() || random.nextInt(10) == 0;
        if (unknown) {
            return new UUID(1, random.nextLong());
        }
        return orderIds.get(random.nextInt(orderIds.size()));
    }

    /** Estado comparável: carteiras, ordens e trades, em ordem estável. */
    private static List<String> snapshot(TestExchange exchange) {
        var lines = new ArrayList<String>();

        exchange.wallets().all().stream()
                .sorted((a, b) -> (a.userId() + a.asset()).compareTo(b.userId() + b.asset()))
                .forEach(wallet -> lines.add("wallet %s %s %s %s".formatted(wallet.userId(), wallet.asset(),
                        wallet.available().stripTrailingZeros().toPlainString(),
                        wallet.reserved().stripTrailingZeros().toPlainString())));

        for (var order : exchange.ordersSeen()) {
            lines.add("order %s seq=%d %s filled=%d %s".formatted(order.id(), order.seq(), order.status(),
                    order.filledQuantity(), order.rejectReason()));
        }
        for (var trade : exchange.trades()) {
            lines.add("trade " + trade);
        }
        lines.add("bids " + exchange.bookOrderIds(OrderSide.BUY));
        lines.add("asks " + exchange.bookOrderIds(OrderSide.SELL));
        return lines;
    }
}
