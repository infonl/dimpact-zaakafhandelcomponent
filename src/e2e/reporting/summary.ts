/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import type { JSONReport, JSONReportSuite } from "@playwright/test/reporter";

// Slack refuses a section whose text is longer than this.
const SUMMARY_LIMIT = 3000;
const NAME_LIMIT = 100;
const SLOWEST_SCENARIOS = 5;

type Scenario = {
  name: string;
  project: string;
  status: "expected" | "unexpected" | "flaky" | "skipped";
  milliseconds: number;
  skippedBecause?: string;
};

function scenariosOf(suite: JSONReportSuite, parents: string[]): Scenario[] {
  // The title of the outermost suite is the path of its file.
  const titles = suite.line === 0 ? parents : [...parents, suite.title];
  return [
    ...suite.specs.flatMap((spec) =>
      spec.tests.map((test) => ({
        name: [...titles, spec.title].join(" → "),
        project: test.projectName,
        status: test.status,
        milliseconds: test.results.reduce(
          (total, { duration }) => total + duration,
          0,
        ),
        skippedBecause: test.annotations.find(({ type }) => type === "skip")
          ?.description,
      })),
    ),
    ...(suite.suites ?? []).flatMap((child) => scenariosOf(child, titles)),
  ];
}

function duration(milliseconds: number) {
  const seconds = Math.round(milliseconds / 1000);
  return seconds < 60
    ? `${seconds}s`
    : `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
}

function escaped(text: string) {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function shortened(text: string, limit: number) {
  const characters = Array.from(text);
  return characters.length > limit
    ? `${characters.slice(0, limit - 1).join("")}…`
    : text;
}

function section(title: string, lines: string[]) {
  return lines.length > 0 ? ["", `*${title}*`, ...lines] : [];
}

/** The result of the run as Slack mrkdwn. The sign in of the setup project only shows when it fails. */
export function slackSummary(report: JSONReport) {
  const scenarios = report.suites
    .flatMap((suite) => scenariosOf(suite, []))
    .filter(({ project, status }) =>
      project === "setup" ? status === "unexpected" : true,
    );
  const withStatus = (status: Scenario["status"]) =>
    scenarios.filter((scenario) => scenario.status === status);
  const line = ({ name }: Scenario, detail?: string) =>
    `• ${escaped(shortened(name, NAME_LIMIT))}${detail ? ` — ${escaped(shortened(detail, NAME_LIMIT))}` : ""}`;

  const passed = withStatus("expected");
  const failed = withStatus("unexpected");
  const flaky = withStatus("flaky");
  const skipped = withStatus("skipped");
  const executed = passed.length + failed.length + flaky.length;
  const isGreen =
    executed > 0 && passed.length === executed && report.errors.length === 0;
  const slowest = [...passed]
    .sort((a, b) => b.milliseconds - a.milliseconds)
    .slice(0, SLOWEST_SCENARIOS);

  return shortened(
    [
      `${isGreen ? "✅" : "❌"} *${passed.length}/${executed} passed* · ${skipped.length} skipped · total *${duration(report.stats.duration)}*`,
      ...section(
        `❌ Failed (${failed.length})`,
        failed.map((scenario) => line(scenario)),
      ),
      ...section(
        `🎲 Flaky (${flaky.length})`,
        flaky.map((scenario) => line(scenario)),
      ),
      ...section(
        "❌ Errors outside a scenario",
        report.errors.map(
          ({ message }) => `• ${escaped(shortened(message ?? "", NAME_LIMIT))}`,
        ),
      ),
      ...section(
        `⏭️ Skipped (${skipped.length})`,
        skipped.map((scenario) => line(scenario, scenario.skippedBecause)),
      ),
      ...section(
        `🐌 Slowest ${slowest.length}`,
        slowest.map((scenario) =>
          line(scenario, duration(scenario.milliseconds)),
        ),
      ),
    ].join("\n"),
    SUMMARY_LIMIT,
  );
}
