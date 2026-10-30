/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { signal } from "@angular/core";

export abstract class MenuItem {
  abstract readonly type: MenuItemType;
  abstract readonly title: string;
  abstract readonly icon?: string;
  activated = false;

  // Backed by a signal, so the OnPush side nav shows a change that code makes after a click.
  private readonly isDisabled = signal(false);

  get disabled() {
    return this.isDisabled();
  }

  set disabled(disabled: boolean) {
    this.isDisabled.set(disabled);
  }
}

export enum MenuItemType {
  HEADER = "HEADER",
  LINK = "LINK",
  HREF = "HREF",
  BUTTON = "BUTTON",
}
