/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  booleanAttribute,
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  ViewEncapsulation,
} from "@angular/core";
import {
  ExtendedComponentSchema,
  FormioComponent,
  FormioOptions,
} from "@formio/angular";

@Component({
  selector: "zac-formio-wrapper",
  templateUrl: "./formio-wrapper.component.html",
  styleUrl: "./formio-wrapper.component.less",
  encapsulation: ViewEncapsulation.ShadowDom,
})
export class FormioWrapperComponent {
  @Input() form: unknown;
  @Input() submission: unknown;
  @Input() options?: FormioOptions;
  @Input({ required: true, transform: booleanAttribute }) readOnly = false;
  @Output() formSubmit = new EventEmitter<object>();
  @Output() formChange = new EventEmitter<{ data: unknown }>();
  @Output() createDocument = new EventEmitter<FormioCustomEvent>();
  @Output() submissionDone = new EventEmitter<boolean>();

  @ViewChild(FormioComponent, { static: false })
  formioComponent!: FormioComponent;

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
