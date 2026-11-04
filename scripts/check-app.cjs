const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs/promises');
const { createRequire } = require('node:module');
const frontendRequire = createRequire(path.resolve(__dirname, '../frontend/package.json'));
const { chromium } = process.env.PLAYWRIGHT_MODULE_PATH ? require(process.env.PLAYWRIGHT_MODULE_PATH) : frontendRequire('playwright');
const baseUrl = process.env.APP_URL || 'http://localhost:3000';

async function main() {
  const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_BROWSER_CHANNEL ? { channel: process.env.PLAYWRIGHT_BROWSER_CHANNEL } : {}) });
  const context = await browser.newContext();
  const errors = [];
  const page = await context.newPage();
  page.on('pageerror', (error) => errors.push(error.message));
  context.on('request', (request) => {
    if (!request.url().startsWith(`${baseUrl}/`)) errors.push(`Unexpected external request: ${request.url()}`);
  });
  try {
    const health = await fetch(`${baseUrl}/healthz`);
    assert.equal(health.status, 200);
    const datasets = await (await fetch(`${baseUrl}/api/v1/statistics/datasets`)).json();
    assert.deepEqual(datasets.map((dataset) => dataset.code), ['immigration', 'tourism']);
    const report = await (await fetch(`${baseUrl}/api/v1/parking`)).json();
    assert.equal(report.freshness, 'unavailable');
    assert.equal(report.items.length, 0);
    const missing = await fetch(`${baseUrl}/api/unknown`);
    assert.equal(missing.status, 404);
    assert.match(missing.headers.get('content-type'), /json/);

    for (const width of [320, 375, 390, 768, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      for (const [route, heading] of [['/', 'Statistics'], ['/parking', 'Parking'], ['/pharmacies', 'Pharmacies'], ['/about', 'Sources']]) {
        const response = await page.goto(`${baseUrl}${route}`);
        assert.equal(response.status(), 200);
        await page.getByRole('heading', { name: heading, exact: true, level: 1 }).waitFor();
        const overflow = await page.evaluate(() => [...document.querySelectorAll('body *')]
          .filter((element) => element.getClientRects().length && element.getBoundingClientRect().right > innerWidth + 1)
          .map((element) => `${element.tagName}.${element.className}`));
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false,
          `${route} overflows at ${width}px: ${overflow.join(', ')}`);
        assert.deepEqual(overflow, [], `${route} overflows at ${width}px`);
      }
    }
    await page.goto(baseUrl);
    assert.equal(await page.locator('main').getByRole('link', { name: /Parking|Pharmacies/i }).count(), 0);
    await page.getByRole('link', { name: 'Parking', exact: true }).click();
    await page.getByRole('heading', { name: 'Parking', exact: true, level: 1 }).waitFor();
    assert.equal(new URL(page.url()).pathname, '/parking');
    assert.deepEqual(errors, []);

    if (process.env.APP_SCREENSHOT_DIR) {
      await fs.mkdir(process.env.APP_SCREENSHOT_DIR, { recursive: true });
      await page.goto(baseUrl);
      await page.getByText('No verified data yet', { exact: true }).first().waitFor();
      for (const [name, width] of [['desktop', 1280], ['mobile', 390]]) {
        await page.setViewportSize({ width, height: 900 });
        await page.screenshot({ path: path.join(process.env.APP_SCREENSHOT_DIR, `${name}.png`), fullPage: true });
      }
    }
    console.log('PASS: real APIs, JSON 404, client navigation, 20 responsive layouts, no external requests or browser errors.');
  } finally {
    await browser.close();
  }
}

main().catch((error) => { console.error(error); process.exitCode = 1; });
