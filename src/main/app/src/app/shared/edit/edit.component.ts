/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  HostBinding,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from "@angular/core";
import { FormControlStatus, FormGroup } from "@angular/forms";
import { Subscription } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { MaterialFormBuilderService } from "../material-form-builder/material-form-builder.service";
import { AbstractFormField } from "../material-form-builder/model/abstract-form-field";
import { StaticTextComponent } from "../static-text/static-text.component";

@Component({
  template: "",
  styleUrls: [
    "../static-text/static-text.component.less",
    "./edit.component.less",
  ],
})
export abstract class EditComponent
  extends StaticTextComponent
  implements OnInit, OnDestroy
{
  editing = false;
  @HostBinding("class.zac-is-invalid") isInValid = false;

  @Input() readonly = false;
  @Input() abstract formField: AbstractFormField;
  @Output() saveField = new EventEmitter<Record<string, unknown>>();

  formFields = new FormGroup({});
  subscription: Subscription | null = null;

  protected constructor(
    protected mfbService: MaterialFormBuilderService,
    protected utilService: UtilService,
  ) {
    super();
  }

  ngOnInit(): void {
    super.ngOnInit();
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  onOutsideClick() {
    if (this.editing && this.formFields.dirty) {
      this.save();
    } else {
      this.cancel();
    }
  }

  edit(): void {
    if (!this.readonly && !this.utilService.hasEditOverlay()) {
      this.editing = true;

      this.formFields = new FormGroup({});
      this.formFields.setControl(this.formField.id, this.formField.formControl);

      this.subscription = this.formFields.statusChanges.subscribe(
        (status: FormControlStatus) => {
          this.isInValid = Boolean(
            this.formFields.get(this.formField.id)?.dirty && status !== "VALID",
          );
        },
      );
    }
  }

  reset(): void {
    this.formFields.reset();
    this.editing = false;
  }

  save(): void {
    if (this.editing && this.formFields.dirty) {
      // Wait for an async validator if it is present.
      if (this.formField.formControl.pending) {
        const sub = this.formField.formControl.statusChanges.subscribe(() => {
          if (this.formField.formControl.valid) {
            this.submitSave();
          }
          sub.unsubscribe();
        });
      } else {
        this.submitSave();
      }
    } else {
      this.cancel();
    }
  }

  private submitSave(): void {
    const values = Object.keys(this.formFields.controls).reduce(
      (result, key) => {
        result[key] = this.formFields.get(key)?.value;
        return result;
      },
      {} as Record<string, unknown> satisfies Record<string, unknown>,
    );

    this.saveField.emit(values);

    this.reset();
  }

  cancel(): void {
    this.reset();
  }
}
