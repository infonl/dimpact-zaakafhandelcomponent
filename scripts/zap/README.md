
# ZA Proxy (ZAP) setup
```text
SPDX-FileCopyrightText: 2024 INFO.nl
SPDX-License-Identifier: EUPL-1.2+
```


The [zap-docker-full-scan.sh](zap-docker-full-scan.sh) script can be used to run a full scan on a site and creates a 
report on the found issues.

## Config

There is a [default config file](default.config) setup in this directory that has the standard rules levels defined.

## Context

There is a [sample context file](sample.context) provided, that you should update with references to the locations of 
the ZAC instance (replace `my-zac.url` with the actual location), and the Keycloak instance (replace `my-keycloak.url` 
with the actual location).

### Users

In the context file, we can find entries for users, such as:
```xml
<user>user-137</user>
```
Users are available for ZAP to login to the application, and run more checks in the application in the context of a user.

We can deal with these users in two ways:
1. either we remove them, and references to these users, completely
2. or we have to use the ZAP app to create valid user entries that can be used to login to the application.

Please refer to the ZAP documentation how you can add users in a context.
Unfortunately there is (currently) no way to add users entries in any other way.

## Script

The [zap-docker-full-scan.sh](zap-docker-full-scan.sh) script will use the provided config and context files to run a
scan on the website provided on the command line.

This script will run through the following steps:
 - Check that the given website is valid and accessible.
 - Sets the working directory to the project root.
 - Verify that the config and context files are available.
 - Creates the reporting output location (./build/reports/zap).
 - Execute the ZAP full scan in the docker container.
 - Html report will be created in the output location.

When a validation fails, the script should give you enough information to be able to fix it.

Sample command:
```sh
./zap-docker-full-scan.sh https://zaakafhandelcomponent-zac-dev.dimpact.lifely.nl/
```
