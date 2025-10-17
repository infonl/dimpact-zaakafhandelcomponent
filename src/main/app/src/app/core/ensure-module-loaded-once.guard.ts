/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class EnsureModuleLoadedOnceGuard {
  constructor(targetModule: unknown) {
    if (!targetModule) return;

    throw new Error(
      `${targetModule.constructor.name} has already been loaded. Import this module in the AppModule only.`,
    );
  }
}
