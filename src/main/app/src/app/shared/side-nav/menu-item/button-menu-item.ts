/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MenuItem, MenuItemType } from "./menu-item";

export class ButtonMenuItem extends MenuItem {
  readonly type: MenuItemType = MenuItemType.BUTTON;

  constructor(
    readonly title: string,
    readonly fn: () => void,
    readonly icon?: string,
  ) {
    super();
  }
}
