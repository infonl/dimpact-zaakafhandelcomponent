#!/usr/bin/env node

/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

/**
 * Generates a hash for translation files
 */
function generateTranslationHash() {
  const i18nDir = path.join(__dirname, "../src/assets/i18n");

  if (!fs.existsSync(i18nDir)) {
    console.log("i18n directory not found, returning empty hash");
    return crypto.createHash("md5").update("").digest("hex").substring(0, 8);
  }

  const files = fs
    .readdirSync(i18nDir)
    .filter((file) => file.endsWith(".json"))
    .sort(); // Sort for consistent ordering

  let combinedContent = "";

  files.forEach((file) => {
    const filePath = path.join(i18nDir, file);
    const content = fs.readFileSync(filePath, "utf8");
    combinedContent += content;
  });

  // Generate a short hash from the combined content
  return crypto
    .createHash("md5")
    .update(combinedContent)
    .digest("hex")
    .substring(0, 8);
}

/**
 * Generates a hash for font files
 */
function generateFontHash() {
  const assetsDir = path.join(__dirname, "../src/assets");
  const fontFiles = [
    "MaterialSymbolsOutlined.woff2",
    "fonts/Roboto/300.woff2",
    "fonts/Roboto/400.woff2",
    "fonts/Roboto/500.woff2",
  ];

  let combinedContent = "";

  fontFiles.forEach((file) => {
    const filePath = path.join(assetsDir, file);
    if (fs.existsSync(filePath)) {
      const stats = fs.statSync(filePath);
      // Use file size and modification time for fonts (binary files)
      combinedContent += `${file}:${stats.size}:${stats.mtime.getTime()}`;
    }
  });

  return crypto
    .createHash("md5")
    .update(combinedContent)
    .digest("hex")
    .substring(0, 8);
}

/**
 * Replaces TRANSLATION_HASH placeholder with actual hash in the compiled JavaScript
 */
function replaceBuildHash() {
  const distDir = path.join(__dirname, "../dist/zaakafhandelcomponent");

  if (!fs.existsSync(distDir)) {
    console.log("Dist directory not found, skipping hash replacement");
    return;
  }

  const translationHash = generateTranslationHash();
  const fontHash = generateFontHash();

  console.log(`Translation hash: ${translationHash}`);
  console.log(`Font hash: ${fontHash}`);

  // Find all JavaScript files in the dist directory
  function processDirectory(dir) {
    const files = fs.readdirSync(dir);

    files.forEach((file) => {
      const filePath = path.join(dir, file);
      const stat = fs.statSync(filePath);

      if (stat.isDirectory()) {
        processDirectory(filePath);
      } else if (file.endsWith(".js") || file.endsWith(".html")) {
        let content = fs.readFileSync(filePath, "utf8");
        let fileUpdated = false;

        // Replace TRANSLATION_HASH with actual hash (only in JS files)
        if (file.endsWith(".js") && content.includes("TRANSLATION_HASH")) {
          content = content.replace(/TRANSLATION_HASH/g, translationHash);
          fs.writeFileSync(filePath, content, "utf8");
          console.log(`Updated translation hash in: ${filePath}`);
          fileUpdated = true;
        }

        // Replace FONT_HASH with actual font hash
        if (content.includes("FONT_HASH")) {
          content = content.replace(/FONT_HASH/g, fontHash);
          fs.writeFileSync(filePath, content, "utf8");
          console.log(`Updated font hash in: ${filePath}`);
          fileUpdated = true;
        }

        if (fileUpdated) {
          // Re-read the file content after updates
          content = fs.readFileSync(filePath, "utf8");
        }
      }
    });
  }

  processDirectory(distDir);
}

// Run the script
if (require.main === module) {
  try {
    replaceBuildHash();
    console.log("Cache busting completed successfully");
  } catch (error) {
    console.error("Error during cache busting:", error);
    process.exit(1);
  }
}

module.exports = {
  generateTranslationHash,
  generateFontHash,
  replaceBuildHash,
};
