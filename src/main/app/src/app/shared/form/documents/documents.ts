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
  OnDestroy,
  OnInit,
  SimpleChanges,
} from "@angular/core";
import { AbstractControl, FormGroup } from "@angular/forms";
import { MatTableDataSource } from "@angular/material/table";
import { Observable, Subject, takeUntil } from "rxjs";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../utils/generated-types";

@Component({
  selector: "zac-documents",
  templateUrl: "./documents.html",
  styleUrls: ["./documents.less"],
})
export class ZacDocuments<
    Form extends Record<string, AbstractControl>,
    Key extends keyof Form,
    Option extends GeneratedType<"RestEnkelvoudigInformatieobject">,
  >
  implements OnInit, OnChanges, OnDestroy
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

  private destroy$ = new Subject<void>();

  protected control?: AbstractControl<Option[] | null>;

  protected selection = new SelectionModel<Option>(true, []);
  protected dataSource = new MatTableDataSource<Option>();
  protected columnsWithSelect = [
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

  constructor(
    private readonly informatieObjectenService: InformatieObjectenService,
  ) {}

  ngOnInit() {
    this.control = this.form.get(String(this.key))!;
    this.setOptions(this.options);
    this.control.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((options) => {
        this.selection.select(...(options ?? [])); // Re-select current values
      });
  }

  ngOnChanges(changes: SimpleChanges) {
    if ("options" in changes) {
      this.setOptions(changes.options.currentValue);
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onToggleOption(option: Option) {
    this.selection.toggle(option);
    this.control?.setValue(this.selection.selected);
  }

  protected viewLink(option: Option) {
    return `/informatie-objecten/${option.uuid}`;
  }

  protected downloadLink(option: Option) {
    if (!option.uuid) return null;
    return this.informatieObjectenService.getDownloadURL(option.uuid);
  }

  private setOptions(options: Array<Option> | Observable<Array<Option>> = []) {
    if (options instanceof Observable) {
      options
        .pipe(takeUntil(this.destroy$))
        .subscribe((options) => this.setOptions(options));
      return;
    }

    this.dataSource.data = options;
    this.selection.select(...(this.control?.value ?? [])); // Re-select current values
  }
}
