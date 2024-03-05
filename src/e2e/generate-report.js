const reporter = require("cucumber-html-reporter");

const options = {
  theme: "bootstrap",
  jsonFile: "reports/e2e-report.json",
  output: "reports/e2e-report.html",
  reportSuiteAsScenarios: true,
  scenarioTimestamp: true,
  launchReport: true,
  metadata: {
    App: "Dimpact e2e test",
    "Test Environment": "Test",
    // more metadata fields as needed
  },
};

reporter.generate(options);
