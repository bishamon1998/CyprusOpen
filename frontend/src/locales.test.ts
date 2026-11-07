import { describe, expect, it } from "vitest";
import { languages, messages, type MessageKey } from "./locales";

const placeholders = (text: string) => [...text.matchAll(/\{(\w+)\}/g)].map((match) => match[1]).sort();

describe("translations", () => {
  it("use the same {placeholders} as English, so no value is lost or shown raw", () => {
    const mismatches = languages.flatMap((language) =>
      (Object.keys(messages.en) as MessageKey[]).flatMap((key) =>
        placeholders(messages[language][key]).join() === placeholders(messages.en[key]).join()
          ? []
          : [`${language}.${key}`],
      ),
    );
    expect(mismatches).toEqual([]);
  });

  it("are not left empty", () => {
    const empty = languages.flatMap((language) =>
      Object.entries(messages[language]).flatMap(([key, text]) => (text.trim() ? [] : [`${language}.${key}`])),
    );
    expect(empty).toEqual([]);
  });
});
