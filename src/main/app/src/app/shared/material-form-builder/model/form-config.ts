/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

export class FormConfig {
  partialButtonText: string | null = null;
  saveButtonText: string;
  cancelButtonText: string;

  partialButtonIcon: string;
  saveButtonIcon: string;
  cancelButtonIcon: string;
  requireUserChanges: boolean;

  constructor() {}
}
