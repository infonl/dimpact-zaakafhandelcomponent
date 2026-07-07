/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Directive, ElementRef, inject, input } from "@angular/core";

@Directive({ selector: "[referentieTabelRow]", standalone: true })
export class ReferentieTabelRowDirective {
  readonly tabelId = input.required<number | null | undefined>({
    alias: "referentieTabelRow",
  });
  readonly element = inject(ElementRef<HTMLElement>);
}
