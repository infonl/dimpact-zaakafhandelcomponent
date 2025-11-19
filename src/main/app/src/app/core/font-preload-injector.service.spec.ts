/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FontCacheBustingService } from "./font-cache-busting.service";
import { FontPreloadInjectorService } from "./font-preload-injector.service";

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

describe("FontPreloadInjectorService", () => {
  let service: FontPreloadInjectorService;
  let fontCacheBustingService: MockFontCacheBustingService;

  beforeEach(() => {
    fontCacheBustingService = new MockFontCacheBustingService();
    service = new FontPreloadInjectorService(
      fontCacheBustingService as unknown as FontCacheBustingService,
    );
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  it("should inject font preload links", () => {
    // Mock document.head
    const mockHead = document.createElement("head");
    jest.spyOn(document, "head", "get").mockReturnValue(mockHead);
    jest.spyOn(mockHead, "insertBefore");

    service.injectFontPreloads();

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
    expect(mockHead.insertBefore).toHaveBeenCalledTimes(4);
  });

  it("should not inject preload links twice", () => {
    const mockHead = document.createElement("head");
    const insertBeforeSpy = jest.spyOn(mockHead, "insertBefore");
    jest.spyOn(document, "head", "get").mockReturnValue(mockHead);

    // First injection
    service.injectFontPreloads();
    const firstCallCount = insertBeforeSpy.mock.calls.length;

    // Second injection
    service.injectFontPreloads();
    const secondCallCount = insertBeforeSpy.mock.calls.length;

    // Should not have called insertBefore again
    expect(secondCallCount).toBe(firstCallCount);
  });

  it("should not create duplicate links", () => {
    const mockHead = document.createElement("head");
    jest.spyOn(document, "head", "get").mockReturnValue(mockHead);
    jest.spyOn(mockHead, "insertBefore");
    jest
      .spyOn(document, "querySelector")
      .mockReturnValue(document.createElement("link"));

    service.injectFontPreloads();

    // Should not insert any links since they already exist
    expect(mockHead.insertBefore).not.toHaveBeenCalled();
  });

  it("should remove font preload links", () => {
    const mockLink1 = document.createElement("link");
    mockLink1.setAttribute("href", "/assets/fonts/Roboto/400.woff2");
    jest.spyOn(mockLink1, "remove");

    const mockLink2 = document.createElement("link");
    mockLink2.setAttribute("href", "/assets/other/resource.css");
    jest.spyOn(mockLink2, "remove");

    const mockHead = document.createElement("head");
    mockHead.appendChild(mockLink1);
    mockHead.appendChild(mockLink2);

    jest.spyOn(document, "head", "get").mockReturnValue(mockHead);
    jest
      .spyOn(document, "querySelectorAll")
      .mockReturnValue([mockLink1] as unknown as NodeListOf<Element>);

    service.removeFontPreloads();

    expect(mockLink1.remove).toHaveBeenCalled();
    expect(mockLink2.remove).not.toHaveBeenCalled();
  });
});
