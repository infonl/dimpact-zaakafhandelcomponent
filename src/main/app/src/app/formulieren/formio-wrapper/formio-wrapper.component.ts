/*
 * SPDX-FileCopyrightText: 2024 Dimpact
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
  private elementRef = inject(ElementRef);
  private static bootstrapStyleSheet: CSSStyleSheet | null = null;
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

  async ngAfterViewInit() {
    await this.loadBootstrapStyles();
  }

  private async loadBootstrapStyles(): Promise<void> {
    const shadowRoot = this.elementRef.nativeElement.shadowRoot as ShadowRoot;
    if (!shadowRoot) return;

    // Use cached stylesheet if available
    if (FormioWrapperComponent.bootstrapStyleSheet) {
      shadowRoot.adoptedStyleSheets = [
        FormioWrapperComponent.bootstrapStyleSheet,
        ...shadowRoot.adoptedStyleSheets,
      ];
      return;
    }

    try {
      // Lazy load Bootstrap CSS from CDN
      const response = await fetch(
        "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css",
      );
      let css = await response.text();

      // Transform :root to :host for Shadow DOM compatibility
      css = css.replace(/:root\b/g, ":host");

      // Create and cache stylesheet
      const sheet = new CSSStyleSheet();
      await sheet.replace(css);
      FormioWrapperComponent.bootstrapStyleSheet = sheet;

      // Adopt into Shadow DOM - prepend so component styles override
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
