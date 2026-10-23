/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

// Checks qa-output/results.json before the QA agent finishes, because the Slack and summary steps
// can't use a file that doesn't parse.
import { readFileSync } from "fs";

const file = new URL("../../../qa-output/results.json", import.meta.url);
const verdicts = ["pass", "fail", "needs-human", "not-testable"];
const problems = [];

let results;
try {
  results = JSON.parse(readFileSync(file, "utf8"));
} catch (error) {
  console.error(`qa-output/results.json is not valid JSON: ${error.message}`);
  process.exit(1);
}

if (
  typeof results.slack_message !== "string" ||
  results.slack_message.trim() === ""
) {
  problems.push('"slack_message" must be a non-empty string');
}
if (!Array.isArray(results.tickets) || results.tickets.length === 0) {
  problems.push('"tickets" must be a non-empty array');
} else {
  results.tickets.forEach((ticket, index) => {
    if (!Array.isArray(ticket.prs))
      problems.push(`tickets[${index}].prs must be an array of PR numbers`);
    if (!verdicts.includes(ticket.verdict)) {
      problems.push(
        `tickets[${index}].verdict must be one of ${verdicts.join(", ")}`,
      );
    }
    if (typeof ticket.summary_line !== "string")
      problems.push(`tickets[${index}].summary_line must be a string`);
  });
}

if (problems.length > 0) {
  console.error(
    `qa-output/results.json has problems:\n- ${problems.join("\n- ")}`,
  );
  process.exit(1);
}
console.log(
  `qa-output/results.json is valid: ${results.tickets.length} tickets`,
);
