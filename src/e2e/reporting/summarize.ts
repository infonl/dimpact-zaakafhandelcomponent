/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { JSONReport } from "@playwright/test/reporter";
import { appendFileSync, readFileSync } from "node:fs";
import { slackSummary } from "./summary.ts";

const [resultsFile, outputFile] = process.argv.slice(2);
const summary = slackSummary(
  JSON.parse(readFileSync(resultsFile, "utf8")) as JSONReport,
);
console.log(summary);

// The output file of a step in GitHub Actions. The Slack message takes the summary from it as a JSON string.
if (outputFile) {
  appendFileSync(outputFile, `slack_summary=${JSON.stringify(summary)}\n`);
}
