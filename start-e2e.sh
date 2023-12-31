#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# Change Directory to your test directory
cd ./src/e2e
# Install dependencies
npm ci

# Install Playwright Browsers
npx playwright install --with-deps

# Define world-parameters JSON
world_params='{"urls": { "zac": "'$ZAC_URL'"}, "headless": false, "users": {"Bob": {"username": "'$E2E_TEST_USER_1_USERNAME'", "password": "'$E2E_TEST_USER_1_PASSWORD'"}}}'

# Run your Playwright tests
npm run e2e:run -- --world-parameters "$world_params"
