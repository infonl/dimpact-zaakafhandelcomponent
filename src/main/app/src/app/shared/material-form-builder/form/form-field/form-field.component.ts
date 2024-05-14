/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from "@angular/core";
import { Subject, takeUntil } from "rxjs";
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
export class FormFieldComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() field: AbstractFormField;

  @Output() valueChanges = new EventEmitter<any>();

  private _field: FormItem;
  private loaded: boolean;

  @ViewChild(FormFieldDirective) formField: FormFieldDirective;

  unsubscribe$ = new Subject<void>();

  constructor(public mfbService: MaterialFormBuilderService) {}

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  ngOnInit(): void {
    this._field = this.mfbService.getFormItem(this.field);
    this.refreshComponent();
  }

  ngAfterViewInit() {
    this.loadComponent();
  }

  refreshComponent() {
    if (this.loaded) {
      this.unsubscribe$.next();
      this.formField.viewContainerRef.clear();
      this.loadComponent();
    }
  }

  loadComponent() {
    if (this._field.data.readonly && !this._field.data.hasReadonlyView()) {
      this._field = new FormItem(
        ReadonlyComponent,
        new ReadonlyFormFieldBuilder(this._field.data.formControl.value)
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
      this._field.data.formControl.valueChanges
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe((value) => {
          this.valueChanges.emit(value);
        });
    }
  }
}
