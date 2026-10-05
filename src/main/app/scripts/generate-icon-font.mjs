#!/usr/bin/env node

/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import * as harfbuzz from "harfbuzzjs";
import { mkdirSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { compress, decompress } from "woff2-encoder";

const appDirectory = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const scannedExtensions = new Set([".ts", ".html", ".less", ".css", ".json"]);
const ligatureCharacters = "abcdefghijklmnopqrstuvwxyz0123456789_";

// FILL stays variable: styles.less switches between filled and outlined icons with it.
const pinnedAxes = ["wght", "GRAD", "opsz"];

const HARFBUZZ_MEMORY_MODE_WRITABLE = 2;
const HARFBUZZ_SUBSET_FLAG_NO_LAYOUT_CLOSURE = 0x200;

function toTag(axis) {
  return [...axis].reduce(
    (tag, character) => (tag << 8) | character.charCodeAt(0),
    0,
  );
}

function listSourceFiles(directory, skippedDirectory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      return entryPath === skippedDirectory
        ? []
        : listSourceFiles(entryPath, skippedDirectory);
    }
    return scannedExtensions.has(path.extname(entry.name)) ? [entryPath] : [];
  });
}

function collectSourceWords(sourceDirectory) {
  const words = new Set();
  const generatedDirectory = path.join(sourceDirectory, "generated");
  for (const file of listSourceFiles(sourceDirectory, generatedDirectory)) {
    for (const word of readFileSync(file, "utf8").match(/[a-z][a-z0-9_]+/g) ??
      []) {
      words.add(word);
    }
  }
  return words;
}

function findIconGlyphs(fontData, words) {
  const font = new harfbuzz.Font(
    new harfbuzz.Face(new harfbuzz.Blob(fontData)),
  );
  const iconGlyphs = new Map();
  for (const word of words) {
    const buffer = new harfbuzz.Buffer();
    buffer.addText(word);
    buffer.guessSegmentProperties();
    harfbuzz.shape(font, buffer);
    const glyphs = buffer.getGlyphInfos();
    if (glyphs.length === 1 && glyphs[0].codepoint !== 0) {
      iconGlyphs.set(word, glyphs[0].codepoint);
    }
  }
  return iconGlyphs;
}

async function subsetFont(fontData, glyphIds) {
  const wasm = readFileSync(
    new URL(import.meta.resolve("harfbuzzjs/dist/harfbuzz-subset.wasm")),
  );
  const { instance } = await WebAssembly.instantiate(wasm);
  const harfbuzzSubset = instance.exports;
  const heap = () => new Uint8Array(harfbuzzSubset.memory.buffer);

  const fontPointer = harfbuzzSubset.malloc(fontData.byteLength);
  heap().set(fontData, fontPointer);
  const blob = harfbuzzSubset.hb_blob_create(
    fontPointer,
    fontData.byteLength,
    HARFBUZZ_MEMORY_MODE_WRITABLE,
    0,
    0,
  );
  const face = harfbuzzSubset.hb_face_create(blob, 0);
  const input = harfbuzzSubset.hb_subset_input_create_or_fail();

  if (glyphIds) {
    const unicodes = harfbuzzSubset.hb_subset_input_unicode_set(input);
    for (const character of ligatureCharacters) {
      harfbuzzSubset.hb_set_add(unicodes, character.codePointAt(0));
    }
    const glyphs = harfbuzzSubset.hb_subset_input_glyph_set(input);
    for (const glyphId of glyphIds) harfbuzzSubset.hb_set_add(glyphs, glyphId);
    // Without this, every ligature reachable from the kept letters would be kept, i.e. all icons.
    harfbuzzSubset.hb_subset_input_set_flags(
      input,
      harfbuzzSubset.hb_subset_input_get_flags(input) |
        HARFBUZZ_SUBSET_FLAG_NO_LAYOUT_CLOSURE,
    );
  } else {
    harfbuzzSubset.hb_subset_input_keep_everything(input);
  }
  for (const axis of pinnedAxes) {
    if (
      !harfbuzzSubset.hb_subset_input_pin_axis_to_default(
        input,
        face,
        toTag(axis),
      )
    ) {
      throw new Error(`Could not pin the variable font axis ${axis}`);
    }
  }

  const subsetFace = harfbuzzSubset.hb_subset_or_fail(face, input);
  if (!subsetFace) throw new Error("HarfBuzz could not subset the font");
  const subsetBlob = harfbuzzSubset.hb_face_reference_blob(subsetFace);
  const dataPointer = harfbuzzSubset.hb_blob_get_data(subsetBlob, 0);
  return heap().slice(
    dataPointer,
    dataPointer + harfbuzzSubset.hb_blob_get_length(subsetBlob),
  );
}

export async function generateIconFont({
  sourceFont,
  sourceDirectory,
  outputFont,
  keepAllIcons = false,
}) {
  const fullFont = await decompress(readFileSync(sourceFont));
  const usedIcons = findIconGlyphs(
    fullFont,
    collectSourceWords(sourceDirectory),
  );
  if (usedIcons.size === 0) {
    throw new Error("No icon names found in the source");
  }

  const generatedFont = await subsetFont(
    fullFont,
    keepAllIcons ? null : new Set(usedIcons.values()),
  );

  const generatedIcons = findIconGlyphs(generatedFont, usedIcons.keys());
  const missingIcons = [...usedIcons.keys()].filter(
    (name) => !generatedIcons.has(name),
  );
  if (missingIcons.length > 0) {
    throw new Error(`Icons missing from the generated font: ${missingIcons}`);
  }

  const woff2 = await compress(generatedFont);
  mkdirSync(path.dirname(outputFont), { recursive: true });
  writeFileSync(outputFont, woff2);
  return { iconNames: [...usedIcons.keys()], byteLength: woff2.length };
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const keepAllIcons = process.argv.includes("--all-icons");
  const outputFont = path.join(
    appDirectory,
    "src/generated/fonts/material-symbols-outlined.woff2",
  );
  const { iconNames, byteLength } = await generateIconFont({
    sourceFont: path.join(appDirectory, "fonts/MaterialSymbolsOutlined.woff2"),
    sourceDirectory: path.join(appDirectory, "src"),
    outputFont,
    keepAllIcons,
  });
  console.log(
    `Icon font: ${keepAllIcons ? "all icons" : `${iconNames.length} icons`}, ${(byteLength / 1024).toFixed(1)} KB -> ${path.relative(appDirectory, outputFont)}`,
  );
}
