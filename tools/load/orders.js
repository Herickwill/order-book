// Teste de carga do POST /orders, com k6.
//
// Uso, com o docker compose de pe (a rede do compose e "order-book_default"):
//
//   docker run --rm -i --network order-book_default -e BASE_URL=http://api:8080 \
//     grafana/k6 run - < tools/load/orders.js
//
// Variaveis (-e NOME=valor): BASE_URL, RATE (req/s, padrao 5000),
// DURATION (padrao 60s), USERS (padrao 200), VUS e MAX_VUS.
//
// Fases:
//   setup     deposita BRL e VIB para USERS usuarios e espera o Worker zerar a fila
//   carga     RATE ordens LIMIT por segundo, durante DURATION, a taxa constante:
//             se a API nao acompanhar, o k6 conta dropped_iterations em vez de
//             reduzir a carga escondido
//   teardown  mede a fila no fim da carga e quanto o Worker leva para drena-la
//
// So POST /orders: e o caminho mais caro (cada requisicao e um commit no
// Postgres) e o unico que alimenta o Worker.

import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RATE = Number(__ENV.RATE || 5000);
const DURATION = __ENV.DURATION || '60s';
const USERS = Number(__ENV.USERS || 200);

const DEPOSIT_BRL = '1000000.00';
const DEPOSIT_VIB = '100000';
const MIN_PRICE_CENTS = 950;
const PRICE_RANGE_CENTS = 101;
const MAX_QUANTITY = 10;
const SETUP_BATCH_SIZE = 100;
const WORKER_WAIT_SECONDS = 60;
const DRAIN_WAIT_SECONDS = 300;
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

const expectedIterations = RATE * durationInSeconds(DURATION);
const maxDroppedIterations = Math.ceil(expectedIterations * 0.01);

export const options = {
    scenarios: {
        orders: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: Number(__ENV.VUS || 300),
            maxVUs: Number(__ENV.MAX_VUS || 2000),
        },
    },
    thresholds: {
        'http_req_failed{name:create-order}': ['rate<0.01'],
        'http_req_duration{name:create-order}': ['p(99)<200'],
        checks: ['rate>0.99'],
        dropped_iterations: [`count<${maxDroppedIterations}`],
    },
    setupTimeout: `${WORKER_WAIT_SECONDS + 60}s`,
    teardownTimeout: `${DRAIN_WAIT_SECONDS + 30}s`,
    summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
    // Prefixo unico por execucao: o teste roda varias vezes sem limpar o banco.
    const runId = `${Date.now()}`;

    const deposits = [];
    for (let user = 0; user < USERS; user++) {
        deposits.push(depositRequest(runId, user, 'BRL', DEPOSIT_BRL));
        deposits.push(depositRequest(runId, user, 'VIB', DEPOSIT_VIB));
    }

    for (let start = 0; start < deposits.length; start += SETUP_BATCH_SIZE) {
        const responses = http.batch(deposits.slice(start, start + SETUP_BATCH_SIZE));
        const accepted = responses.every((response) => response.status === 202 || response.status === 200);
        if (!accepted) {
            fail(`Deposito recusado no setup: status=${responses.find((r) => r.status >= 300).status}`);
        }
    }

    const waited = waitForEmptyInbox(WORKER_WAIT_SECONDS);
    if (waited < 0) {
        fail(`O Worker nao processou os depositos em ${WORKER_WAIT_SECONDS}s. Ele esta de pe?`);
    }
    console.log(`Setup: ${deposits.length} depositos processados em ${waited.toFixed(1)}s. runId=${runId}`);

    return { runId };
}

export default function (data) {
    const user = userId(data.runId, Math.floor(Math.random() * USERS));
    const side = Math.random() < 0.5 ? 'BUY' : 'SELL';
    const priceCents = MIN_PRICE_CENTS + Math.floor(Math.random() * PRICE_RANGE_CENTS);
    const price = (priceCents / 100).toFixed(2);
    const quantity = 1 + Math.floor(Math.random() * MAX_QUANTITY);
    const clientOrderId = `${data.runId}-${exec.scenario.iterationInTest}`;

    const body = `{"userId":"${user}","clientOrderId":"${clientOrderId}","side":"${side}",`
        + `"type":"LIMIT","price":${price},"quantity":${quantity}}`;

    const response = http.post(`${BASE_URL}/orders`, body, {
        headers: JSON_HEADERS.headers,
        tags: { name: 'create-order' },
    });

    check(response, { 'ordem aceita (202)': (r) => r.status === 202 });
}

export function teardown() {
    const pendingAtEnd = gauge('orderbook_inbox_pending');
    const oldestAtEnd = gauge('orderbook_inbox_oldest_pending_seconds');
    console.log(`Fim da carga: comandos na fila=${pendingAtEnd}, mais antigo ha ${oldestAtEnd.toFixed(1)}s`);

    const drained = waitForEmptyInbox(DRAIN_WAIT_SECONDS);
    const trades = gauge('orderbook_trades_last_id');
    if (drained < 0) {
        console.log(`O Worker NAO drenou a fila em ${DRAIN_WAIT_SECONDS}s. trades ate agora=${trades}`);
        return;
    }
    console.log(`Worker drenou a fila ${drained.toFixed(1)}s depois do fim da carga. trades no banco=${trades}`);
}

function depositRequest(runId, user, asset, amount) {
    const body = `{"userId":"${userId(runId, user)}","depositId":"${asset}-${runId}-${user}",`
        + `"asset":"${asset}","amount":${amount}}`;
    return ['POST', `${BASE_URL}/deposits`, body, JSON_HEADERS];
}

function userId(runId, user) {
    return `load-${runId}-${user}`;
}

/** Espera orderbook_inbox_pending chegar a zero. Devolve os segundos esperados, ou -1 no timeout. */
function waitForEmptyInbox(timeoutSeconds) {
    const startedAt = Date.now();
    while ((Date.now() - startedAt) / 1000 < timeoutSeconds) {
        const pending = gauge('orderbook_inbox_pending');
        if (pending === 0) {
            return (Date.now() - startedAt) / 1000;
        }
        sleep(0.5);
    }
    return -1;
}

/** Le uma gauge do motor em /actuator/prometheus. */
function gauge(name) {
    const response = http.get(`${BASE_URL}/actuator/prometheus`, { tags: { name: 'metrics' } });
    const match = response.body.match(new RegExp(`^${name} (\\S+)$`, 'm'));
    if (!match) {
        fail(`Metrica ${name} ausente em /actuator/prometheus`);
    }
    return Number(match[1]);
}

function durationInSeconds(duration) {
    const match = /^(\d+)(s|m)$/.exec(duration);
    if (!match) {
        throw new Error(`DURATION invalida: ${duration}. Use, por exemplo, 60s ou 2m.`);
    }
    const value = Number(match[1]);
    return match[2] === 'm' ? value * 60 : value;
}
