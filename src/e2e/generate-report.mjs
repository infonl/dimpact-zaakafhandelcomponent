/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import report from "multiple-cucumber-html-reporter";
import { readdir, stat, writeFile } from "fs/promises";

report.generate({
  jsonDir: "./reports/",
  reportPath: "./reports/",
  pageTitle: "Dimpact e2e test",
  reportName: "E2E Test Report",
  openReportInBrowser: false,
  displayDuration: true,
  customData: {
    title: "Run info",
    data: [
      { label: "App", value: "Dimpact e2e test" },
      { label: "Test Environment", value: "Test" },
    ],
  },
});
await writeVideoHtmlPage()

async function dirRecursive(d = "", root = "./reports/videos/") {
  const fullPath = root + d
  const st = await stat(fullPath)
  if(st.isFile()) {
    // these are empty videos
    if(st.size < 8000) return undefined
    return d
  }
  const sub = await readdir(fullPath)
  const inner = (await Promise.all(sub.map(x => dirRecursive(x, fullPath + '/'))))
    .filter(Boolean)
  if(!d) return inner
  return [d, inner]
}

async function writeVideoHtmlPage() {
  const html = await generateVideoHtml()
  await writeFile("reports/videos.html", html, {encoding: 'utf8'})
}
// src/e2e/reports/videos/Zaken%20verdelen%20%2F%20vrijgeven/Bob%20distributes%20zaken%20to%20a%20group/9f2e3525deb9bcd64b5aaa2ed263170c.webm

async function generateVideoHtml() {
  return `
  <!DOCTYPE html>
  <html>
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Playwright videos</title>
    <style type="text/css">
      :root {
        color-scheme: dark light;
      }

      body {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
        padding-inline: calc(50vw - 400px);
      }

      video {
        width: 100%;
      }

      @media (prefers-color-scheme: light) {
        video {
          outline: 2px black solid;
        }
      }

      video:not(:first-of-type) {
        margin-block-start: 2rem;
      }
    </style>
  </head>
  <body>
    ${await generateVideoList()}
  </body>
  </html>
  `
}

async function generateVideoList() {
  const tree = await dirRecursive()
  return tree.map(([k,v]) => `
      <h1>${decodeURIComponent(k)}</h1>${v.map(([kk,vv]) => `
      <h2>${decodeURIComponent(kk)}</h2>${vv.map(vvv =>`
      <video controls autoplay muted loop>
        <source src="videos/${encodeURIComponent(k)}/${encodeURIComponent(kk)}/${(vvv)}" type="video/webm" />
      </video>`).join('')}
    `).join('')}`).join('')
}
