/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
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
  @Input() options: FormioOptions;
  @Input() readOnly: boolean;
  @Output() formSubmit = new EventEmitter<object>();
  @Output() formChange = new EventEmitter<{ data: unknown }>();
  @Output() createDocument = new EventEmitter<FormioCustomEvent>();

  @ViewChild(FormioComponent, { static: false })
  formioComponent!: FormioComponent;

  onSubmit(event: object) {
    this.formSubmit.emit(event);
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
  data: Record<string, object>;
  event?: Event;
}
