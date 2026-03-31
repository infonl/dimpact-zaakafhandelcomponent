/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Directive, ElementRef, inject, input } from "@angular/core";

@Directive({ selector: "[bpmnNodeRow]", standalone: true })
export class BpmnNodeRowDirective {
  readonly key = input.required<string>({ alias: "bpmnNodeRow" });
  readonly el = inject(ElementRef<HTMLElement>);
}
