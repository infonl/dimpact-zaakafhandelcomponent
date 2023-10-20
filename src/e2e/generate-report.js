const reporter = require('cucumber-html-reporter');

const options = {
    theme: 'bootstrap',
    jsonFile: 'reports/e2e-report.json',
    output: 'reports/e2e-report.html',
    reportSuiteAsScenarios: true,
    scenarioTimestamp: true,
    launchReport: true,
    metadata: {
        "App": "Your App Name",
        "Test Environment": "Your Test Environment",
        // more metadata fields as needed
    }
};

reporter.generate(options);