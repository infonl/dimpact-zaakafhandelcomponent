# Updating dependencies

This document describes how to update dependencies in ZAC and (OpenAPI) API specifications used by ZAC.

## Updating dependencies in ZAC

This is normally done by GitHub Dependabot using the Dependabot workflows in the project.
This includes updating of backend (Java), frontend (TypeScript) as well as GitHub Action and Docker dependencies.

## Updating (OpenAPI) API specifications used by ZAC

ZAC integrates with various components and external services using APIs.
Most of these APIs are defined by OpenAPI specifications.
These [OpenAPI](https://www.openapis.org/) specifications are used in the ZAC build process to automatically generate ZAC client code
(with a few exceptions).
The OpenAPI specifications that are used for this client code generation can be found
in the [api-secs](../../src/main/resources/api-specs) folder.

To update these OpenAPI specification, follow these steps:

1. Obtain the version (YAML file) of the OpenAPI specification in question.
   1. For the ZGW APIs you can find these specifications from a running OpenZaak instance
by following the `Components` links on the OpenZaak homepage.
   2. Now follow the `API specificaties` link.
   3. Now manually append the string `openapi.yaml` to the URL in the browser. The URL will now be
something like: `https://<open-zaak-url>/zaken/api/v1/schema/openapi.yaml` and press enter.
   4. A download of the OpenAPI specification will start.
2. Replace the existing OpenAPI specification in the [api-secs](../../src/main/resources/api-specs) folder with the new version.
3. Do a diff of the newly generated OpenAPI specification with the current version and make sure that
any manual changes that were made to the current version are also made to the new version (if they still apply).
These manual changes are indicated with a code comment containing the term `Lifely`.
Some of these manual changes include:
   1. Added `readOnly: false` attributes to properties that were missing a setter (or constructor) in the generated
Java client code but which need to be set by ZAC.
   2. Added empty values to certain enumerations to allow for empty (string) values in the HTTP responses
of certain OpenZaak requests.
4. Regenerate the Java client code. This can be done by running the following Gradle task:
   ```shell
   ./gradlew clean generateJavaClients
   ```
   This will generate the Java client code in the [src/generated/java](../../src/generated/java) folder.
5. Check if ZAC still compiles and runs correctly and all automated tests succeed.
6. Manually test the functionality that uses the updated API where automated test coverage is lacking.

## Updating fonts and icons

ZAC uses the [Material Design Icons](https://materialdesignicons.com/) and [Google Fonts](https://fonts.google.com/) libraries, but we host them locally to prevent external dependencies.

The css of all icons and fonts is located in [src/main/app/src/styles.less](../../src/main/app/src/styles.less) file.
All assets are located in the [src/main/app/src/assets](../../src/main/app/src/assets) folder.

When updating the icons or fonts, make sure to update the css and assets accordingly. We only load in the fonts we actually need so we are not sending unnecessary data to the client.

If you need a new font-weight for example, you can find the css and assets for the roboto font here [https://fonts.googleapis.com/css?family=Roboto](https://fonts.googleapis.com/css?family=Roboto), then you can copy paste the css and download the assets and add them to the project.

