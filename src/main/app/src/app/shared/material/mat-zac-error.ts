/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AfterViewInit, Directive, ElementRef, inject } from "@angular/core";

@Directive({
  selector: "[appEnhanceMatError]",
  standalone: true,
})
export class EnhanceMatErrorDirective implements AfterViewInit {
  private readonly elRef = inject(ElementRef);

  ngAfterViewInit() {
    const css = `
        text-overflow: ellipsis;
        overflow: hidden;
        white-space: nowrap;
      `;

    const matErrorElement = this.elRef.nativeElement;

    matErrorElement.title = matErrorElement.innerHTML;
    matErrorElement.setAttribute("style", css);
  }
}
