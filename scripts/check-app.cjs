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
    // One dataset per CYSTAT fixture table served by the CI fixture server.
    const fixtureTables = require("node:fs")
      .readFileSync(
        require("node:path").join(__dirname, "../backend/statistics-service/src/test/resources/cystat/tables.properties"),
        "utf8",
      )
      .split(/\r?\n/)
      .filter((line) => line && !line.startsWith("#")).length;
    assert.equal(datasets.length, fixtureTables);
    for (const code of ["immigrant-groups", "tourist-arrivals", "inflation", "fuel-sales", "birth-dates", "house-prices"]) {
      assert.ok(datasets.some((dataset) => dataset.code === code), code);
    }
    assert.ok(datasets.every((dataset) => dataset.updatedAt !== null));
    const tourism = await (
      await fetch(`${baseUrl}/api/v1/statistics/datasets/tourist-arrivals`)
    ).json();
    assert.ok(tourism.series.some((series) => series.key === "GB"));
    const report = await (await fetch(`${baseUrl}/api/v1/parking`)).json();
    assert.equal(report.freshness, "unavailable");
    assert.ok(report.items.length > 0);
    assert.ok(report.items.every((item) => item.availableSpaces === null));
    const air = await (await fetch(`${baseUrl}/api/v1/air-quality`)).json();
    assert.equal(air.stations.length, 3, "air quality stations (the misplaced one is dropped)");
    assert.equal(air.stations.find((station) => station.id === "station_1").level, "moderate");
    assert.equal(air.source.license, "CC BY-SA 4.0");
    const beaches = await (await fetch(`${baseUrl}/api/v1/beaches`)).json();
    assert.equal(beaches.beaches.length, 3, "bathing waters (the one no longer designated is dropped)");
    assert.equal(beaches.season, 2024);
    assert.ok(beaches.beaches.every((beach) => beach.quality === "excellent"));
    const pharmacies = await (await fetch(`${baseUrl}/api/v1/pharmacies`)).json();
    assert.equal(pharmacies.items.length, 5);
    assert.ok(pharmacies.checkedAt);
    assert.ok(pharmacies.items.every((item) => item.id && "location" in item));
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
        ["/beaches", "Beaches"],
        ["/air", "Air quality"],
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
                !element.closest(".map-surface") &&
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
    await page.goto(`${baseUrl}/beaches`);
    await page.locator(".result-list li").nth(2).waitFor();
    await page.getByRole("searchbox", { name: "Search beaches" }).fill("karafa");
    assert.equal(await page.locator(".result-list li").count(), 1);
    await page.locator(".result-row").first().click();
    await page.locator(".beach-tests").waitFor();
    await page.goto(`${baseUrl}/air`);
    await page.locator(".result-list li").nth(2).waitFor();
    assert.equal(await page.locator(".result-list li").count(), 3);
    await page.locator(".result-row").first().click();
    await page.getByRole("table").getByText("NO₂").waitFor();
    await page.goto(`${baseUrl}/pharmacies`);
    await page.locator(".result-list li").nth(4).waitFor();
    assert.equal(await page.locator(".result-list li").count(), 5);

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
    await page
      .getByRole("heading", {
        name: `${report.items.length} car parks`,
        exact: true,
      })
      .waitFor();
    const attribution = (
      await page.locator(".tool-attribution").textContent()
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
              .querySelector(".tool-results-heading h2")
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
    assert.equal(await page.locator(".map-pin.is-selected").count(), 1);
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
      .locator(".result-row small")
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
      for (const route of ["/", "/pharmacies", "/beaches", "/air", "/about"]) {
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
    for (const chart of [
      "Immigration · Citizenship",
      "Tourist arrivals · Tourist arrivals",
      "Inflation · By year",
      "Population · Population",
      "Marriages · Marriages",
      "Crime · By type",
      "Earnings · Median and average",
      "Fuel sales · Road fuels",
      "Cyprus calendar · Birthdays",
      "Seasons of life · Births",
      "Causes of death · By cause",
      "Family today · Wedding age",
      "Who marries whom · Nationalities",
      "Divorces · Years married",
      "Where every 100 € goes · By category",
      "Household income · By district",
      "House prices · Index",
      "New homes approved · Per month",
      "Cars by fuel · New registrations",
      "What tourists spend · Per day",
      "Trips of residents · By year",
    ]) {
      await page.getByRole("img", { name: chart, exact: true }).waitFor();
    }
    await page.getByRole("radio", { name: "Weddings" }).click();
    await page.getByRole("img", { name: "Cyprus calendar · Weddings" }).waitFor();
    // A folded category hides its charts and stays folded after a reload.
    await page.getByRole("button", { name: /^Safety/ }).click();
    await page.getByRole("img", { name: "Crime · By type" }).waitFor({ state: "detached" });
    await page.reload();
    await page.getByRole("img", { name: "Cyprus calendar · Birthdays" }).waitFor();
    assert.equal(await page.getByRole("img", { name: "Crime · By type" }).count(), 0, "Safety stays folded");
    await page.getByRole("button", { name: /^Safety/ }).click();
    await page.getByRole("img", { name: "Crime · By type" }).waitFor();
    await page.getByRole("radio", { name: "Since 1981" }).click();
    await page.getByRole("img", { name: "Immigration · Since 1981" }).waitFor();
    await page.getByRole("radio", { name: "Winter" }).click();
    await page
      .getByRole("img", { name: "Tourist arrivals · Tourist arrivals" })
      .waitFor();
    await page.getByRole("radio", { name: "By category" }).click();
    await page.getByRole("img", { name: "Inflation · By category" }).waitFor();
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
