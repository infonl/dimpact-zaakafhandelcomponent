/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

const { TestEnvironment } = require("jest-environment-jsdom");

/**
 * Runs a spec file in the time zone of its `@browser-time-zone` pragma instead of the Europe/Amsterdam that
 * globalJest.js sets. A spec cannot change the zone itself: Jest gives it a copy of process.env, and only the
 * process.env of the worker changes the zone of Date. The worker runs one spec file at a time, so the zone is
 * restored when the file is done.
 */
module.exports = class BrowserTimeZoneEnvironment extends TestEnvironment {
  constructor(config, context) {
    super(config, context);
    this.browserTimeZone = context.docblockPragmas["browser-time-zone"];
  }

  async setup() {
    this.workerTimeZone = process.env.TZ;
    if (this.browserTimeZone) process.env.TZ = this.browserTimeZone;
    await super.setup();
  }

  async teardown() {
    process.env.TZ = this.workerTimeZone;
    await super.teardown();
  }
};
