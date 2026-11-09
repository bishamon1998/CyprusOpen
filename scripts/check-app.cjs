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
    // Railway's health check; API contents are covered by the backend integration tests.
    const health = await fetch(`${baseUrl}/healthz`);
    assert.equal(health.status, 200);
    const report = await (await fetch(`${baseUrl}/api/v1/parking`)).json();
    const date = (value) =>
      new Date(value).toLocaleDateString("en-GB", {
        day: "numeric",
        month: "short",
        year: "numeric",
        timeZone: "Asia/Nicosia",
      });
    const dateTime = (value) =>
      new Date(value).toLocaleString("en-GB", {
        day: "numeric",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        timeZone: "Asia/Nicosia",
      });
    const missing = await fetch(`${baseUrl}/api/unknown`);
    assert.equal(missing.status, 404);
    assert.match(missing.headers.get("content-type"), /json/);

    for (const width of [320, 390, 768, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      for (const [route, heading] of [
        ["/", "Statistics"],
        ["/parking", "Parking"],
        ["/pharmacies", "Pharmacies"],
        ["/beaches", "Beaches"],
        ["/air", "Air quality"],
        ["/nature", "Nature"],
        ["/about", "About"],
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
    // Four picnic sites, six camping sites and three trails; the trails are drawn as routes.
    await page.goto(`${baseUrl}/nature`);
    await page.locator(".result-list li").nth(12).waitFor();
    assert.equal(await page.locator(".result-list li").count(), 13);
    assert.equal(await page.locator("path.map-route").count(), 3);
    // Every tool shows when the source last changed its data and when it was last checked.
    const natureDates = await page.locator(".tool-attribution").innerText();
    assert.ok(natureDates.includes("Data as of 23 Oct 2023"), natureDates);
    assert.ok(natureDates.includes("Last checked ") && !natureDates.includes("Last checked —"), natureDates);
    await page.getByRole("combobox", { name: "Kind of place" }).selectOption("trail");
    assert.equal(await page.locator(".result-list li").count(), 3);
    await page.getByRole("list", { name: "Picnic sites, camping sites and trails" })
      .getByRole("button", { name: /Pipis/ }).click();
    await page.getByRole("link", { name: /Directions to the trail/ }).waitFor();
    await page.getByRole("searchbox", { name: "Search picnic sites, camping, trails" }).fill("Σπηλιές");
    assert.equal(await page.locator(".result-list li").count(), 1);
    // A mistyped address shows the app's own not-found page with links to every section.
    const mistyped = await page.goto(`${baseUrl}/parkng`);
    assert.equal(mistyped.status(), 404);
    await page.getByRole("heading", { name: "Page not found", level: 1 }).waitFor();
    await page.locator(".not-found").getByRole("link", { name: "Parking" }).click();
    await page.getByRole("heading", { name: "Parking", exact: true, level: 1 }).waitFor();
    await page.goto(`${baseUrl}/about`);
    await page.getByRole("heading", { name: "About Cyprus Open", level: 2 }).waitFor();
    await page.getByRole("link", { name: /Source code on GitHub/ }).waitFor();
    await page.goto(`${baseUrl}/pharmacies`);
    await page.locator(".result-list li").nth(4).waitFor();
    const pharmacyDates = await page.locator(".tool-attribution").innerText();
    assert.ok(pharmacyDates.includes("Data as of 3 Oct 2026"), pharmacyDates);
    assert.equal(await page.locator(".result-list li").count(), 5);

    await page.goto(baseUrl);
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
        `Data as of ${date(report.directory.publishedAt)}`,
      ),
    );
    assert.ok(
      attribution.includes(
        `Last checked ${dateTime(report.directory.checkedAt)}`,
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
    // The town comes first, then the distance.
    assert.match(firstDistance, /^Larnaca · [\d.]+ km · straight-line/);
    assert.ok(parseFloat(firstDistance.split(" · ")[1]) < 2);
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
      for (const route of ["/", "/pharmacies", "/beaches", "/air", "/nature", "/about"]) {
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
      "PASS: responsive layouts in EN/RU/EL, statistics charts, parking search/selection/directions, location/denial, tile and API failure fallbacks, beaches, air quality, nature, pharmacies. No external network or browser errors.",
    );
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
