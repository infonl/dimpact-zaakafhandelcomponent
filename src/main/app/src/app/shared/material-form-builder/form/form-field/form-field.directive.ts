/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Directive, ViewContainerRef } from "@angular/core";

@Directive({
  selector: "[mfb-form-field]",
  standalone: false,
})
export class FormFieldDirective {
  constructor(public viewContainerRef: ViewContainerRef) {}
}
