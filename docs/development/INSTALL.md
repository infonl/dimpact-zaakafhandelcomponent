# Installation

These instructions describe how to build, install and run the `zaakafhandelcomponent (ZAC)` software _for developers_.
General ZAC usage instructions may be found in the [README.md](../../README.md) file.

## Build the software

### Prerequisites

- Java JDK 21 (we use the `Temurin` distribution)

### Gradle build

The software is built using Gradle and for the final step using Maven.
Both a Gradle and a Maven wrapper are included in the source code, so you do not need to install either Gradle or Maven yourself.

This builds all the software, including the Java backend as well as the TypeScript frontend (using `npm`), runs all unit tests
and packages the built software first into a WAR archive and then finally by invoking a Maven command from Gradle into a
[WildFly application server](https://www.wildfly.org/) bootable fat-JAR. This last step uses [Galleon](https://docs.wildfly.org/galleon/).

All generated artifacts by Gradle are placed into the `build` folder while the final WildFly bootable JAR is placed in the
`target` folder. The reason why we use Maven for this last step is because there is unfortunately no Gradle alternative for the
[WildFly Maven Plugin](https://docs.wildfly.org/wildfly-maven-plugin).

If you want to skip running the unit tests, use the following command:

```shell
./gradlew build -x test
```

### Build the Docker image

To build the ZAC Docker image using the generated JAR archive from the previous step, use the following command:

```shell
./gradlew buildDockerImage
```

## Run the software

There are various ways to run ZAC locally.
- For local backend development purposes we recommend to run ZAC from the IntelliJ IDE.
- For local frontend development and testing purposes we recommend to run ZAC in a Docker container.
- For local e2e testing you can follow the [end-to-end-testing](testing.md#end-to-end-e2e-tests) from the [testing](testing.md) documentation.

### Prerequisites

- Access to all services (such as Keycloak, Open Zaak, etc) that are required by ZAC.
You either run these locally (using [Docker Compose](installDockerCompose.md)) or on a central development environment.
- Environment variables required by ZAC. See the section below.

#### Environment variables

ZAC requires a number of environment variables to be set. These are documented in the [.env.example](../../.env.example) file.

These can be set in various ways. The options are:
-   1Password: Use the [1Password CLI extensions](https://developer.1password.com/docs/cli/).
-   Env file: Create an `.env` file based on the [.env.example](../../.env.example) example and use the https://github.com/Ashald/EnvFile IntelliJ plugin to read all required environment variables from your local `.env` file.
-   Or alternatively, when running ZAC in IntelliJ, in `Startup/Connection` add all required ZAC environment variables to the `Debug` configuration.

We prefer to use the 1Password CLI extensions as it is the most secure and enables you to
centrally manage these variables for all developers.
It requires you to have configured all environment variables in a 1Password Vault shared with all developers.
In the section below we describe how to use the 1Password CLI extensions when running ZAC from IntelliJ.

### Run ZAC in IDE

Check the instructions in [IDE Configuration](./ideConfig.md#run-zac-in-intellij)

### Run ZAC in a Docker container

As an alternative to running ZAC in IntelliJ you can also run ZAC in a Docker container.
There are several ways to do this.

#### Run ZAC in a Docker container using Docker Compose

If you also wish to run all services that are required by
ZAC locally, the easiest way is to use our Docker Compose setup with can also run ZAC.
Please see the [Docker Compose instructions](installDockerCompose.md) for more information.

#### Run ZAC in a Docker container by itself

As an alternative to run ZAC as a Docker container by itself you use the following command:

```shell
docker run -p 8080:8080 --env-file .env --name zaakafhandelcomponent zaakafhandelcomponent:latest
```

Or run one of the official ZAC Docker images from the [ZAC GitHub Packages Container Registry](https://github.com/infonl/dimpact-zaakafhandelcomponent/pkgs/container/zaakafhandelcomponent):

```shell
docker run -p 8080:8080 --env-file .env --name zaakafhandelcomponent ghcr.io/infonl/zaakafhandelcomponent:main-75
```

Be aware that you will need to set the ZAC environment variables according to your needs.

## Miscellaneous

### Generating Redoc documentation for the ZAC backend API

In order to generate API documentation for the ZAC backend API, use the following command:

```shell
./gradlew generateZacApiDocs
```

The ZAC API documentation is generated in the `build/generated/zac-api-docs` folder.

Note that this task is not run automatically as part of `./gradlew build`.
