/*
 * SPDX-FileCopyrightText: 2024 Dimpact, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  booleanAttribute,
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  OnInit,
  Output,
  ViewChild,
  ViewEncapsulation,
} from "@angular/core";
import {
  ExtendedComponentSchema,
  FormioComponent,
  FormioHookOptions,
} from "@formio/angular";
import { FormioBootstrapLoaderService } from "./formio-bootstrap-loader.service";

@Component({
  selector: "zac-formio-wrapper",
  templateUrl: "./formio-wrapper.component.html",
  styleUrl: "./formio-wrapper.component.less",
  encapsulation: ViewEncapsulation.ShadowDom,
  standalone: false,
})
export class FormioWrapperComponent implements OnInit {
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
  private bootstrapLoader = inject(FormioBootstrapLoaderService);
  protected stylesLoaded = false;

  async ngOnInit() {
    await this.loadBootstrapStyles();
    this.stylesLoaded = true;
  }

  private async loadBootstrapStyles(): Promise<void> {
    const shadowRoot = this.elementRef.nativeElement.shadowRoot as ShadowRoot;
    if (!shadowRoot) return;

    try {
      const sheet = await this.bootstrapLoader.getBootstrapStyleSheet();
      shadowRoot.adoptedStyleSheets = [sheet, ...shadowRoot.adoptedStyleSheets];
    } catch (error) {
      console.error("Failed to load Bootstrap CSS:", error);
      // Allow form to render without Bootstrap styles
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

  protected get formioOptions() {
    return {
      disableAlerts: true,
      i18n: {
        nl: {
          cancel: "Annuleren",
          next: "Volgende",
          previous: "Vorige",
          submit: "Indienen",
        },
      },
      ...this.options,
    };
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
