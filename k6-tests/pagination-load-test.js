/**
 * Load Testing Script for Pagination
 * Uses k6 (Grafana k6) for load testing
 * 
 * File: k6-tests/pagination-load-test.js
 * Run: k6 run k6-tests/pagination-load-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const pageLoadTrend = new Trend('page_load_time');
const requestCounter = new Counter('requests_total');
const activeUsers = new Gauge('active_users');

// Test configuration
export const options = {
  stages: [
    { duration: '1m', target: 10 },    // Ramp up to 10 users
    { duration: '3m', target: 50 },    // Ramp up to 50 users
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '3m', target: 100 },   // Stay at 100 users
    { duration: '2m', target: 0 },     // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(99)<500'], // 99% of requests must complete below 500ms
    'errors': ['rate<0.1'],              // Error rate must be below 10%
  },
};

const API_BASE_URL = __ENV.API_URL || 'http://localhost:8080/api';

export default function () {
  activeUsers.add(1);

  // Test 1: Product Pagination
  group('Product Pagination', () => {
    for (let page = 0; page < 10; page++) {
      const url = `${API_BASE_URL}/products/paginated?page=${page}&size=10&sort=id`;
      
      const startTime = new Date();
      const response = http.get(url);
      const duration = new Date() - startTime;
      
      pageLoadTrend.add(duration);
      requestCounter.add(1);

      const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'has data': (r) => r.json('data') !== null,
        'has pagination': (r) => r.json('pagination') !== null,
        'page matches': (r) => r.json('pagination.page') === page,
        'response time < 500ms': (r) => r.timings.duration < 500,
      });

      if (!success) {
        errorRate.add(1);
        console.error(`Product pagination failed: page=${page}, status=${response.status}`);
      }

      sleep(0.5); // 500ms delay between requests
    }
  });

  // Test 2: Product Search Pagination
  group('Product Search Pagination', () => {
    const keywords = ['shirt', 'pants', 'shoes', 'hat', 'dress'];
    
    keywords.forEach((keyword) => {
      for (let page = 0; page < 5; page++) {
        const url = `${API_BASE_URL}/products/search?keyword=${keyword}&page=${page}&size=10`;
        
        const startTime = new Date();
        const response = http.get(url);
        const duration = new Date() - startTime;
        
        pageLoadTrend.add(duration);
        requestCounter.add(1);

        const success = check(response, {
          'status is 200': (r) => r.status === 200,
          'has data': (r) => r.json('data') !== null,
          'has pagination': (r) => r.json('pagination') !== null,
          'response time < 500ms': (r) => r.timings.duration < 500,
        });

        if (!success) {
          errorRate.add(1);
        }

        sleep(0.5);
      }
    });
  });

  // Test 3: Order List Pagination (with auth token)
  group('Order Pagination (ADMIN)', () => {
    const token = __ENV.ADMIN_TOKEN || 'your-admin-jwt-token';
    
    const headers = {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };

    for (let page = 0; page < 10; page++) {
      const url = `${API_BASE_URL}/orders?page=${page}&size=10&sort=createdAt`;
      
      const startTime = new Date();
      const response = http.get(url, { headers });
      const duration = new Date() - startTime;
      
      pageLoadTrend.add(duration);
      requestCounter.add(1);

      const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'has data': (r) => r.json('data') !== null,
        'has pagination': (r) => r.json('pagination') !== null,
        'response time < 1000ms': (r) => r.timings.duration < 1000,
      });

      if (!success) {
        errorRate.add(1);
      }

      sleep(1); // 1s delay for order queries (heavier)
    }
  });

  // Test 4: Concurrent Pagination Requests
  group('Concurrent Pagination', () => {
    const batch = http.batch([
      ['GET', `${API_BASE_URL}/products/paginated?page=0&size=20`],
      ['GET', `${API_BASE_URL}/products/paginated?page=1&size=20`],
      ['GET', `${API_BASE_URL}/products/paginated?page=2&size=20`],
      ['GET', `${API_BASE_URL}/products/paginated?page=3&size=20`],
      ['GET', `${API_BASE_URL}/products/paginated?page=4&size=20`],
    ]);

    batch.forEach((response) => {
      requestCounter.add(1);
      pageLoadTrend.add(response.timings.duration);

      const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
      });

      if (!success) {
        errorRate.add(1);
      }
    });
  });

  activeUsers.add(-1);
  sleep(1);
}

/**
 * Run tests with:
 * k6 run k6-tests/pagination-load-test.js
 * 
 * With custom settings:
 * k6 run -u 50 -d 5m k6-tests/pagination-load-test.js
 * 
 * Export to HTML report:
 * k6 run -u 100 -d 10m --out html=report.html k6-tests/pagination-load-test.js
 * 
 * Monitor in real-time with Grafana:
 * k6 run -u 100 -d 10m --out grafana k6-tests/pagination-load-test.js
 */
