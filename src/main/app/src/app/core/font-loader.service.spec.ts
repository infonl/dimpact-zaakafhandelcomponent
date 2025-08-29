/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FontCacheBustingService } from "./font-cache-busting.service";
import { FontLoaderService } from "./font-loader.service";

// Mock FontCacheBustingService for testing
class MockFontCacheBustingService {
  getMaterialSymbolsFontUrl = jest
    .fn()
    .mockReturnValue("./assets/MaterialSymbolsOutlined.woff2?v=abc12345");
  getRobotoFontUrl = jest
    .fn()
    .mockImplementation(
      (weight: string) => `./assets/fonts/Roboto/${weight}.woff2?v=abc12345`,
    );
}

describe("FontLoaderService", () => {
  let service: FontLoaderService;
  let fontCacheBustingService: MockFontCacheBustingService;

  beforeEach(() => {
    fontCacheBustingService = new MockFontCacheBustingService();
    service = new FontLoaderService(
      fontCacheBustingService as unknown as FontCacheBustingService,
    );
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should load fonts when called", () => {
    // Mock document.head
    const mockStyle = document.createElement("style");
    jest.spyOn(document, "createElement").mockReturnValue(mockStyle);
    jest.spyOn(document.head, "appendChild");

    service.loadFonts();

    expect(
      fontCacheBustingService.getMaterialSymbolsFontUrl,
    ).toHaveBeenCalled();
    expect(fontCacheBustingService.getRobotoFontUrl).toHaveBeenCalledWith(
      "300",
    );
    expect(fontCacheBustingService.getRobotoFontUrl).toHaveBeenCalledWith(
      "400",
    );
    expect(fontCacheBustingService.getRobotoFontUrl).toHaveBeenCalledWith(
      "500",
    );
  });

  it("should not load fonts twice", () => {
    const mockStyle = document.createElement("style");
    jest.spyOn(document, "createElement").mockReturnValue(mockStyle);
    jest.spyOn(document.head, "appendChild");

    // Load fonts first time
    service.loadFonts();
    const firstCallCount =
      fontCacheBustingService.getMaterialSymbolsFontUrl.mock.calls.length;

    // Load fonts second time
    service.loadFonts();
    const secondCallCount =
      fontCacheBustingService.getMaterialSymbolsFontUrl.mock.calls.length;

    // Should not have called the service again
    expect(secondCallCount).toBe(firstCallCount);
  });

  it("should inject CSS with correct font-face rules", () => {
    const mockStyle = document.createElement("style");
    jest.spyOn(document, "createElement").mockReturnValue(mockStyle);
    jest.spyOn(document.head, "appendChild");

    service.loadFonts();

    expect(mockStyle.textContent).toContain("@font-face");
    expect(mockStyle.textContent).toContain("font-family: 'Roboto'");
    expect(mockStyle.textContent).toContain("font-display: swap");

    // Check that both Material Symbols and Roboto fonts were requested
    expect(
      fontCacheBustingService.getMaterialSymbolsFontUrl,
    ).toHaveBeenCalled();
    expect(fontCacheBustingService.getRobotoFontUrl).toHaveBeenCalledWith(
      "300",
    );
    expect(fontCacheBustingService.getRobotoFontUrl).toHaveBeenCalledWith(
      "400",
    );
    expect(fontCacheBustingService.getRobotoFontUrl).toHaveBeenCalledWith(
      "500",
    );
  });

  it("should remove existing font styles before adding new ones", () => {
    const existingStyle = document.createElement("style");
    existingStyle.id = "font-material-symbols-outlined";
    document.head.appendChild(existingStyle);

    const mockStyle = document.createElement("style");
    jest.spyOn(document, "createElement").mockReturnValue(mockStyle);
    jest.spyOn(existingStyle, "remove");

    service.loadFonts();

    expect(existingStyle.remove).toHaveBeenCalled();
  });
});
