/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import * as harfbuzz from "harfbuzzjs";
import assert from "node:assert/strict";
import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { afterEach, beforeEach, describe, it } from "node:test";
import { fileURLToPath } from "node:url";
import { decompress } from "woff2-encoder";
import { generateIconFont } from "./generate-icon-font.mjs";

const sourceFont = fileURLToPath(
  new URL("../fonts/MaterialSymbolsOutlined.woff2", import.meta.url),
);

describe("generateIconFont", () => {
  let workDirectory;
  let sourceDirectory;
  let outputFont;

  beforeEach(() => {
    workDirectory = mkdtempSync(path.join(tmpdir(), "icon-font-"));
    sourceDirectory = path.join(workDirectory, "src");
    outputFont = path.join(workDirectory, "out/fonts/icons.woff2");
  });

  afterEach(() => {
    rmSync(workDirectory, { recursive: true, force: true });
  });

  function writeSourceFile(relativePath, content) {
    const file = path.join(sourceDirectory, relativePath);
    mkdirSync(path.dirname(file), { recursive: true });
    writeFileSync(file, content);
  }

  async function generate(options = {}) {
    return generateIconFont({
      sourceFont,
      sourceDirectory,
      outputFont,
      ...options,
    });
  }

  async function loadGeneratedFont() {
    const fontData = await decompress(readFileSync(outputFont));
    const face = new harfbuzz.Face(new harfbuzz.Blob(fontData));
    return { face, font: new harfbuzz.Font(face) };
  }

  function shape(font, text) {
    const buffer = new harfbuzz.Buffer();
    buffer.addText(text);
    buffer.guessSegmentProperties();
    harfbuzz.shape(font, buffer);
    return buffer.getGlyphInfos().map((glyph) => glyph.codepoint);
  }

  function rendersAsIcon(font, name) {
    const glyphs = shape(font, name);
    return glyphs.length === 1 && glyphs[0] !== 0;
  }

  it("keeps an icon whose name appears in the source, so that it renders as one glyph", async () => {
    writeSourceFile("app/a.component.html", "<mat-icon>edit</mat-icon>");

    const { iconNames } = await generate();

    const { font } = await loadGeneratedFont();
    assert.ok(iconNames.includes("edit"));
    assert.ok(rendersAsIcon(font, "edit"));
  });

  it("leaves out an icon whose name appears nowhere in the source, so that it renders as plain letters", async () => {
    writeSourceFile("app/a.component.html", "<mat-icon>edit</mat-icon>");

    await generate();

    const { font } = await loadGeneratedFont();
    assert.equal(shape(font, "pets").length, "pets".length);
  });

  it("finds icon names in .ts, .html, .less, .css and .json files", async () => {
    writeSourceFile("app/a.ts", 'const icon = "home";');
    writeSourceFile("app/a.html", "<mat-icon>mail</mat-icon>");
    writeSourceFile("app/a.less", '.a::after { content: "folder"; }');
    writeSourceFile("app/a.css", '.a::after { content: "delete"; }');
    writeSourceFile("assets/a.json", '{ "icon": "search" }');

    const { iconNames } = await generate();

    for (const name of ["home", "mail", "folder", "delete", "search"]) {
      assert.ok(iconNames.includes(name), `${name} is missing`);
    }
  });

  it("ignores icon names in other file types", async () => {
    writeSourceFile("app/a.html", "<mat-icon>edit</mat-icon>");
    writeSourceFile("app/README.md", "pets");

    const { iconNames } = await generate();

    assert.ok(!iconNames.includes("pets"));
  });

  it("ignores icon names in src/generated, so that the generated OpenAPI types do not pull in icons", async () => {
    writeSourceFile("app/a.html", "<mat-icon>edit</mat-icon>");
    writeSourceFile("generated/types/api.d.ts", '"pets": string;');

    const { iconNames } = await generate();

    assert.ok(!iconNames.includes("pets"));
  });

  it("keeps every icon when asked to, so that a new icon works in the dev server without a restart", async () => {
    writeSourceFile("app/a.component.html", "<mat-icon>edit</mat-icon>");

    await generate({ keepAllIcons: true });

    const { font } = await loadGeneratedFont();
    assert.ok(rendersAsIcon(font, "pets"));
  });

  it("keeps only the FILL axis variable, so that styles.less can switch between filled and outlined icons", async () => {
    writeSourceFile("app/a.component.html", "<mat-icon>home</mat-icon>");

    await generate();

    const { face, font } = await loadGeneratedFont();
    assert.deepEqual(Object.keys(face.getAxisInfos()), ["FILL"]);
    const [homeGlyph] = shape(font, "home");
    font.setVariations([new harfbuzz.Variation("FILL", 0)]);
    const outlined = font.glyphToPath(homeGlyph);
    font.setVariations([new harfbuzz.Variation("FILL", 1)]);
    const filled = font.glyphToPath(homeGlyph);
    assert.notEqual(filled, outlined);
  });

  it("writes a much smaller woff2 file than the source font", async () => {
    writeSourceFile("app/a.component.html", "<mat-icon>edit</mat-icon>");

    const { byteLength } = await generate();

    const written = readFileSync(outputFont);
    assert.equal(written.subarray(0, 4).toString("latin1"), "wOF2");
    assert.equal(written.length, byteLength);
    assert.ok(byteLength < readFileSync(sourceFont).length / 100);
  });

  it("fails when the source contains no icon names, so that a broken scan never ships an empty font", async () => {
    writeSourceFile("app/a.ts", "const answer = 42;");

    await assert.rejects(generate(), /No icon names found/);
  });
});
