/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewEncapsulation,
} from "@angular/core";
import { FormioOptions } from "@formio/angular";

@Component({
  selector: "zac-formio-wrapper",
  templateUrl: "./formio-wrapper.component.html",
  styleUrl: "./formio-wrapper.component.less",
  encapsulation: ViewEncapsulation.ShadowDom,
})
export class FormioWrapperComponent {
  @Input() form: any;
  @Input() submission: any;
  @Input() options: FormioOptions;
  @Input() readOnly: boolean;
  @Output() submit = new EventEmitter<any>();
  @Output() change = new EventEmitter<any>();

  onSubmit(event: any) {
    this.submit.emit(event);
  }

  onChange(event: any) {
    this.change.emit(event);
  }
}
