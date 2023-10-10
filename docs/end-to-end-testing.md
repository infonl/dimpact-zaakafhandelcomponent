# Cucumber

We use cucumber to write reusable tests in human readable format (Gherkin) for e2e tests.

## Running tests

First make sure to install all the dependencies by running the following command in the src/main/e2e folder:

```npm install```

To run the tests you can use the following command in the src/main/e2e folder:

```npm run e2e```

## Writing tests

We have predefined steps that you can use to write tests. You can find them in the [src/main/e2e/step-definitions](src/main/e2e/step-definitions) folder. each file in this folder represents a specific domain, like "zaak" is meant for non reusables steps that are specific to the "zaak" domain. steps in common are meant to be reusable across domains.
![Alt text](/docs//img/cucumber-example.png)

In a .feature file you should be able to write out tests based on the predefined steps with auto complete.
![Alt text](/docs/img/cucumber-auto-complete.png)



### Writing cucumber tests in intellij

You need to make sure to install the [cucumber.js](https://plugins.jetbrains.com/plugin/7418-cucumber-js) plugin.

Then you will have all the autocomplete features available to you

### Writing cucumber tests in vscode

You need to make sure to install the official [cucumber](https://marketplace.visualstudio.com/items?itemName=CucumberOpen.cucumber-official) plugin.

Then you will have all the autocomplete features available to you

