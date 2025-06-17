#!/bin/sh

#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

set -e

echo "Change Directory to e2e test directory .."
cd ./src/e2e

if [ "$SKIP_INSTALL" != "true" ]; then
  echo "Install dependencies ..."
  npm ci

  echo "Install Playwright Browsers ..."
  npx playwright install --with-deps
fi

echo "Cleanup screenshots ..."
rm -rf reports/*

echo "Define world-parameters JSON ..."
world_params='{"urls": { "zac": "'$ZAC_URL'", "openForms": "'$OPEN_FORMS_URL'"}, "headless": '${HEADLESS:-false}', "users": {"Bob": {"username": "'$E2E_TEST_USER_1_USERNAME'", "password": "'$E2E_TEST_USER_1_PASSWORD'"}, "Oscar": {"username": "'$E2E_TEST_USER_2_USERNAME'", "password": "'$E2E_TEST_USER_2_PASSWORD'"}}}'

echo "Run your Playwright tests ..."
if [ "$EXCLUDE_LIVE_SCENARIO_TAGS" = "true" ]; then
    npm run e2e:run -- --world-parameters "$world_params" --tags "not @live-env-only"
else
    npm run e2e:run -- --world-parameters "$world_params"
fi
