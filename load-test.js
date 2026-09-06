import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics to track exchange performance
const ordersPlaced = new Counter('exchange_orders_placed');
const tradesCreated = new Counter('exchange_trades_created');
const orderLatency = new Trend('exchange_order_latency', true);
const errorRate = new Rate('exchange_error_rate');

// Configurable target URL (defaults to localhost:8080)
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '10s', target: 10 },  // Ramp-up: 10 concurrent traders
    { duration: '30s', target: 30 },  // Sustained load: 30 concurrent traders
    { duration: '10s', target: 0 },   // Ramp-down to 0
  ],
  thresholds: {
    // 99% of requests must succeed
    http_req_failed: ['rate<0.01'],
    // 95% of requests must complete under 100ms
    http_req_duration: ['p(95)<100'],
    // Custom error rate below 1%
    exchange_error_rate: ['rate<0.01'],
  },
};

// Generates randomized, overlapping limit orders to stimulate crossing matches
function getRandomOrder() {
  const sides = ['BUY', 'SELL'];
  const side = sides[Math.floor(Math.random() * sides.length)];

  // Overlapping price band around 50.00 to trigger trades
  // BUY: 48.00 - 52.00, SELL: 49.00 - 53.00
  let price;
  if (side === 'BUY') {
    price = (48.00 + Math.random() * 4.00).toFixed(2);
  } else {
    price = (49.00 + Math.random() * 4.00).toFixed(2);
  }

  const quantity = Math.floor(Math.random() * 20) + 5; // 5 to 25 shares
  const userId = Math.floor(Math.random() * 100) + 1;   // 100 distinct users

  return {
    userId: userId,
    side: side,
    price: parseFloat(price),
    quantity: quantity,
    instrument: 'ACME',
  };
}

export function setup() {
  // Pre-flight check: ensure the exchange API is reachable before starting
  const res = http.get(`${BASE_URL}/api/orderbook`);
  if (res.status !== 200) {
    throw new Error(`Exchange server unreachable at ${BASE_URL}. Status: ${res.status}`);
  }
  console.log(`[k6 setup] Connected to Exchange API at ${BASE_URL}. Starting load test...`);
  return { startTime: new Date().toISOString() };
}

export default function () {
  const orderData = getRandomOrder();
  const payload = JSON.stringify(orderData);
  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  // 1. Submit Limit Order
  const start = Date.now();
  const orderRes = http.post(`${BASE_URL}/api/orders`, payload, params);
  const latency = Date.now() - start;

  orderLatency.add(latency);

  const orderSuccess = check(orderRes, {
    'order status is 201': (r) => r.status === 201,
    'valid order ID returned': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.id !== undefined && body.id !== null;
      } catch (_) {
        return false;
      }
    },
    'remaining quantity is non-negative': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.remainingQuantity >= 0 && body.remainingQuantity <= body.originalQuantity;
      } catch (_) {
        return false;
      }
    },
    'order status is valid': (r) => {
      try {
        const body = JSON.parse(r.body);
        return ['OPEN', 'PARTIALLY_FILLED', 'FILLED'].includes(body.status);
      } catch (_) {
        return false;
      }
    },
  });

  if (orderSuccess) {
    ordersPlaced.add(1);
    errorRate.add(0);
  } else {
    errorRate.add(1);
  }

  // 2. Intermittent read traffic: 15% of iterations fetch the OrderBook snapshot
  if (Math.random() < 0.15) {
    const bookRes = http.get(`${BASE_URL}/api/orderbook`);
    check(bookRes, {
      'orderbook status is 200': (r) => r.status === 200,
      'orderbook has valid queues': (r) => {
        try {
          const b = JSON.parse(r.body);
          return Array.isArray(b.buyOrders) && Array.isArray(b.sellOrders);
        } catch (_) {
          return false;
        }
      },
    });
  }

  // Realistic simulation sleep: 20ms to 50ms between order submissions
  sleep(0.02 + Math.random() * 0.03);
}

export function teardown(data) {
  console.log(`\n======================================================`);
  console.log(`[k6 teardown] Load test completed.`);

  // Query final trade history
  const tradesRes = http.get(`${BASE_URL}/api/trades`);
  if (tradesRes.status === 200) {
    const trades = JSON.parse(tradesRes.body);
    console.log(`Total executed trades recorded in DB: ${trades.length}`);
    tradesCreated.add(trades.length);
  }

  // Query final order book depth
  const bookRes = http.get(`${BASE_URL}/api/orderbook`);
  if (bookRes.status === 200) {
    const book = JSON.parse(bookRes.body);
    console.log(`Final resting BUY orders in book:  ${book.buyOrders.length}`);
    console.log(`Final resting SELL orders in book: ${book.sellOrders.length}`);
  }
  console.log(`======================================================\n`);
}
