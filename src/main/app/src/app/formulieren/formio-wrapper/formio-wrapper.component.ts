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
import { MatDrawer } from "@angular/material/sidenav";
import { FormioComponent, FormioOptions } from "@formio/angular";

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
  @Output() formSubmit = new EventEmitter<any>();
  @Output() formChange = new EventEmitter<any>();

  @Output() openDocumentCreateDrawerEvent = new EventEmitter<string>();

  @ViewChild(FormioComponent, { static: false })
  formioComponent!: FormioComponent;

  ngAfterViewInit() {
    setTimeout(() => {
      if (this.formioComponent && this.formioComponent.formio) {
        const formioElement = this.formioComponent.formio.element;

        // Select button by 'name' attribute (alternative: use 'ref' if needed)
        const button = formioElement.querySelector(
          'button[name="data[openDrawer]"]',
        );

        if (button) {
          button.addEventListener("click", () => {
            console.log("Button clicked!");
            this.openDocumentCreateDrawerEvent.emit("openDrawer");
          });
        }
      }
    }, 1000);
  }

  onSubmit(event: any) {
    this.formSubmit.emit(event);
  }

  onChange(event: any) {
    // Filter out form.io change events that do not contain data
    console.log("custom event", event);
    if (event.data) this.formChange.emit(event);
  }
}
