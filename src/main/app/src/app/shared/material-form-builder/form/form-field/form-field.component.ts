/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild,
} from "@angular/core";
import { Subscription } from "rxjs";
import { ReadonlyFormFieldBuilder } from "../../form-components/readonly/readonly-form-field-builder";
import { ReadonlyComponent } from "../../form-components/readonly/readonly.component";
import { MaterialFormBuilderService } from "../../material-form-builder.service";
import { AbstractFormField } from "../../model/abstract-form-field";
import { FormItem } from "../../model/form-item";
import { FormFieldDirective } from "./form-field.directive";

@Component({
  selector: "mfb-form-field",
  templateUrl: "./form-field.component.html",
  styleUrls: ["./form-field.component.less"],
})
export class FormFieldComponent<T = unknown> implements OnInit, AfterViewInit {
  @Input({ required: true }) field!: AbstractFormField<T>;

  @Output() valueChanges = new EventEmitter<T | null | undefined>();

  private _field!: FormItem;
  private loaded = false;
  private valueChangesSubscription?: Subscription;

  @ViewChild(FormFieldDirective) formField!: FormFieldDirective;

  constructor(private readonly mfbService: MaterialFormBuilderService) {}

  ngOnInit() {
    this._field = this.mfbService.getFormItem(this.field);
    this.refreshComponent();
  }

  ngAfterViewInit() {
    this.loadComponent();
  }

  refreshComponent() {
    if (this.loaded) {
      this.valueChangesSubscription?.unsubscribe();
      this.formField.viewContainerRef.clear();
      this.loadComponent();
    }
  }

  loadComponent() {
    if (this._field.data.readonly && !this._field.data.hasReadonlyView()) {
      this._field = new FormItem(
        ReadonlyComponent,
        new ReadonlyFormFieldBuilder(String(this._field.data.formControl.value))
          .id(this._field.data.id)
          .label(this._field.data.label)
          .build(),
      );
    }
    const componentRef = this.formField.viewContainerRef.createComponent(
      this._field.component,
    );

    // apply data and activate change detection
    componentRef.instance.data = this._field.data;
    componentRef.changeDetectorRef.detectChanges();
    this.loaded = true;
    if (this._field.data.hasFormControl()) {
      this.valueChangesSubscription =
        this._field.data.formControl.valueChanges.subscribe((value) => {
          this.valueChanges.emit(value as T);
        });
    }
  }
}
