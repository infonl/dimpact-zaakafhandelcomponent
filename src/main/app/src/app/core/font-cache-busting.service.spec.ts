/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { FontCacheBustingService } from "./font-cache-busting.service";

// Mock window.location for testing
Object.defineProperty(window, "location", {
  value: {
    hostname: "localhost",
  },
  writable: true,
});

describe("FontCacheBustingService", () => {
  let service: FontCacheBustingService;

  beforeEach(() => {
    service = new FontCacheBustingService();
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should add cache busting parameter to font URL", () => {
    const fontPath = "./assets/fonts/Roboto/400.woff2";
    const result = service.getFontUrl(fontPath);

    expect(result).toBe("./assets/fonts/Roboto/400.woff2?v=test1234");
  });

  it("should handle URLs that already have parameters", () => {
    const fontPath = "./assets/fonts/Roboto/400.woff2?existing=param";
    const result = service.getFontUrl(fontPath);

    expect(result).toBe(
      "./assets/fonts/Roboto/400.woff2?existing=param&v=test1234"
    );
  });

  it("should return Material Symbols font URL with cache busting", () => {
    const result = service.getMaterialSymbolsFontUrl();

    expect(result).toBe("./assets/MaterialSymbolsOutlined.woff2?v=test1234");
  });

  it("should return Roboto font URLs with cache busting", () => {
    const weights = ["300", "400", "500"] as const;

    weights.forEach((weight) => {
      const result = service.getRobotoFontUrl(weight);
      expect(result).toBe(`./assets/fonts/Roboto/${weight}.woff2?v=test1234`);
    });
  });

  it("should use consistent hash for all font URLs", () => {
    const url1 = service.getMaterialSymbolsFontUrl();
    const url2 = service.getRobotoFontUrl("400");

    const hash1 = url1.match(/\?v=([a-f0-9]+)/)?.[1];
    const hash2 = url2.match(/\?v=([a-f0-9]+)/)?.[1];

    expect(hash1).toBe(hash2);
  });
});
