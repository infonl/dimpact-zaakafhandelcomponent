/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { SelectionModel } from "@angular/cdk/collections";
import {
  booleanAttribute,
  Component,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
} from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { MatTableDataSource } from "@angular/material/table";
import { Observable, Subject, takeUntil } from "rxjs";
import { GeneratedType } from "../../utils/generated-types";

@Component({
  selector: "zac-documents",
  templateUrl: "./documents.html",
})
export class ZacDocuments<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
    Option extends GeneratedType<"RestEnkelvoudigInformatieobject">,
  >
  implements OnInit, OnChanges
{
  @Input({ required: true }) key!: Key & string;
  @Input({ required: true }) form!: FormGroup<Form>;
  @Input({ transform: booleanAttribute }) readonly = false;
  @Input() label?: string;
  @Input() selectLabel?: string;
  @Input({ required: true }) options!:
    | Array<Option>
    | Observable<Array<Option>>;
  @Input({ transform: booleanAttribute }) viewDocumentInNewTab = false;

  private destroy$ = new Subject();

  protected control?: AbstractControl<Option[] | null>;

  protected selection = new SelectionModel<Option>(true, []);
  protected dataSource = new MatTableDataSource<Option>();
  protected columnsWithSelect: (string | keyof Option)[] = [
    "select",
    "titel",
    "documentType",
    "status",
    "versie",
    "auteur",
    "creatiedatum",
    "bestandsomvang",
    "indicaties",
    "url",
  ] as const;

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
    this.setOptions(this.options);
  }

  ngOnChanges(changes: SimpleChanges) {
    if ("options" in changes) {
      this.setOptions(changes.options.currentValue);
    }
  }

  protected onToggleOption(option: Option) {
    this.selection.toggle(option);
    this.control?.setValue(this.selection.selected);
  }

  private setOptions(options: Array<Option> | Observable<Array<Option>> = []) {
    if (options instanceof Observable) {
      options
        .pipe(takeUntil(this.destroy$))
        .subscribe((options) => this.setOptions(options));
      return;
    }

    this.dataSource.data = options;
    this.selection.clear();
  }
}
