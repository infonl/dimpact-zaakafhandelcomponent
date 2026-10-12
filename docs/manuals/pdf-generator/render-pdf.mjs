/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { tmpdir } from "node:os";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { Marked } from "marked";
import { gfmHeadingId } from "marked-gfm-heading-id";
import { chromium } from "playwright";

const [markdownFile, pdfFile, title] = process.argv.slice(2);
if (!markdownFile || !pdfFile || !title) {
  console.error("Usage: node render-pdf.mjs <markdown file> <pdf file> <title>");
  process.exit(1);
}

const require = createRequire(import.meta.url);
const githubMarkdownCss = readFileSync(
  require.resolve("github-markdown-css/github-markdown-light.css"),
  "utf8",
);
const body = new Marked({ gfm: true })
  .use(gfmHeadingId())
  .parse(readFileSync(markdownFile, "utf8"));
const manualDirectory = pathToFileURL(
  path.dirname(path.resolve(markdownFile)) + path.sep,
).href;

const html = `<!doctype html>
<html lang="nl">
<head>
<meta charset="utf-8">
<title>${title}</title>
<base href="${manualDirectory}">
<style>
${githubMarkdownCss}
.markdown-body { font-family: Helvetica, Arial, sans-serif; font-size: 13px; }
.markdown-body > p:first-child img { display: block; max-height: 24cm; margin: 0 auto; }
.markdown-body img { max-width: 100%; page-break-inside: avoid; }
.markdown-body table { display: table; width: 100%; }
.markdown-body tr, .markdown-body blockquote { page-break-inside: avoid; }
.markdown-body h1, .markdown-body h2, .markdown-body h3 { page-break-after: avoid; }
</style>
</head>
<body class="markdown-body">${body}</body>
</html>`;

// Chromium only loads the file:// images of the manual from a page that is itself opened from a file.
const workDirectory = mkdtempSync(path.join(tmpdir(), "manual-pdf-"));
const htmlFile = path.join(workDirectory, "manual.html");
writeFileSync(htmlFile, html);

const browser = await chromium.launch();
try {
  const page = await browser.newPage();
  await page.goto(pathToFileURL(htmlFile).href, { waitUntil: "networkidle" });
  const brokenImages = await page.$$eval("img", (images) =>
    images
      .filter((image) => !image.complete || image.naturalWidth === 0)
      .map((image) => image.getAttribute("src")),
  );
  if (brokenImages.length > 0) {
    throw new Error(`Images that could not be loaded: ${brokenImages.join(", ")}`);
  }
  await page.pdf({
    path: pdfFile,
    format: "A4",
    printBackground: true,
    displayHeaderFooter: true,
    headerTemplate: "<div></div>",
    footerTemplate:
      '<div style="font-size: 9px; margin: 0 auto;"><span class="pageNumber"></span> / <span class="totalPages"></span></div>',
    margin: { top: "1.5cm", bottom: "1.5cm", left: "1.5cm", right: "1.5cm" },
  });
  console.log(`Generated ${pdfFile}`);
} finally {
  await browser.close();
  rmSync(workDirectory, { recursive: true, force: true });
}
