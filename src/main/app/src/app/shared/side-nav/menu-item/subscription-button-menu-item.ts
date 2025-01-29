/*
 * SPDX-FileCopyrightText: 2024-2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { SideNavAction } from "../side-nav-action";
import { ButtonMenuItem } from "./button-menu-item";

export class AsyncButtonMenuItem extends ButtonMenuItem {
  constructor(
    readonly title: string,
    readonly fn: () => Observable<void>,
    readonly icon?: string,
    readonly action?: SideNavAction,
  ) {
    super(title, fn, icon, action);
  }
}
