# Installation

These instructions describe how to build, install and run the `zaakafhandelcomponent (ZAC)` software _for developers_.
General instructions may be found in the [README.md](README.md) file.

## Prerequisites

* Java 17 (we use the `Temurin` distribution)

## Build the software

### Maven build

The software is built using Maven. The Maven wrapper is included in the source code, so you do not need to install Maven.
To build the software use the following command:

```shell
./mvnw clean package
```

This will build all the software, including the Java backend as well as the TypeScript frontend (using `npm`), runs all unit tests
and packages the built software first into a WAR archive and then into a Wildfly application server bootable fat-JAR.
All generated artifacts are placed into the `target` directory.

If you want to skip running the tests, use the following command:

```shell
./mvnw clean package -DskipTests
```

### Build the Docker image

To build the ZAC Docker image using the generated JAR archive from the previous step, use the following command:

```shell
docker build -t zaakafhandelcomponent:latest --file Containerfile .
```

## Run the software

### Prerequisites

- Access to all required services by ZAC, either locally (e.g. using Docker Compose) or on a central development environment.
- A `.env` file containing all required environment variables.

More detailed instructions will follow.

### Run in a Docker container

To run ZAC in a Docker container use the following command:

```shell
docker run -p 8080:8080 --env-file .env --name zaakafhandelcomponent zaakafhandelcomponent:latest
```

Or run one of the official ZAC Docker images from the [ZAC GitHub Packages Container Registry](https://github.com/infonl/dimpact-zaakafhandelcomponent/pkgs/container/zaakafhandelcomponent):

```shell
docker run -p 8080:8080 --env-file .env --name zaakafhandelcomponent ghcr.io/infonl/zaakafhandelcomponent:main-75
```
