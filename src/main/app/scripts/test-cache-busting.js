#!/usr/bin/env node

/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

/**
 * Test script voor cache busting implementatie
 */
function testCacheBusting() {
  console.log("🧪 Testing cache busting implementation...\n");

  try {
    // 1. Test build process
    console.log("1. Building application...");
    execSync("npm run build", { stdio: "inherit" });
    console.log("✅ Build completed successfully\n");

    // 2. Verify cache busting script execution
    console.log("2. Verifying cache busting script...");
    const distDir = path.join(__dirname, "../dist/zaakafhandelcomponent");

    if (!fs.existsSync(distDir)) {
      throw new Error("Dist directory not found");
    }

    console.log("✅ Dist directory exists\n");

    // 3. Check if hashes were replaced
    console.log("3. Checking hash replacement...");
    const builtFiles = findBuiltFiles(distDir);
    let hashReplaced = false;

    builtFiles.forEach((file) => {
      const content = fs.readFileSync(file, "utf8");
      if (
        content.includes("TRANSLATION_HASH") ||
        content.includes("FONT_HASH")
      ) {
        console.log(
          `⚠️  Warning: Placeholder hash found in ${path.relative(distDir, file)}`,
        );
      } else if (content.includes("?v=")) {
        hashReplaced = true;
        console.log(
          `✅ Cache busting hash found in ${path.relative(distDir, file)}`,
        );
      }
    });

    if (!hashReplaced) {
      throw new Error("No cache busting hashes found in built files");
    }

    console.log("✅ Hash replacement verified\n");

    // 4. Test translation files
    console.log("4. Testing translation files...");
    const i18nDir = path.join(__dirname, "../src/assets/i18n");
    const translationFiles = ["en.json", "nl.json"];

    translationFiles.forEach((file) => {
      const filePath = path.join(i18nDir, file);
      if (!fs.existsSync(filePath)) {
        throw new Error(`Translation file not found: ${file}`);
      }

      const content = fs.readFileSync(filePath, "utf8");
      const parsed = JSON.parse(content);

      if (Object.keys(parsed).length === 0) {
        throw new Error(`Translation file is empty: ${file}`);
      }

      console.log(
        `✅ Translation file ${file} is valid (${Object.keys(parsed).length} keys)`,
      );
    });

    console.log("✅ Translation files verified\n");

    // 5. Test font files
    console.log("5. Testing font files...");
    const fontFiles = [
      "src/assets/MaterialSymbolsOutlined.woff2",
      "src/assets/fonts/Roboto/300.woff2",
      "src/assets/fonts/Roboto/400.woff2",
      "src/assets/fonts/Roboto/500.woff2",
    ];

    fontFiles.forEach((fontFile) => {
      const filePath = path.join(__dirname, "..", fontFile);
      if (!fs.existsSync(filePath)) {
        throw new Error(`Font file not found: ${fontFile}`);
      }

      const stats = fs.statSync(filePath);
      console.log(
        `✅ Font file ${fontFile} exists (${(stats.size / 1024).toFixed(1)} KB)`,
      );
    });

    console.log("✅ Font files verified\n");

    // 6. Test cache busting script directly
    console.log("6. Testing cache busting script directly...");
    execSync("npm run cache-bust", { stdio: "inherit" });
    console.log("✅ Cache busting script executed successfully\n");

    // 7. Verify final build
    console.log("7. Verifying final build...");
    const finalBuiltFiles = findBuiltFiles(distDir);
    let finalHashReplaced = false;

    finalBuiltFiles.forEach((file) => {
      const content = fs.readFileSync(file, "utf8");
      if (
        content.includes("TRANSLATION_HASH") ||
        content.includes("FONT_HASH")
      ) {
        throw new Error(
          `Placeholder hash still found in ${path.relative(distDir, file)}`,
        );
      } else if (content.includes("?v=")) {
        finalHashReplaced = true;
      }
    });

    if (!finalHashReplaced) {
      throw new Error("No cache busting hashes found after final build");
    }

    console.log("✅ Final build verified\n");

    console.log("🎉 All cache busting tests passed!");
    console.log("\n📋 Summary:");
    console.log("  ✅ Build process works");
    console.log("  ✅ Cache busting script executes");
    console.log("  ✅ Translation files are valid");
    console.log("  ✅ Font files exist");
    console.log("  ✅ Hashes are properly replaced");
    console.log("  ✅ Final build is ready for deployment");
  } catch (error) {
    console.error("❌ Test failed:", error.message);
    process.exit(1);
  }
}

/**
 * Find all JavaScript and HTML files in a directory recursively
 */
function findBuiltFiles(dir) {
  const files = [];

  function scanDirectory(currentDir) {
    const items = fs.readdirSync(currentDir);

    items.forEach((item) => {
      const itemPath = path.join(currentDir, item);
      const stat = fs.statSync(itemPath);

      if (stat.isDirectory()) {
        scanDirectory(itemPath);
      } else if (item.endsWith(".js") || item.endsWith(".html")) {
        files.push(itemPath);
      }
    });
  }

  scanDirectory(dir);
  return files;
}

// Run the test
if (require.main === module) {
  testCacheBusting();
}

module.exports = { testCacheBusting };
