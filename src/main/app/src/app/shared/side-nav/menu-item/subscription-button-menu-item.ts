/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Observable } from "rxjs";
import { ButtonMenuItem } from "./button-menu-item";

export class AsyncButtonMenuItem extends ButtonMenuItem {
  constructor(
    readonly title: string,
    readonly fn: () => Observable<void>,
    readonly icon?: string,
  ) {
    super(title, fn, icon);
  }
}
