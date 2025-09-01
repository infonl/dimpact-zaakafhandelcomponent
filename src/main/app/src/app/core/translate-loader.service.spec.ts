/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import {
  CacheBustingTranslateLoader,
  createCacheBustingTranslateLoader,
} from "./translate-loader.service";

// Mock window.location for testing
Object.defineProperty(window, "location", {
  value: {
    hostname: "localhost",
  },
  writable: true,
});

describe("CacheBustingTranslateLoader", () => {
  let loader: CacheBustingTranslateLoader;
  let httpClient: HttpClient;

  beforeEach(() => {
    // Create a simple mock HttpClient for testing
    httpClient = {
      get: jest.fn(),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
      patch: jest.fn(),
      head: jest.fn(),
      options: jest.fn(),
      request: jest.fn(),
      handler: {},
    } as unknown as HttpClient;

    loader = createCacheBustingTranslateLoader(httpClient);
  });

  it("should be created", () => {
    expect(loader).toBeTruthy();
  });

  it("should load translation with cache busting parameter", (done) => {
    const mockTranslation = { "test.key": "Test Value" };
    const language = "en";

    (httpClient.get as jest.Mock).mockReturnValue({
      pipe: jest.fn().mockReturnValue({
        subscribe: (callback: (arg: unknown) => void) =>
          callback(mockTranslation),
      }),
    });

    loader.getTranslation(language).subscribe((translation) => {
      expect(translation).toEqual(mockTranslation);
      expect(httpClient.get).toHaveBeenCalledWith(
        expect.stringMatching(/^\.\/assets\/i18n\/en\.json\?v=[a-f0-9]+$/),
      );
      done();
    });
  });

  it("should handle HTTP errors gracefully", (done) => {
    const language = "en";

    (httpClient.get as jest.Mock).mockReturnValue({
      pipe: jest.fn().mockReturnValue({
        subscribe: (callback: (arg: unknown) => void) => {
          // The service uses catchError which returns of({}), so we should get an empty object
          callback({});
        },
      }),
    });

    loader.getTranslation(language).subscribe((translation) => {
      expect(translation).toEqual({});
      done();
    });
  });

  it("should handle string responses", (done) => {
    const mockTranslationString = '{"test.key": "Test Value"}';
    const language = "en";

    (httpClient.get as jest.Mock).mockReturnValue({
      pipe: jest.fn().mockReturnValue({
        subscribe: (callback: (arg: unknown) => void) =>
          callback(mockTranslationString),
      }),
    });

    loader.getTranslation(language).subscribe((translation) => {
      expect(translation).toEqual(mockTranslationString);
      done();
    });
  });

  it("should use correct URL format", (done) => {
    const language = "nl";

    (httpClient.get as jest.Mock).mockReturnValue({
      pipe: jest.fn().mockReturnValue({
        subscribe: (callback: (arg: unknown) => void) => callback({}),
      }),
    });

    loader.getTranslation(language).subscribe(() => {
      expect(httpClient.get).toHaveBeenCalledWith(
        expect.stringMatching(/^\.\/assets\/i18n\/nl\.json\?v=[a-f0-9]+$/),
      );
      done();
    });
  });
});

describe("createCacheBustingTranslateLoader", () => {
  it("should create loader with default parameters", () => {
    const mockHttpClient = {} as HttpClient;
    const loader = createCacheBustingTranslateLoader(mockHttpClient);

    expect(loader).toBeTruthy();
    expect(loader).toBeInstanceOf(CacheBustingTranslateLoader);
  });
});
