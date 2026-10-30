package com.meli.orderbook.worker.matching;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.order.RejectReason;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma ordem processada pelo Worker, com o seu estado de execução.
 *
 * <p>É mutável porque a mesma instância fica no livro, é executada por vários
 * trades e tem o estado final gravado no fim do lote. Toda mudança passa por
 * {@link #fill}, {@link #cancel} ou {@link #reject}, que validam a transição.
 * Uma transição inválida (sair de {@code FILLED}, por exemplo) é bug: lança
 * {@link IllegalStateException}, e o Worker encerra o processo.
 *
 * <p>A ordem não conhece saldo: o preço serve só para a prioridade no livro e
 * para decidir se cruza.
 */
public final class Order {

    private final UUID id;
    private final String userId;
    private final long seq;
    private final OrderSide side;
    private final OrderType type;
    private final BigDecimal price;
    private final long quantity;
    private final OffsetDateTime createdAt;

    private long filledQuantity;
    private OrderStatus status;
    private RejectReason rejectReason;

    private Order(
            UUID id,
            String userId,
            long seq,
            OrderSide side,
            OrderType type,
            BigDecimal price,
            long quantity,
            long filledQuantity,
            OrderStatus status,
            OffsetDateTime createdAt) {

        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.seq = seq;
        this.side = Objects.requireNonNull(side, "side");
        this.type = Objects.requireNonNull(type, "type");
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = filledQuantity;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        validateShape();
    }

    /**
     * Cria uma ordem recém-chegada, com estado {@code NEW} e nada executado.
     *
     * @param id        o id gerado pela API
     * @param userId    o dono da ordem
     * @param seq       a posição de chegada atribuída pelo Worker
     * @param side      compra ou venda
     * @param type      {@code LIMIT} ou {@code MARKET}
     * @param price     o preço limite; nulo numa ordem {@code MARKET}
     * @param quantity  a quantidade de VIB, maior que zero
     * @param createdAt o momento em que a API gravou o comando
     * @return a ordem aceita
     * @throws IllegalArgumentException se o formato contradiz o tipo da ordem
     */
    public static Order accept(
            UUID id,
            String userId,
            long seq,
            OrderSide side,
            OrderType type,
            BigDecimal price,
            long quantity,
            OffsetDateTime createdAt) {

        return new Order(id, userId, seq, side, type, price, quantity, 0, OrderStatus.NEW, createdAt);
    }

    /**
     * Recria uma ordem aberta lida do banco quando o Worker sobe.
     *
     * @param id             o id da ordem
     * @param userId         o dono da ordem
     * @param seq            a posição de chegada gravada
     * @param side           compra ou venda
     * @param type           o tipo; só {@code LIMIT} fica aberta
     * @param price          o preço limite
     * @param quantity       a quantidade original
     * @param filledQuantity o quanto já executou
     * @param status         {@code NEW} ou {@code PARTIALLY_FILLED}
     * @param createdAt      o momento do envio
     * @return a ordem restaurada
     * @throws IllegalArgumentException se a ordem não está aberta ou o estado
     *                                  contradiz a quantidade executada
     */
    public static Order restore(
            UUID id,
            String userId,
            long seq,
            OrderSide side,
            OrderType type,
            BigDecimal price,
            long quantity,
            long filledQuantity,
            OrderStatus status,
            OffsetDateTime createdAt) {

        var open = status.isOpen();
        if (!open) {
            throw new IllegalArgumentException(
                    "Só ordens abertas são restauradas no livro. orderId=%s status=%s".formatted(id, status));
        }

        var expectedStatus = statusFor(filledQuantity, quantity);
        if (expectedStatus != status) {
            throw new IllegalArgumentException(
                    "Estado incoerente com a quantidade executada. orderId=%s status=%s filled=%d quantity=%d"
                            .formatted(id, status, filledQuantity, quantity));
        }

        return new Order(id, userId, seq, side, type, price, quantity, filledQuantity, status, createdAt);
    }

    /**
     * Registra a execução de parte da ordem e atualiza o estado para
     * {@code PARTIALLY_FILLED} ou {@code FILLED}.
     *
     * @param executedQuantity a quantidade executada no trade
     * @throws IllegalStateException se a ordem não está aberta ou a quantidade
     *                               não cabe no restante
     */
    public void fill(long executedQuantity) {
        requireOpen("executar");

        var remaining = remaining();
        var fits = executedQuantity > 0 && executedQuantity <= remaining;
        if (!fits) {
            throw new IllegalStateException(
                    "Quantidade executada fora do restante. orderId=%s executed=%d remaining=%d"
                            .formatted(id, executedQuantity, remaining));
        }

        filledQuantity += executedQuantity;
        status = statusFor(filledQuantity, quantity);
    }

    /**
     * Encerra uma ordem aberta sem executar o restante. O que já executou fica
     * em {@code filledQuantity}.
     *
     * @throws IllegalStateException se a ordem já está num estado final
     */
    public void cancel() {
        requireOpen("cancelar");
        status = OrderStatus.CANCELLED;
    }

    /**
     * Recusa uma ordem que não executou nada.
     *
     * @param reason o motivo da recusa
     * @throws IllegalStateException se a ordem não está {@code NEW} ou já executou algo
     */
    public void reject(RejectReason reason) {
        Objects.requireNonNull(reason, "reason");

        var untouched = status == OrderStatus.NEW && filledQuantity == 0;
        if (!untouched) {
            throw new IllegalStateException(
                    "Só uma ordem NEW sem execução pode ser rejeitada. orderId=%s status=%s filled=%d"
                            .formatted(id, status, filledQuantity));
        }

        status = OrderStatus.REJECTED;
        rejectReason = reason;
    }

    /**
     * A quantidade que ainda falta executar.
     *
     * @return {@code quantity - filledQuantity}
     */
    public long remaining() {
        return quantity - filledQuantity;
    }

    /**
     * Se a ordem pode executar mais.
     *
     * @return {@code true} enquanto o estado for {@code NEW} ou {@code PARTIALLY_FILLED}
     */
    public boolean isOpen() {
        return status.isOpen();
    }

    /**
     * Se a ordem aceita executar contra um maker a este preço.
     *
     * <p>A {@code MARKET} aceita qualquer preço. A compra {@code LIMIT} aceita
     * pagar até o seu preço; a venda {@code LIMIT} aceita receber a partir do
     * seu preço.
     *
     * @param makerPrice o preço do nível do livro
     * @return {@code true} se o preço está dentro do limite da ordem
     */
    public boolean acceptsPrice(BigDecimal makerPrice) {
        var market = type == OrderType.MARKET;
        if (market) {
            return true;
        }

        var comparison = makerPrice.compareTo(price);
        var buying = side == OrderSide.BUY;
        if (buying) {
            return comparison <= 0;
        }
        return comparison >= 0;
    }

    /** {@return o id da ordem} */
    public UUID id() {
        return id;
    }

    /** {@return o dono da ordem} */
    public String userId() {
        return userId;
    }

    /** {@return a posição de chegada; desempata a fila de um preço} */
    public long seq() {
        return seq;
    }

    /** {@return compra ou venda} */
    public OrderSide side() {
        return side;
    }

    /** {@return {@code LIMIT} ou {@code MARKET}} */
    public OrderType type() {
        return type;
    }

    /** {@return o preço limite; nulo numa ordem {@code MARKET}} */
    public BigDecimal price() {
        return price;
    }

    /** {@return a quantidade original} */
    public long quantity() {
        return quantity;
    }

    /** {@return a quantidade já executada} */
    public long filledQuantity() {
        return filledQuantity;
    }

    /** {@return o estado atual} */
    public OrderStatus status() {
        return status;
    }

    /** {@return o motivo da rejeição; nulo se a ordem não foi rejeitada} */
    public RejectReason rejectReason() {
        return rejectReason;
    }

    /** {@return o momento em que a API gravou o comando} */
    public OffsetDateTime createdAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Order[id=%s, userId=%s, seq=%d, %s %s, price=%s, quantity=%d, filled=%d, status=%s]"
                .formatted(id, userId, seq, type, side, price, quantity, filledQuantity, status);
    }

    private static OrderStatus statusFor(long filledQuantity, long quantity) {
        if (filledQuantity == 0) {
            return OrderStatus.NEW;
        }
        if (filledQuantity == quantity) {
            return OrderStatus.FILLED;
        }
        return OrderStatus.PARTIALLY_FILLED;
    }

    private void requireOpen(String action) {
        var open = status.isOpen();
        if (!open) {
            throw new IllegalStateException(
                    "Não é possível %s uma ordem em estado final. orderId=%s status=%s"
                            .formatted(action, id, status));
        }
    }

    /**
     * As mesmas regras dos CHECKs de {@code orders}. A API já valida o formato,
     * então chegar aqui com formato inválido é bug.
     */
    private void validateShape() {
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser positiva. orderId=%s".formatted(id));
        }

        var filledInRange = filledQuantity >= 0 && filledQuantity <= quantity;
        if (!filledInRange) {
            throw new IllegalArgumentException(
                    "A quantidade executada está fora do intervalo. orderId=%s".formatted(id));
        }

        var priced = price != null;
        var limit = type == OrderType.LIMIT;
        if (limit && !priced) {
            throw new IllegalArgumentException("Ordem LIMIT sem preço. orderId=%s".formatted(id));
        }
        if (limit && price.signum() <= 0) {
            throw new IllegalArgumentException("Ordem LIMIT com preço não positivo. orderId=%s".formatted(id));
        }
        if (!limit && priced) {
            throw new IllegalArgumentException("Ordem MARKET com preço. orderId=%s".formatted(id));
        }
    }
}
