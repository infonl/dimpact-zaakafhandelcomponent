#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const {
  generateTranslationHash,
  generateFontHash,
} = require("./cache-busting");

describe("Cache Busting Script", () => {
  const testDir = path.join(__dirname, "test-temp");

  beforeEach(() => {
    // Create test directory
    if (!fs.existsSync(testDir)) {
      fs.mkdirSync(testDir, { recursive: true });
    }

    // Create test i18n directory
    const i18nDir = path.join(testDir, "i18n");
    if (!fs.existsSync(i18nDir)) {
      fs.mkdirSync(i18nDir, { recursive: true });
    }

    // Create test assets directory
    const assetsDir = path.join(testDir, "assets");
    if (!fs.existsSync(assetsDir)) {
      fs.mkdirSync(assetsDir, { recursive: true });
    }

    const fontsDir = path.join(assetsDir, "fonts", "Roboto");
    if (!fs.existsSync(fontsDir)) {
      fs.mkdirSync(fontsDir, { recursive: true });
    }
  });

  afterEach(() => {
    // Clean up test directory
    if (fs.existsSync(testDir)) {
      fs.rmSync(testDir, { recursive: true, force: true });
    }
  });

  describe("generateTranslationHash", () => {
    it("should generate consistent hash for same content", () => {
      const i18nDir = path.join(testDir, "i18n");

      // Create test translation files
      fs.writeFileSync(path.join(i18nDir, "en.json"), '{"key": "value"}');
      fs.writeFileSync(path.join(i18nDir, "nl.json"), '{"key": "waarde"}');

      const originalDir = process.cwd();
      process.chdir(testDir);

      const hash1 = generateTranslationHash();
      const hash2 = generateTranslationHash();

      process.chdir(originalDir);

      expect(hash1).toBe(hash2);
      expect(hash1).toMatch(/^[a-f0-9]{8}$/);
    });

    it("should generate different hash for different content", () => {
      const i18nDir = path.join(testDir, "i18n");

      // Create test translation files
      fs.writeFileSync(path.join(i18nDir, "en.json"), '{"key": "value"}');
      fs.writeFileSync(path.join(i18nDir, "nl.json"), '{"key": "waarde"}');

      const originalDir = process.cwd();
      process.chdir(testDir);

      const hash1 = generateTranslationHash();

      // Change content
      fs.writeFileSync(path.join(i18nDir, "en.json"), '{"key": "new value"}');

      const hash2 = generateTranslationHash();

      process.chdir(originalDir);

      expect(hash1).not.toBe(hash2);
    });
  });

  describe("generateFontHash", () => {
    it("should generate consistent hash for same fonts", () => {
      const assetsDir = path.join(testDir, "assets");

      // Create test font files
      fs.writeFileSync(
        path.join(assetsDir, "MaterialSymbolsOutlined.woff2"),
        "font-data-1"
      );
      fs.writeFileSync(
        path.join(assetsDir, "fonts", "Roboto", "300.woff2"),
        "font-data-2"
      );
      fs.writeFileSync(
        path.join(assetsDir, "fonts", "Roboto", "400.woff2"),
        "font-data-3"
      );
      fs.writeFileSync(
        path.join(assetsDir, "fonts", "Roboto", "500.woff2"),
        "font-data-4"
      );

      const originalDir = process.cwd();
      process.chdir(testDir);

      const hash1 = generateFontHash();
      const hash2 = generateFontHash();

      process.chdir(originalDir);

      expect(hash1).toBe(hash2);
      expect(hash1).toMatch(/^[a-f0-9]{8}$/);
    });

    it("should generate different hash when font files change", () => {
      const assetsDir = path.join(testDir, "assets");

      // Create test font files
      fs.writeFileSync(
        path.join(assetsDir, "MaterialSymbolsOutlined.woff2"),
        "font-data-1"
      );
      fs.writeFileSync(
        path.join(assetsDir, "fonts", "Roboto", "300.woff2"),
        "font-data-2"
      );

      const originalDir = process.cwd();
      process.chdir(testDir);

      const hash1 = generateFontHash();

      // Change font file
      fs.writeFileSync(
        path.join(assetsDir, "MaterialSymbolsOutlined.woff2"),
        "font-data-changed"
      );

      const hash2 = generateFontHash();

      process.chdir(originalDir);

      expect(hash1).not.toBe(hash2);
    });
  });
});
