/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { SelectionModel } from "@angular/cdk/collections";
import { NgIf } from "@angular/common";
import { booleanAttribute, Component, effect, input } from "@angular/core";
import { AbstractControl, ReactiveFormsModule } from "@angular/forms";
import { MatIconAnchor } from "@angular/material/button";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { TranslatePipe } from "@ngx-translate/core";
import { takeUntil } from "rxjs";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { DocumentIconComponent } from "../../document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../../indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { BestandsomvangPipe } from "../../pipes/bestandsomvang.pipe";
import { GeneratedType } from "../../utils/generated-types";
import { MultiInputFormField } from "../BaseFormField";

@Component({
  selector: "zac-documents",
  templateUrl: "./documents.html",
  styleUrls: ["./documents.less"],
  standalone: true,
  imports: [
    BestandsomvangPipe,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
    MatCheckboxModule,
    MatIconAnchor,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    NgIf,
    ReactiveFormsModule,
    TranslatePipe,
  ],
})
export class ZacDocuments<
  Form extends Record<string, AbstractControl>,
  Key extends keyof Form,
  Option extends GeneratedType<"RestEnkelvoudigInformatieobject">,
> extends MultiInputFormField<Form, Key, Option, () => string> {
  protected selectLabel = input<string>();
  protected viewDocumentInNewTab = input(false, {
    transform: booleanAttribute,
  });

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
  ) {
    super();

    effect(() => {
      this.dataSource.data = this.availableOptions();
    });

    effect(() => {
      this.control()
        ?.valueChanges.pipe(takeUntil(this.destroy$))
        .subscribe((options) => {
          this.selection.select(...((options as unknown as Option[]) ?? [])); // Re-select current values
        });
    });
  }

  protected onToggleOption(option: Option) {
    this.selection.toggle(option);
    this.control()?.setValue(this.selection.selected as unknown as Option);
  }

  protected viewLink(option: Option) {
    return `/informatie-objecten/${option.uuid}`;
  }

  protected downloadLink(option: Option) {
    if (!option.uuid) return null;
    return this.informatieObjectenService.getDownloadURL(option.uuid);
  }
}
