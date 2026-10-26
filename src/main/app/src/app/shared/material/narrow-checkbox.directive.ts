/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Directive,
  ElementRef,
  OnInit,
  Renderer2,
  inject,
} from "@angular/core";

@Directive({
  selector: "[zacNarrowMatCheckbox]",
  standalone: true,
})
export class ZacNarrowMatCheckboxDirective implements OnInit {
  private readonly el = inject(ElementRef);
  private readonly renderer = inject(Renderer2);

  ngOnInit(): void {
    this.renderer.addClass(this.el.nativeElement, "mat-narrow-checkbox");
  }
}
