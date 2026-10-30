package com.meli.orderbook.worker.settlement;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;
import com.meli.orderbook.worker.matching.Order;
import com.meli.orderbook.worker.matching.Trade;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Tudo que mexe em saldo: reserva, devolução, liquidação de trade e depósito.
 *
 * <p>Fica separado do matching em código, não em processo: o mesmo loop chama
 * os dois, e os efeitos vão na mesma transação. O matching decide o que
 * executa; esta classe decide para onde o dinheiro vai.
 *
 * <p><b>Lastro.</b> Enquanto uma ordem {@code LIMIT} está aberta, o saldo
 * reservado para ela é sempre:
 * <ul>
 *   <li>compra: {@code preço limite * restante} em BRL;</li>
 *   <li>venda: {@code restante} em VIB.</li>
 * </ul>
 * As três operações que tocam a reserva mantêm essa conta:
 * {@link #reserveFor} na entrada, {@link #settle} a cada trade e
 * {@link #releaseRemaining} no cancelamento.
 */
public final class Settlement {

    private final Wallets wallets;

    /**
     * Cria a liquidação sobre as carteiras em memória.
     *
     * @param wallets as carteiras em memória
     */
    public Settlement(Wallets wallets) {
        this.wallets = wallets;
    }

    /**
     * Reserva o lastro da ordem inteira: compra reserva
     * {@code preço * quantidade} de BRL; venda reserva {@code quantidade} de VIB.
     *
     * <p>Não serve para a compra {@code MARKET}, que não tem preço: ela usa
     * {@link #reserveAllAvailable}.
     *
     * @param order a ordem recém-chegada
     * @return {@code false} se o saldo não cobre; nesse caso nada muda
     */
    public boolean reserveFor(Order order) {
        var quantity = BigDecimal.valueOf(order.quantity());
        var buying = order.side() == OrderSide.BUY;

        if (buying) {
            var cost = order.price().multiply(quantity);
            return reserve(order.userId(), Asset.BRL, cost);
        }
        return reserve(order.userId(), Asset.VIB, quantity);
    }

    /**
     * Reserva todo o disponível do ativo. Usado pela compra {@code MARKET},
     * que não sabe de antemão quanto vai gastar.
     *
     * @param userId o dono
     * @param asset  o ativo a reservar
     * @return o valor reservado, ou vazio se não havia nada disponível
     */
    public Optional<BigDecimal> reserveAllAvailable(String userId, Asset asset) {
        var found = wallets.find(userId, asset);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        var wallet = found.get();
        var available = wallet.available();
        var nothingAvailable = available.signum() == 0;
        if (nothingAvailable) {
            return Optional.empty();
        }

        wallet.reserve(available);
        wallets.markTouched(wallet);
        return Optional.of(available);
    }

    /**
     * Devolve ao disponível o que ainda está reservado para a ordem: compra
     * devolve {@code preço * restante} de BRL; venda devolve {@code restante}
     * de VIB. É o espelho de {@link #reserveFor}.
     *
     * <p>Usado no cancelamento e na sobra da venda {@code MARKET}. Não serve
     * para a compra {@code MARKET}, cuja sobra é calculada pelo valor gasto.
     *
     * @param order uma ordem aberta com restante positivo
     */
    public void releaseRemaining(Order order) {
        var remaining = BigDecimal.valueOf(order.remaining());
        var buying = order.side() == OrderSide.BUY;

        if (buying) {
            var reservedBrl = order.price().multiply(remaining);
            release(order.userId(), Asset.BRL, reservedBrl);
            return;
        }
        release(order.userId(), Asset.VIB, remaining);
    }

    /**
     * Devolve um valor reservado para o disponível.
     *
     * @param userId o dono
     * @param asset  o ativo
     * @param amount o valor, positivo
     * @throws IllegalStateException se a carteira não existe ou não tem esse valor reservado
     */
    public void release(String userId, Asset asset, BigDecimal amount) {
        var wallet = wallets.require(userId, asset);
        wallet.release(amount);
        wallets.markTouched(wallet);
    }

    /**
     * Liquida um trade de {@code q} unidades ao preço {@code p}:
     *
     * <ul>
     *   <li>comprador: sai {@code p * q} do BRL reservado e entram {@code q} VIB
     *       no disponível;</li>
     *   <li>vendedor: saem {@code q} VIB do reservado e entra {@code p * q} de BRL
     *       no disponível;</li>
     *   <li>se o comprador é o taker de uma compra {@code LIMIT} com limite
     *       {@code L}, a melhora de preço {@code (L - p) * q} volta do reservado
     *       para o disponível.</li>
     * </ul>
     *
     * <p>Assim saem {@code L * q} do reservado do comprador a cada trade, e o
     * que continua reservado é {@code L * restante}. Quando o comprador é o
     * maker, {@code L = p} e não há melhora. A compra {@code MARKET} não tem
     * {@code L}: a sobra volta inteira quando a ordem termina. Num self-trade
     * as duas pontas são as mesmas carteiras, e os passos se somam.
     *
     * @param trade o trade a liquidar
     * @param taker a ordem que chegou e gerou o trade
     */
    public void settle(Trade trade, Order taker) {
        var quantity = BigDecimal.valueOf(trade.quantity());
        var cost = trade.cost();

        var buyerBrl = wallets.require(trade.buyUserId(), Asset.BRL);
        buyerBrl.spendReserved(cost);
        wallets.markTouched(buyerBrl);

        var buyerVib = wallets.getOrCreate(trade.buyUserId(), Asset.VIB);
        buyerVib.credit(quantity);
        wallets.markTouched(buyerVib);

        var sellerVib = wallets.require(trade.sellUserId(), Asset.VIB);
        sellerVib.spendReserved(quantity);
        wallets.markTouched(sellerVib);

        var sellerBrl = wallets.getOrCreate(trade.sellUserId(), Asset.BRL);
        sellerBrl.credit(cost);
        wallets.markTouched(sellerBrl);

        var improvement = priceImprovement(trade, taker);
        var improved = improvement.signum() > 0;
        if (improved) {
            buyerBrl.release(improvement);
        }
    }

    /**
     * Credita um depósito no disponível. Cria só a carteira do ativo
     * depositado.
     *
     * @param userId o dono
     * @param asset  o ativo
     * @param amount o valor, positivo
     * @return {@code false} se o total da carteira passaria do limite da coluna;
     *         nesse caso nada muda
     */
    public boolean deposit(String userId, Asset asset, BigDecimal amount) {
        var existing = wallets.find(userId, asset);
        var fits = existing.map(wallet -> wallet.canCredit(amount)).orElse(true);
        if (!fits) {
            return false;
        }

        var wallet = wallets.getOrCreate(userId, asset);
        wallet.credit(amount);
        wallets.markTouched(wallet);
        return true;
    }

    /**
     * Move o valor do disponível para o reservado, se o disponível cobre.
     * Usuário sem carteira tem saldo zero, e nenhuma carteira é criada.
     */
    private boolean reserve(String userId, Asset asset, BigDecimal amount) {
        var found = wallets.find(userId, asset);
        if (found.isEmpty()) {
            return false;
        }

        var wallet = found.get();
        var covered = wallet.covers(amount);
        if (!covered) {
            return false;
        }

        wallet.reserve(amount);
        wallets.markTouched(wallet);
        return true;
    }

    /**
     * {@code (L - p) * q} quando o comprador é o taker de uma compra
     * {@code LIMIT}; zero nos demais casos.
     */
    private static BigDecimal priceImprovement(Trade trade, Order taker) {
        var takerBuys = taker.side() == OrderSide.BUY;
        var takerIsLimit = taker.type() == OrderType.LIMIT;
        if (!takerBuys || !takerIsLimit) {
            return BigDecimal.ZERO;
        }

        var priceDifference = taker.price().subtract(trade.price());
        var quantity = BigDecimal.valueOf(trade.quantity());
        return priceDifference.multiply(quantity);
    }
}
