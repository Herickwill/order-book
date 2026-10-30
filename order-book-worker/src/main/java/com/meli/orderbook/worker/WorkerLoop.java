package com.meli.orderbook.worker;

import com.meli.orderbook.worker.persistence.BatchWriter;
import com.meli.orderbook.worker.persistence.CommandReader;
import com.meli.orderbook.worker.persistence.StateLoader;
import com.meli.orderbook.worker.persistence.WorkerLock;
import com.meli.orderbook.worker.processing.CommandProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * O ciclo de vida do Worker: lock, reconstrução do estado e o loop de lotes.
 *
 * <pre>
 * subida:  pega o advisory lock (espera, se outro Worker tem)
 *          reconstrói livro e carteiras do banco
 * loop:    lê até batch-size comandos RECEIVED, em ordem de id
 *          fila vazia: dorme poll-interval e lê de novo
 *          processa em memória e grava tudo numa transação
 * </pre>
 *
 * <p>O loop roda numa thread própria, iniciada quando o contexto do Spring
 * termina de subir. Como {@link SmartLifecycle} da fase mais alta, é o
 * primeiro componente a parar no desligamento, antes do pool de conexões: um
 * SIGTERM deixa o lote corrente terminar o commit, e só então o processo para.
 *
 * <p>Qualquer exceção encerra o processo com código 1. Nada aqui tenta se
 * consertar em memória: a única recuperação é subir de novo e reconstruir do
 * banco ({@code restart: always} no compose).
 */
@Component
public class WorkerLoop implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(WorkerLoop.class);

    private static final int FAILURE_EXIT_CODE = 1;
    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(20);

    private final WorkerLock workerLock;
    private final StateLoader stateLoader;
    private final CommandReader commandReader;
    private final BatchWriter batchWriter;
    private final WorkerProperties properties;
    private final ConfigurableApplicationContext context;

    private volatile boolean running;
    private Thread loopThread;

    public WorkerLoop(
            WorkerLock workerLock,
            StateLoader stateLoader,
            CommandReader commandReader,
            BatchWriter batchWriter,
            WorkerProperties properties,
            ConfigurableApplicationContext context) {

        this.workerLock = workerLock;
        this.stateLoader = stateLoader;
        this.commandReader = commandReader;
        this.batchWriter = batchWriter;
        this.properties = properties;
        this.context = context;
    }

    @Override
    public void start() {
        running = true;
        loopThread = new Thread(this::runSafely, "worker-loop");
        loopThread.start();
    }

    /**
     * Pede ao loop que pare depois do lote corrente e espera ele terminar.
     *
     * <p>Se quem chama é a própria thread do loop (o caminho de falha, em
     * {@link #exitWithFailure()}), não há o que esperar: ela esperaria por si
     * mesma para sempre. Se o Worker ainda está bloqueado esperando o lock, a
     * espera tem limite, porque a chamada ao banco não responde a interrupção;
     * o lock some junto com a conexão quando o processo sai.
     */
    @Override
    public void stop() {
        running = false;

        var calledFromLoopThread = Thread.currentThread() == loopThread;
        if (calledFromLoopThread) {
            return;
        }

        try {
            var finished = loopThread.join(SHUTDOWN_WAIT);
            if (!finished) {
                log.warn("Loop do Worker não terminou em {}; saindo assim mesmo.", SHUTDOWN_WAIT);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void runSafely() {
        try {
            run();
        } catch (Throwable failure) {
            log.error("Falha no Worker; encerrando o processo para reconstruir o estado do banco.", failure);
            exitWithFailure();
        }
    }

    private void run() throws InterruptedException {
        log.info("Aguardando o advisory lock do Worker.");
        workerLock.acquire();
        log.info("Advisory lock adquirido; reconstruindo o estado do banco.");

        var state = stateLoader.load();
        var processor = new CommandProcessor(state.book(), state.wallets(), state.lastSeq());
        log.info("Estado reconstruído. carteiras={} ordensAbertas={} ultimoSeq={}",
                state.wallets().all().size(), state.openOrders(), state.lastSeq());

        while (running) {
            var processed = processNextBatch(processor);
            if (!processed) {
                Thread.sleep(properties.pollInterval());
            }
        }

        log.info("Loop do Worker parado.");
    }

    /**
     * Lê, processa e grava um lote.
     *
     * @return {@code false} se a fila estava vazia
     */
    private boolean processNextBatch(CommandProcessor processor) {
        var commands = commandReader.readPending(properties.batchSize());
        if (commands.isEmpty()) {
            return false;
        }

        var startedAt = System.nanoTime();
        var changes = processor.process(commands);
        var processedAt = System.nanoTime();
        batchWriter.write(changes);
        var committedAt = System.nanoTime();

        var processingMicros = (processedAt - startedAt) / 1_000;
        var commitMicros = (committedAt - processedAt) / 1_000;
        log.info("Lote gravado. comandos={} ordens={} trades={} carteiras={} primeiroId={} ultimoId={} "
                        + "processamentoUs={} commitUs={}",
                commands.size(), changes.orders().size(), changes.trades().size(), changes.wallets().size(),
                commands.getFirst().id(), commands.getLast().id(), processingMicros, commitMicros);
        return true;
    }

    /**
     * Fecha o contexto do Spring e sai com código 1. Roda na thread do loop;
     * o fechamento chama {@link #stop()} nesta mesma thread, que por isso não
     * espera.
     */
    private void exitWithFailure() {
        running = false;

        var exitCode = SpringApplication.exit(context, () -> FAILURE_EXIT_CODE);
        System.exit(exitCode);
    }
}
