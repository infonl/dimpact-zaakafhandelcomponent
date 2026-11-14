/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

export abstract class MenuItem {
  abstract readonly type: MenuItemType;
  abstract readonly title: string;
  abstract readonly icon?: string;
  activated = false;
  disabled = false;
}

export enum MenuItemType {
  HEADER = "HEADER",
  LINK = "LINK",
  HREF = "HREF",
  BUTTON = "BUTTON",
}
