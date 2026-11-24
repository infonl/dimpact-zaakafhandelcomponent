/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import type { JSONReport } from "@playwright/test/reporter";
import { slackSummary } from "./summary";

type Outcome = "expected" | "unexpected" | "flaky" | "skipped";

function reportWith(
  scenarios: {
    title: string;
    status: Outcome;
    project?: string;
    skippedBecause?: string;
    serverErrors?: string[];
  }[],
  errors: { message: string }[] = [],
) {
  return {
    errors,
    stats: { duration: 95_000 },
    suites: [
      {
        title: "intake.feature.spec.js",
        line: 0,
        specs: [],
        suites: [
          {
            title: "Intake",
            line: 3,
            specs: scenarios.map(
              ({
                title,
                status,
                project = "journeys",
                skippedBecause,
                serverErrors = [],
              }) => ({
                title,
                tests: [
                  {
                    projectName: project,
                    status,
                    results: [
                      {
                        duration: 12_000,
                        annotations: serverErrors.map((description) => ({
                          type: "server-error",
                          description,
                        })),
                      },
                    ],
                    annotations: skippedBecause
                      ? [{ type: "skip", description: skippedBecause }]
                      : [],
                  },
                ],
              }),
            ),
          },
        ],
      },
    ],
  } as unknown as JSONReport;
}

test("given a run in which every executed scenario passed, when it is summarized, then it is green and names the feature and the scenario", () => {
  const summary = slackSummary(
    reportWith([{ title: "A zaak waits", status: "expected" }]),
  );

  expect(summary).toContain("✅ *1/1 passed* · 0 skipped · total *1m 35s*");
  expect(summary).toContain("• Intake → A zaak waits — 12s");
});

test("given a failed and a flaky scenario, when the run is summarized, then it is red and lists both", () => {
  const summary = slackSummary(
    reportWith([
      { title: "fails", status: "unexpected" },
      { title: "wavers", status: "flaky" },
    ]),
  );

  expect(summary).toContain("❌ *0/2 passed*");
  expect(summary).toContain("*❌ Failed (1)*\n• Intake → fails");
  expect(summary).toContain("*🎲 Flaky (1)*\n• Intake → wavers");
});

test("given server errors of ZAC, when the run is summarized, then it counts them per cause and for each scenario that did not pass", () => {
  const klanten = "Server response from Klanten: 500";
  const summary = slackSummary(
    reportWith([
      {
        title: "fails",
        status: "unexpected",
        serverErrors: [
          `${klanten} (GET /rest/zaken/zaak/{uuid})`,
          `${klanten} (GET /rest/zaken/zaak/id/{id})`,
        ],
      },
      { title: "wavers", status: "flaky" },
      {
        title: "passes",
        status: "expected",
        serverErrors: ["HTTP 502 (GET /rest/taken)"],
      },
    ]),
  );

  expect(summary).toContain(`• Intake → fails — 2 × ${klanten}`);
  expect(summary).toContain("• Intake → wavers — no server errors");
  expect(summary).toContain(
    `*⚠️ Server errors of ZAC during the run (3)*\n• 2 × ${klanten}\n• 1 × HTTP 502`,
  );
});

test("given a skipped scenario, when the run is summarized, then it tells why, so a gap in the coverage of the target stays visible", () => {
  const summary = slackSummary(
    reportWith([
      { title: "passes", status: "expected" },
      { title: "skips", status: "skipped", skippedBecause: "no mailbox" },
    ]),
  );

  expect(summary).toContain("✅ *1/1 passed* · 1 skipped");
  expect(summary).toContain("• Intake → skips — no mailbox");
});

test("given sign ins of the setup project, when the run is summarized, then only a failed one shows", () => {
  const summary = slackSummary(
    reportWith([
      { title: "sign in works", status: "expected", project: "setup" },
      { title: "sign in fails", status: "unexpected", project: "setup" },
    ]),
  );

  expect(summary).not.toContain("sign in works");
  expect(summary).toContain("sign in fails");
});

test("given a run without an executed scenario, when it is summarized, then it is red", () => {
  expect(slackSummary(reportWith([]))).toContain("❌ *0/0 passed*");
});

test("given an error outside a scenario, when the run is summarized, then it is red and shows the error", () => {
  const summary = slackSummary(
    reportWith(
      [{ title: "passes", status: "expected" }],
      [{ message: "No config" }],
    ),
  );

  expect(summary).toContain("❌ *1/1 passed*");
  expect(summary).toContain("No config");
});

test("given more failures than fit in a Slack section, when the run is summarized, then the text is cut off at what Slack accepts", () => {
  const summary = slackSummary(
    reportWith(
      Array.from({ length: 60 }, (_, index) => ({
        title: `scenario ${index} ${"x".repeat(90)}`,
        status: "unexpected" as const,
      })),
    ),
  );

  expect(Array.from(summary).length).toBeLessThanOrEqual(3000);
});

test("given a name with markup of Slack, when the run is summarized, then it is escaped", () => {
  expect(
    slackSummary(reportWith([{ title: "<a> & b", status: "unexpected" }])),
  ).toContain("&lt;a&gt; &amp; b");
});
