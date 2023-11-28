# Testing ZAC

## Unit tests

Both backend and frontend unit tests are run as part of the `test` phase in the normal Gradle build.
You can run them separately using the following command:

```shell
./gradlew test --info
```

## Integration tests

Our integration tests use the [TestContainers framework](https://testcontainers.com/) together
with our [Docker Compose set-up](installDockerCompose.md) to run all required services (Keycloak, Open Zaak, etc)
as well as ZAC itself as a Docker container.
This set-up makes it relatively slow to run the integration tests and for this reason they are not run as part of
the standard Gradle `test` phase and normal Gradle build.

If you wish to run the integration tests you can use the following command:

```shell
./gradlew itest --info
```

It is also possible to run the integration tests from inside your IDE (we use IntelliJ IDEA).
To do this you will first need to do the following:

1. Start Docker.
2. Build the ZAC Docker image using the following command:
    ```shell
    ./gradlew buildZacDockerImage
    ```
3. Create a 'run configuration' in your IDE where the following two environment variables are set: `BAG_API_CLIENT_MP_REST_URL` and `BAG_API_KEY`.
4. Run the integration tests from your IDE using this run configuration.

Running the integration tests will first start up all required services (Keycloak, Open Zaak, etc) as Docker containers using our [Docker Compose file](installDockerCompose.md),
then start up ZAC as Docker container and finally run the integration tests.

## End-to-end (e2e) tests

### Cucumber

We use cucumber to write reusable tests in human readable format (Gherkin) for e2e tests.

### Running the e2e tests

First make sure to install all the dependencies by running the following command in the src/main/e2e folder:

```npm install```

To run the tests you can use the following command in the src/main/e2e folder:

```npm run e2e```

### Writing e2e tests

We have predefined steps that you can use to write tests. You can find them in the [src/main/e2e/step-definitions](../../src/e2e/step-definitions) folder. each file in this folder represents a specific domain, like "zaak" is meant for non reusables steps that are specific to the "zaak" domain. steps in common are meant to be reusable across domains.
![Alt text](./attachments/images/cucumber-example.png)

In a .feature file you should be able to write out tests based on the predefined steps with auto complete.
![Alt text](./attachments/images/cucumber-auto-complete.png)

#### Writing cucumber tests in intellij

You need to make sure to install the [cucumber.js](https://plugins.jetbrains.com/plugin/7418-cucumber-js) plugin.

Then you will have all the autocomplete features available to you

#### Writing cucumber tests in vscode

You need to make sure to install the official [cucumber](https://marketplace.visualstudio.com/items?itemName=CucumberOpen.cucumber-official) plugin.

Then you will have all the autocomplete features available to you

### Running e2e tests locally

Running e2e tests locally unfortunately requires some extra steps to make it work with our current setup. This is because docker containers can't communicate with the host machine using localhost. To make this work we need to add an entry to the /etc/hosts file on your machine. This is only needed when running the tests locally. When running the tests in the pipeline this is not needed.

#### Steps to Add host.docker.internal Entry to /etc/hosts File
When working with Docker, adding host.docker.internal to your /etc/hosts file allows Docker containers to access services running on the host machine. Follow these steps to add this entry:

1. Open the /etc/hosts File:
    - You need administrative privileges to edit the /etc/hosts file.
    - Open the file in a text editor of your choice. For example, using vim, you would use the following command:
    ```bash
        sudo vim /etc/hosts
    ```
2. Add the host.docker.internal Entry:
    - In the /etc/hosts file, add a new line to link host.docker.internal to the IP address of your host machine. This is typically 127.0.0.1 (localhost).
    - The entry should look like this:
    ```csharp
        # ZAC
        127.0.0.1 host.docker.internal
    ```
    - Save the file and exit the text editor.
3. Verify the Entry:

    - After adding the entry, you can verify it by running a command that references host.docker.internal from within a Docker container. For example, using a simple ping test:
    ```bash
        docker run --rm alpine ping -c 4 host.docker.internal
    ```
This command runs a temporary Alpine Linux container and pings host.docker.internal four times. Successful ping responses indicate that the entry is correctly configured.

#### Steps to run the tests locally

`docker-compose.yml`
- replace the follwoing values in the `docker-compose.yml` file:

under services -> keycloack -> command
```diff
-      - CONTEXT_URL=http://host.docker.internal:8080
+      - CONTEXT_URL=http://host.docker.internal:8080
```

under services -> zac -> environment
```diff
-      - "--hostname-url=http://host.docker.internal:8081"
+      - "--hostname-url=http://host.docker.internal:8081"
```

## Manual tests

### Using Postman

Using [Postman](https://www.postman.com/) you can test ZAC by sending HTTP requests to the ZAC backend API.
This way you can test a locally running ZAC or you can test ZAC running on e.g. the TEST environment. Be aware that by doing so you are depending on the requests you send, of course changing data and state in ZAC.

#### Set up Postman for ZAC

As a Lifely developer you can use our shared Postman ZAC collection in our [ZAC API TEST Postman team workspace](https://zaakafhandelcomponent.postman.co/workspace/aec6c5c4-affd-490b-9c81-e8b1cf339d22).
Alternatively you can set up a Postman collection yourself using the instructions below.

To use our shared Postman collection you need to be a member of our Zaakafhandelcomponent Postman team (max 3 members for the free Postman version).

##### Import ZAC OpenAPI spec

1. Make sure your locally cloned ZAC Git repo is up-to-date and you have recently run a build (see [INSTALL.md](INSTALL.md) for instructions).
2. In Postman click ‘import’ and select the generated OpenAPI YAML spec file in the ZAC Git project in the folder: `build/generated/openapi/META-INF/openapi/`
3. Select to create a new Postman Collection.

##### Create environment(s)

Create a Postman environment (e.g. “Localhost”) with the following variables:

| Variable             |        Description         |        Value for ZAC on localhost         |
|----------------------|:--------------------------:|:-----------------------------------------:|
| baseUrl              |        ZAC base URL        |           http://localhost:8080           |
| keycloakBaseUrl      |     Keycloak base URL      |           http://localhost:8081           |
| keycloakClientSecret | Keycloak ZAC client secret | keycloakZaakafhandelcomponentClientSecret |

Also see:

![postman-environment-localhost.png](./attachments/images/postman-environment-localhost.png)

When testing ZAC running on a different environment please set the values accordingly.

##### Set up OAuth2 authorization using Keycloak

Next in the collection you have created set up authorization at the top level of this collection. Use the following settings in the Authorization tab:

| Variable              |                                     Value                                      |
|-----------------------|:------------------------------------------------------------------------------:|
| Type                  |                                     Oauth2                                     |
| Add auth data to      |                                Request headers                                 |
| Header prefix         |                                     Bearer                                     |
| Auto-refresh token    |                                    enabled                                     |
| Use token type        |                                  Access token                                  |
| Token name            |                                    Keycloak                                    |
| Grant type            |                               Authorization code                               |
| Callback URL          |                                  {{baseUrl}}                                   |
| Auth URL              | {{keycloakBaseUrl}}/realms/zaakafhandelcomponent/protocol/openid-connect/auth  |
| Access Token URL      | {{keycloakBaseUrl}}/realms/zaakafhandelcomponent/protocol/openid-connect/token |
| Client ID             |                             zaakafhandelcomponent                              |
| Client secret         |                            {{keycloakClientSecret}}                            |
| Scope                 |                                     openid                                     |
| State                 |                                    (empty)                                     |
| Client authentication |                           Send as basic auth header                            |

Also see:

![postman-authorization-1.png](./attachments/images/postman-authorization-1.png)
![postman-authorization-2.png](./attachments/images/postman-authorization-2.png)

##### Generate access token

1. In the authorization tab click ‘Get New Access Token’
2. Log in to ZAC using the appropriate (test) user account.
3. Now you should be able to any send requests to ZAC defined in the Postman collection.
