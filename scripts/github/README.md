[//]: # (SPDX-FileCopyrightText: 2023 Lifely
[//]: # (SPDX-License-Identifier: EUPL-1.2+)

# Testing GitHub Actions using ACT

GitHub Actions can be tested using the [ACT](https://github.com/nektos/act)
testing framework. Please refer it's documentation for details.

## Setup
To be able to run ACT, it needs to be installed. The following command can be
used to install it using HomeBrew:
```shell
brew install act
```

## Configuration
The act runner reads the local configuration from the [`.actrc`](.actrc) file in
this directory.

## Environment
The environment variables required by the GitHub workflow should be provided in an
`.env` file in this directory.

## Secrets
Secrets are by default loaded from the [`.secrets`](.secrets) file in this
directory. This file will NOT be committed into the repository to
ensure no secrets will accidentally be exposed there.

## Script
There's a script that kicks off act in the correct location:
```shell
./test-workflows.sh
```

This script can be used to test the whole flow, or you can adjust it so that it
only runs a single job.
