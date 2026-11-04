const assert = require("node:assert/strict");
const path = require("node:path");
const fs = require("node:fs/promises");
const { createRequire } = require("node:module");
const frontendRequire = createRequire(
  path.resolve(__dirname, "../frontend/package.json"),
);
const { chromium } = process.env.PLAYWRIGHT_MODULE_PATH
  ? require(process.env.PLAYWRIGHT_MODULE_PATH)
  : frontendRequire("playwright");
const baseUrl = process.env.APP_URL || "http://localhost:3000";

async function main() {
  const browser = await chromium.launch({
    headless: true,
    ...(process.env.PLAYWRIGHT_BROWSER_CHANNEL
      ? { channel: process.env.PLAYWRIGHT_BROWSER_CHANNEL }
      : {}),
  });
  const context = await browser.newContext();
  const errors = [];
  const tileOrigin = new URL(
    (await (await fetch(`${baseUrl}/map-config.json`)).json()).tileUrl,
  ).origin;
  const interceptExternal = (route) => {
    const url = new URL(route.request().url());
    if (url.origin === new URL(baseUrl).origin) return route.continue();
    if (url.origin === tileOrigin)
      return route.fulfill({
        contentType: "image/png",
        body: Buffer.from(
          "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=",
          "base64",
        ),
      });
    errors.push(`Unexpected external request: ${url}`);
    return route.abort();
  };
  await context.route("**/*", interceptExternal);
  const page = await context.newPage();
  page.on("pageerror", (error) => errors.push(error.message));
  try {
    const health = await fetch(`${baseUrl}/healthz`);
    assert.equal(health.status, 200);
    const datasets = await (
      await fetch(`${baseUrl}/api/v1/statistics/datasets`)
    ).json();
    assert.deepEqual(
      datasets.map((dataset) => dataset.code),
      ["immigration", "tourism"],
    );
    const report = await (await fetch(`${baseUrl}/api/v1/parking`)).json();
    assert.equal(report.freshness, "unavailable");
    assert.ok(report.items.length > 0);
    assert.ok(report.items.every((item) => item.availableSpaces === null));
    const date = (value) =>
      new Date(value).toLocaleDateString("en-GB", {
        day: "numeric",
        month: "short",
        year: "numeric",
        timeZone: "Asia/Nicosia",
      });
    const missing = await fetch(`${baseUrl}/api/unknown`);
    assert.equal(missing.status, 404);
    assert.match(missing.headers.get("content-type"), /json/);

    for (const width of [320, 375, 390, 768, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      for (const [route, heading] of [
        ["/", "Statistics"],
        ["/parking", "Parking"],
        ["/pharmacies", "Pharmacies"],
        ["/about", "Sources"],
      ]) {
        const response = await page.goto(`${baseUrl}${route}`);
        assert.equal(response.status(), 200);
        await page
          .getByRole("heading", { name: heading, exact: true, level: 1 })
          .waitFor();
        const overflow = await page.evaluate(() =>
          [...document.querySelectorAll("body *")]
            .filter(
              (element) =>
                !element.closest(".parking-map") &&
                element.getClientRects().length &&
                element.getBoundingClientRect().right > innerWidth + 1,
            )
            .map((element) => `${element.tagName}.${element.className}`),
        );
        assert.equal(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth,
          ),
          false,
          `${route} overflows at ${width}px: ${overflow.join(", ")}`,
        );
        assert.deepEqual(overflow, [], `${route} overflows at ${width}px`);
      }
    }
    await page.goto(baseUrl);
    assert.equal(
      await page
        .locator("main")
        .getByRole("link", { name: /Parking|Pharmacies/i })
        .count(),
      0,
    );
    await page.getByRole("link", { name: "Parking", exact: true }).click();
    await page
      .getByRole("heading", { name: "Parking", exact: true, level: 1 })
      .waitFor();
    assert.equal(new URL(page.url()).pathname, "/parking");
    await page.getByRole("heading", { name: `${report.items.length} car parks`, exact: true }).waitFor();
    const attribution = (
      await page.locator(".parking-attribution").textContent()
    ).replace(/\s+/g, " ");
    assert.ok(
      attribution.includes(
        `Source published ${date(report.directory.publishedAt)}`,
      ),
    );
    assert.ok(
      attribution.includes(
        `Directory checked ${date(report.directory.checkedAt)}`,
      ),
    );
    for (const area of [
      "nicosia",
      "limassol",
      "larnaca",
      "paphos",
      "ayia-napa",
    ]) {
      await page.getByRole("combobox", { name: "Area" }).selectOption(area);
      await page.waitForFunction(
        () =>
          Number(
            document
              .querySelector(".parking-result-heading h2")
              .textContent.split(" ")[0],
          ) > 0,
      );
    }
    await page.getByRole("combobox", { name: "Area" }).selectOption("nicosia");
    await page
      .getByRole("searchbox", { name: "Search car parks" })
      .fill("Faneromenis");
    await page
      .getByRole("list", { name: "Car parks" })
      .getByRole("button", { name: /Faneromenis/ })
      .click();
    const direction = page.getByRole("link", {
      name: /Directions in Google Maps/,
    });
    await direction.waitFor();
    const destination = new URL(await direction.getAttribute("href"));
    assert.equal(destination.hostname, "www.google.com");
    assert.equal(destination.searchParams.get("api"), "1");
    assert.match(destination.searchParams.get("destination"), /^35\.173/);
    await page
      .getByRole("region", { name: "Car park map" })
      .getByRole("button", { name: "Faneromenis (Municipal)", exact: true })
      .click();
    assert.equal(await page.locator(".parking-pin.is-selected").count(), 1);
    await page
      .getByRole("searchbox", { name: "Search car parks" })
      .fill("no matching car park");
    await page.getByText("No matches. Try another name or area.").waitFor();
    await page.getByRole("searchbox", { name: "Search car parks" }).fill("");
    await context.setGeolocation({ latitude: 34.914, longitude: 33.638 });
    await context.grantPermissions(["geolocation"], { origin: baseUrl });
    await page.getByRole("button", { name: "Near me", exact: true }).click();
    await page.getByRole("button", { name: "Clear location" }).waitFor();
    assert.equal(
      await page.getByRole("combobox", { name: "Area" }).inputValue(),
      "cyprus",
    );
    const firstDistance = await page
      .locator(".parking-row small")
      .first()
      .textContent();
    assert.match(firstDistance, /km · straight-line/);
    assert.ok(parseFloat(firstDistance) < 2);
    await page.getByRole("button", { name: "Clear location" }).click();
    const deniedContext = await browser.newContext();
    await deniedContext.route("**/*", interceptExternal);
    const deniedPage = await deniedContext.newPage();
    deniedPage.on("pageerror", (error) => errors.push(error.message));
    const permissions = await deniedContext.newCDPSession(deniedPage);
    const { targetInfo } = await permissions.send("Target.getTargetInfo");
    await permissions.send("Browser.setPermission", {
      permission: { name: "geolocation" },
      setting: "denied",
      origin: new URL(baseUrl).origin,
      browserContextId: targetInfo.browserContextId,
    });
    await deniedPage.goto(`${baseUrl}/parking`);
    assert.equal(
      await deniedPage.evaluate(
        async () =>
          (await navigator.permissions.query({ name: "geolocation" })).state,
      ),
      "denied",
    );
    await deniedPage
      .getByRole("button", { name: "Near me", exact: true })
      .click();
    await deniedPage
      .getByText("Location access declined. Choose an area instead.")
      .waitFor();
    await deniedContext.close();
    await context.route(`${tileOrigin}/**`, (route) => route.abort());
    await page.reload();
    await page
      .getByText("Map tiles unavailable. The list and directions still work.")
      .waitFor();
    await page.getByRole("combobox", { name: "Area" }).selectOption("nicosia");
    await page
      .getByRole("list", { name: "Car parks" })
      .getByRole("button", { name: /Faneromenis/ })
      .click();
    await page
      .getByRole("link", { name: /Directions in Google Maps/ })
      .waitFor();
    for (const [language, heading] of [
      ["ru", "Парковки"],
      ["el", "Στάθμευση"],
    ]) {
      await page.locator("#language").selectOption(language);
      await page
        .getByRole("heading", { name: heading, exact: true, level: 1 })
        .waitFor();
      await page.reload();
      await page
        .getByRole("heading", { name: heading, exact: true, level: 1 })
        .waitFor();
      assert.equal(await page.locator("html").getAttribute("lang"), language);
      for (const width of [320, 390, 768, 1280]) {
        await page.setViewportSize({ width, height: 900 });
        assert.equal(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth,
          ),
          false,
          `${language} overflows at ${width}px`,
        );
      }
      for (const route of ["/", "/pharmacies", "/about"]) {
        await page.goto(`${baseUrl}${route}`);
        await page.locator("main h1").waitFor();
        assert.equal(
          await page.evaluate(
            () => document.documentElement.scrollWidth > innerWidth,
          ),
          false,
          `${language} ${route} overflows`,
        );
      }
      await page.goto(`${baseUrl}/parking`);
    }
    await page.locator("#language").selectOption("en");
    await page.getByRole("link", { name: "Statistics", exact: true }).click();
    assert.equal(await page.locator("footer").count(), 0);
    assert.equal(
      await page.getByText("People & travel", { exact: true }).count(),
      0,
    );
    assert.equal(
      await page.getByText("Not published", { exact: true }).count(),
      0,
    );
    let parkingFailure = "legacy";
    await context.route(`${baseUrl}/api/v1/parking`, (route) =>
      parkingFailure
        ? route.fulfill({
            status: parkingFailure === "legacy" ? 200 : 503,
            contentType: "application/json",
            body:
              parkingFailure === "legacy"
                ? '{"freshness":"unavailable","lastUpdatedAt":null,"items":[]}'
                : '{"title":"Service unavailable"}',
          })
        : route.continue(),
    );
    for (const failure of ["legacy", "unavailable"]) {
      parkingFailure = failure;
      await page.goto(`${baseUrl}/parking`);
      await page
        .getByRole("alert")
        .getByText("Car parks could not be loaded.")
        .waitFor();
      assert.equal(
        await page
          .getByRole("heading", { name: "0 car parks", exact: true })
          .count(),
        0,
      );
    }
    parkingFailure = "";
    await page.getByRole("button", { name: "Try again", exact: true }).click();
    await page
      .getByRole("heading", {
        name: `${report.items.length} car parks`,
        exact: true,
      })
      .waitFor();
    assert.deepEqual(errors, []);

    if (process.env.APP_SCREENSHOT_DIR) {
      await fs.mkdir(process.env.APP_SCREENSHOT_DIR, { recursive: true });
      await page.goto(`${baseUrl}/parking`);
      await page
        .getByRole("heading", {
          name: `${report.items.length} car parks`,
          exact: true,
        })
        .waitFor();
      for (const [name, width] of [
        ["desktop", 1280],
        ["mobile", 390],
      ]) {
        await page.setViewportSize({ width, height: 900 });
        await page.screenshot({
          path: path.join(process.env.APP_SCREENSHOT_DIR, `${name}.png`),
          fullPage: true,
        });
      }
    }
    console.log(
      "PASS: real APIs, directory coverage, search/selection/directions, location/denial, tile failure fallback, 20 responsive layouts. No external network or browser errors.",
    );
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
