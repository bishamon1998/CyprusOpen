const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs/promises');
const { pathToFileURL } = require('node:url');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE_PATH || 'playwright');

async function main() {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const failures = [];
  const context = await browser.newContext({ locale: 'ru-RU' });
  context.on('request', (request) => {
    if (/^https?:/.test(request.url())) failures.push(`Unexpected network request: ${request.url()}`);
  });
  const page = await context.newPage();
  page.on('pageerror', (error) => failures.push(error.message));
  const url = pathToFileURL(path.resolve(__dirname, '../docs/prototype/index.html')).href;
  try {
    await page.goto(url);
    assert.equal(await page.locator('html').getAttribute('lang'), 'en');
    await page.locator('[data-language="ru"]').click();
    await page.reload();
    assert.equal(await page.locator('html').getAttribute('lang'), 'ru');

    let layouts = 0;
    for (const width of [320, 375, 390, 768, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      for (const locale of ['en', 'ru', 'el']) {
        await page.locator(`[data-language="${locale}"]`).click();
        for (const section of ['statistics', 'parking', 'pharmacies', 'about']) {
          await page.locator(`nav a[data-page="${section}"]`).click();
          await page.waitForFunction((id) => !document.getElementById(id).hidden, section);
          const issues = await page.evaluate(() => {
            const visible = [...document.querySelectorAll('body *')].filter((element) => element.getClientRects().length);
            return visible.filter((element) => {
              const box = element.getBoundingClientRect();
              return box.right > innerWidth + 1 || (box.left < -1 && !element.classList.contains('skip'));
            }).map((element) => `${element.tagName}.${element.className}`);
          });
          assert.deepEqual(issues, [], `${section}/${locale}/${width}: horizontal overflow`);
          layouts++;
        }
      }
    }

    await page.locator('[data-language="en"]').click();
    await page.locator('nav a[data-page="statistics"]').click();
    for (const state of ['loading', 'empty', 'error', 'stale', 'draft']) {
      await page.locator('#state').selectOption(state);
      assert.equal(await page.locator('#statistics .data-region').count(), 2);
      assert.equal(await page.locator('#statistics .data-region').filter({ hasText: 'undefined' }).count(), 0);
      if (state === 'draft') assert.equal(await page.locator('.chart-sketch').count(), 2);
      else if (state === 'stale') assert.equal(await page.locator('.data-region .notice').count(), 2);
      else assert.equal(await page.locator(`.state-message.${state}`).count(), 2);
    }
    await page.locator('nav a[data-page="parking"]').click();
    await page.locator('[data-view="map"]').click();
    assert.equal(await page.locator('#parking-map').isVisible(), true);
    assert.equal(await page.locator('#parking-list').isVisible(), false);
    await page.locator('[data-view="list"]').click();
    assert.equal(await page.locator('#parking-list').isVisible(), true);
    await page.locator('#state').selectOption('empty');
    assert.equal(await page.locator('#parking-list').isVisible(), false);
    assert.deepEqual(failures, []);

    if (process.env.PROTOTYPE_SCREENSHOT_DIR) {
      await fs.mkdir(process.env.PROTOTYPE_SCREENSHOT_DIR, { recursive: true });
      await page.locator('#state').selectOption('draft');
      await page.locator('nav a[data-page="statistics"]').click();
      for (const [name, width] of [['desktop', 1280], ['mobile', 390]]) {
        await page.setViewportSize({ width, height: 900 });
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.screenshot({ path: path.join(process.env.PROTOTYPE_SCREENSHOT_DIR, `${name}.png`), fullPage: true });
      }
    }
    console.log(`PASS: ${layouts} layouts; language default/persistence; 5 data states; map/list; no network requests or runtime errors.`);
  } finally {
    await browser.close();
  }
}

main().catch((error) => { console.error(error); process.exitCode = 1; });
