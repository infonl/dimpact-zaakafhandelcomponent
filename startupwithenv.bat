@REM #
@REM # SPDX-FileCopyrightText: 2023 Lifely
@REM # SPDX-License-Identifier: EUPL-1.2+
@REM #

@REM # Uses the 1Password CLI tools to set up the environment variables for running ZAC in IntelliJ.
@REM # Please see docs/development/INSTALL.md for details on how to use this script.

@REM # Note that we do not use masking as this does not work well in our context and will result in
@REM # crashes of WildFly/ZAC when running in IntelliJ when you have configured DEBUG logging in WildFly
@REM # with errors such as: "fatal error: concurrent map read and map write goroutine 2013 [running]:
@REM # go.1password.io/op/op-cli/command/subprocess/masking.matches.add(...)"
op run --env-file=".env.tpl" --no-masking -- .\wildfly-31.0.1.Final\bin\standalone.bat
