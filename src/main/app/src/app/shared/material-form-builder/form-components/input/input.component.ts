/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { BehaviorSubject, Subject, takeUntil } from "rxjs";
import { ActionIcon } from "../../../edit/action-icon";
import { FormComponent } from "../../model/form-component";
import { InputFormField } from "./input-form-field";

@Component({
  templateUrl: "./input.component.html",
  styleUrls: ["./input.component.less"],
})
export class InputComponent extends FormComponent implements OnInit, OnDestroy {
  data!: InputFormField;
  destroyed$ = new Subject<void>();

  clearDisabled$ = new BehaviorSubject<boolean>(false);
  iconButtonsDisabled$ = new BehaviorSubject<boolean>(false);

  constructor(public translate: TranslateService) {
    super();
  }

  ngOnInit(): void {
    this.setDisabled();
    this.data.formControl.statusChanges
      .pipe(takeUntil(this.destroyed$))
      .subscribe(() => {
        this.setDisabled();
      });
    this.data.onClear.pipe(takeUntil(this.destroyed$)).subscribe(() => {
      this.data.formControl.setValue(null);
    });
  }

  ngOnDestroy(): void {
    this.destroyed$.next();
    this.destroyed$.complete();
  }

  private setDisabled(): void {
    if (this.data.externalInput) {
      this.data.styleClass = this.data.styleClass + " input__non-editable";
      this.data.formControl.disable();
      this.clearDisabled$.next(false);
      this.iconButtonsDisabled$.next(false);
      return;
    }

    this.clearDisabled$.next(this.data.formControl.disabled);
    this.iconButtonsDisabled$.next(this.data.formControl.disabled);
  }

  iconClick($event: MouseEvent, icon: ActionIcon): void {
    icon.iconClicked.next(null);
  }

  clicked(): void {
    if (this.data.clicked.observed) {
      this.data.clicked.next(null);
    }
  }
}
