/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  booleanAttribute,
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  Output,
  ViewChild,
  ViewEncapsulation,
} from "@angular/core";
import {
  ExtendedComponentSchema,
  FormioComponent,
  FormioHookOptions,
} from "@formio/angular";

@Component({
  selector: "zac-formio-wrapper",
  templateUrl: "./formio-wrapper.component.html",
  styleUrl: "./formio-wrapper.component.less",
  encapsulation: ViewEncapsulation.ShadowDom,
  standalone: false,
})
export class FormioWrapperComponent implements AfterViewInit {
  @Input() form: unknown;
  @Input() submission: unknown;
  @Input() options?: FormioHookOptions;
  @Input({ required: true, transform: booleanAttribute }) readOnly = false;
  @Output() formSubmit = new EventEmitter<object>();
  @Output() formChange = new EventEmitter<{ data: unknown }>();
  @Output() createDocument = new EventEmitter<FormioCustomEvent>();
  @Output() submissionDone = new EventEmitter<boolean>();

  @ViewChild(FormioComponent, { static: false })
  formioComponent!: FormioComponent;

  private elementRef = inject(ElementRef);
  private static bootstrapStyleSheet: CSSStyleSheet | null = null;

  constructor() {}

  async ngAfterViewInit() {
    await this.loadBootstrapStyles();
  }

  private async loadBootstrapStyles(): Promise<void> {
    const shadowRoot = this.elementRef.nativeElement.shadowRoot as ShadowRoot;
    if (!shadowRoot) return;

    if (FormioWrapperComponent.bootstrapStyleSheet) {
      // Use cached stylesheet
      shadowRoot.adoptedStyleSheets = [
        FormioWrapperComponent.bootstrapStyleSheet,
        ...shadowRoot.adoptedStyleSheets,
      ];
      return;
    }

    try {
      const response = await fetch(
        "/assets/vendor/bootstrap/bootstrap.min.css",
      );
      let css = await response.text();
      css = css.replace(/:root\b/g, ":host");

      const sheet = new CSSStyleSheet();
      await sheet.replace(css);
      FormioWrapperComponent.bootstrapStyleSheet = sheet;

      shadowRoot.adoptedStyleSheets = [sheet, ...shadowRoot.adoptedStyleSheets];
    } catch (error) {
      console.error("Failed to load Bootstrap CSS:", error);
    }
  }

  onSubmit(event: object) {
    this.formSubmit.emit(event);
    this.submissionDone.emit(true);
  }

  onChange(event: object) {
    // Filter out form.io change events that do not contain data
    if ("data" in event && event.data) this.formChange.emit(event);
  }

  onCustomEvent(event: FormioCustomEvent) {
    if (event.type === "createDocument") {
      // Emit to parent
      this.createDocument.emit(event);
    }
  }
}

export interface FormioCustomEvent {
  type: string;
  component: ExtendedComponentSchema;
  data: Record<string, string>;
  event?: Event;
}

export interface FormioChangeEvent {
  data: Record<string, string>;
}
