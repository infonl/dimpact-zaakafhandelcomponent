#!/bin/bash

# Set Environment Variables
export APP_ENV=devlocal

# Change Directory to your test directory
cd ./src/e2e
# Install dependencies
npm ci

# Install Playwright Browsers
npx playwright install --with-deps

# Define world-parameters JSON
world_params='{"urls": { "zac": "'$ZAC_URL'"}, "headless": false, "users": {"Bob": {"username": "'$TEST_USER_1_USERNAME'", "password": "'$TEST_USER_1_PASSWORD'"}}}'

# Run your Playwright tests
npm run e2e:run -- --world-parameters "$world_params"