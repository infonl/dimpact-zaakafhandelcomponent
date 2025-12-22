/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { readFile, writeFile } from 'fs/promises';
import { existsSync } from 'fs';

const REPORT_PATH = 'reports/e2e-report.json';
const METRICS_OUTPUT = 'reports/e2e-metrics.json';

/**
 * Extracts metrics from the Cucumber JSON report generated during e2e test runs.
 * This script does not impact the e2e test execution itself.
 */
async function extractMetrics() {
  if (!existsSync(REPORT_PATH)) {
    console.error(`Report not found at ${REPORT_PATH}`);
    console.error('Make sure to run e2e tests first: npm run e2e:start');
    process.exit(1);
  }

  console.log('Extracting metrics from e2e test report...');

  const report = JSON.parse(await readFile(REPORT_PATH, 'utf8'));

  const metrics = {
    summary: {
      totalFeatures: report.length,
      totalScenarios: 0,
      passedScenarios: 0,
      failedScenarios: 0,
      skippedScenarios: 0,
      totalSteps: 0,
      passedSteps: 0,
      failedSteps: 0,
      skippedSteps: 0,
      totalDuration: 0,
      totalDurationFormatted: '',
      averageScenarioDuration: '',
    },
    features: [],
    slowestScenarios: [],
    failedScenarios: [],
  };

  for (const feature of report) {
    const featureMetrics = {
      name: feature.name,
      uri: feature.uri,
      scenarios: [],
      totalDuration: 0,
      passedScenarios: 0,
      failedScenarios: 0,
      skippedScenarios: 0,
    };

    for (const scenario of feature.elements) {
      metrics.summary.totalScenarios++;

      // Calculate scenario duration by summing step durations
      const duration = scenario.steps.reduce((sum, step) => {
        return sum + (step.result.duration || 0);
      }, 0);

      // Determine scenario status
      const hasFailedStep = scenario.steps.some(s => s.result.status === 'failed');
      const hasSkippedStep = scenario.steps.some(s => s.result.status === 'skipped');
      const allPassed = scenario.steps.every(s => s.result.status === 'passed');

      let status;
      if (hasFailedStep) {
        status = 'failed';
        metrics.summary.failedScenarios++;
        featureMetrics.failedScenarios++;
      } else if (hasSkippedStep) {
        status = 'skipped';
        metrics.summary.skippedScenarios++;
        featureMetrics.skippedScenarios++;
      } else if (allPassed) {
        status = 'passed';
        metrics.summary.passedScenarios++;
        featureMetrics.passedScenarios++;
      } else {
        status = 'unknown';
      }

      metrics.summary.totalDuration += duration;
      featureMetrics.totalDuration += duration;

      // Count steps
      scenario.steps.forEach(step => {
        metrics.summary.totalSteps++;
        if (step.result.status === 'passed') metrics.summary.passedSteps++;
        else if (step.result.status === 'failed') metrics.summary.failedSteps++;
        else if (step.result.status === 'skipped') metrics.summary.skippedSteps++;
      });

      const scenarioData = {
        name: scenario.name,
        type: scenario.type,
        status,
        duration: duration,
        durationFormatted: formatDuration(duration),
        steps: scenario.steps.length,
        tags: scenario.tags?.map(t => t.name) || [],
      };

      featureMetrics.scenarios.push(scenarioData);

      // Track slowest scenarios
      metrics.slowestScenarios.push({
        feature: feature.name,
        scenario: scenario.name,
        duration: duration,
        durationFormatted: formatDuration(duration),
      });

      // Track failed scenarios with error details
      if (status === 'failed') {
        const failedStep = scenario.steps.find(s => s.result.status === 'failed');
        metrics.failedScenarios.push({
          feature: feature.name,
          scenario: scenario.name,
          step: failedStep?.name || 'Unknown',
          error: failedStep?.result.error_message?.split('\n')[0] || 'No error message',
          duration: formatDuration(duration),
        });
      }
    }

    featureMetrics.totalDurationFormatted = formatDuration(featureMetrics.totalDuration);
    metrics.features.push(featureMetrics);
  }

  // Sort slowest scenarios
  metrics.slowestScenarios.sort((a, b) => b.duration - a.duration);
  metrics.slowestScenarios = metrics.slowestScenarios.slice(0, 10);

  // Calculate summary statistics
  metrics.summary.totalDurationFormatted = formatDuration(metrics.summary.totalDuration);

  if (metrics.summary.totalScenarios > 0) {
    metrics.summary.averageScenarioDuration = formatDuration(
      metrics.summary.totalDuration / metrics.summary.totalScenarios
    );
    metrics.summary.passRate = (
      (metrics.summary.passedScenarios / metrics.summary.totalScenarios) * 100
    ).toFixed(2) + '%';
  } else {
    metrics.summary.averageScenarioDuration = formatDuration(0);
    metrics.summary.passRate = '0.00%';
  }
  // Add timestamp
  metrics.timestamp = new Date().toISOString();

  // Write metrics to file
  await writeFile(METRICS_OUTPUT, JSON.stringify(metrics, null, 2));

  // Console output
  console.log('\n📈 E2E Test Metrics Summary:');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`Features:          ${metrics.summary.totalFeatures}`);
  console.log(`Scenarios:         ${metrics.summary.totalScenarios} (✅ ${metrics.summary.passedScenarios}, ❌ ${metrics.summary.failedScenarios}, ⊘ ${metrics.summary.skippedScenarios})`);
  console.log(`Steps:             ${metrics.summary.totalSteps} (✅ ${metrics.summary.passedSteps}, ❌ ${metrics.summary.failedSteps}, ⊘ ${metrics.summary.skippedSteps})`);
  console.log(`Pass Rate:         ${metrics.summary.passRate}`);
  console.log(`Total Duration:    ${metrics.summary.totalDurationFormatted}`);
  console.log(`Avg Scenario:      ${metrics.summary.averageScenarioDuration}`);

  if (metrics.failedScenarios.length > 0) {
    console.log('\n❌ Failed Scenarios:');
    console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
    metrics.failedScenarios.forEach((failure, idx) => {
      console.log(`${idx + 1}. ${failure.feature} → ${failure.scenario}`);
      console.log(`   Step: ${failure.step}`);
      console.log(`   Error: ${failure.error}`);
    });
  }

  console.log('\n🐌 Top 5 Slowest Scenarios:');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  metrics.slowestScenarios.slice(0, 5).forEach((scenario, idx) => {
    console.log(`${idx + 1}. ${scenario.durationFormatted.padEnd(10)} - ${scenario.feature} → ${scenario.scenario}`);
  });

  console.log(`\n✅ Metrics saved to ${METRICS_OUTPUT}`);
}

/**
 * Formats duration from nanoseconds to human-readable format
 * @param {number} nanoseconds - Duration in nanoseconds
 * @returns {string} Formatted duration string
 */
function formatDuration(nanoseconds) {
  if (!nanoseconds) return '0s';

  const seconds = nanoseconds / 1_000_000_000;

  if (seconds < 1) {
    return `${(nanoseconds / 1_000_000).toFixed(0)}ms`;
  } else if (seconds < 60) {
    return `${seconds.toFixed(2)}s`;
  } else {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.floor(seconds % 60);
    return `${minutes}m ${remainingSeconds}s`;
  }
}

// Run the extraction
extractMetrics().catch(err => {
  console.error('❌ Error extracting metrics:', err);
  process.exit(1);
});
