/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { MatDrawer } from "@angular/material/sidenav";
import { TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/select/select-form-field-builder";
import { AbstractFormControlField } from "src/app/shared/material-form-builder/model/abstract-form-control-field";
import { Zaak } from "../model/zaak";

@Component({
  selector: "zac-zaak-link",
  templateUrl: "./zaak-link.component.html",
  styleUrls: ["./zaak-link.component.less"],
})
export class ZaakLinkComponent implements OnInit, OnDestroy {
  @Input({ required: true }) zaak!: Zaak; // GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;

  intro: string = "";
  selectLinkTypeField?: AbstractFormControlField;
  caseSearchField?: AbstractFormControlField;
  isValid: boolean = false;
  loading: boolean = false;

  private ngDestroy = new Subject<void>();

  constructor(private translate: TranslateService) {}

  ngOnInit() {
    this.intro = this.translate.instant("zaak.koppelen.uitleg", {
      zaakID: this.zaak.identificatie,
    });

    this.selectLinkTypeField = new SelectFormFieldBuilder()
      .id("linkType")
      .label("zaak.link.type")
      .options([
        // { value: "geen", label: "zaak.link.type.-geen-" },
        { value: "hoofdzaak", label: "zaak.link.type.hoofdzaak" },
        { value: "deelzaak", label: "zaak.link.type.deelzaak" },
      ])
      .optionValue("value")
      .optionLabel("label")
      .build();

    this.caseSearchField = new InputFormFieldBuilder()
      .id("zaak")
      .label("zaak.identificatie")
      .disabled(true)
      .build();

    this.selectLinkTypeField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        if (!this.caseSearchField) return;

        switch (value) {
          case "hoofdzaak":
            this.caseSearchField.formControl.enable();
            this.caseSearchField.hint.label = this.translate.instant(
              "zaak.link.hoofdzaak.hint",
            );
            break;
          case "deelzaak":
            this.caseSearchField.formControl.enable();
            this.caseSearchField.hint.label = this.translate.instant(
              "zaak.link.deelzaak.hint",
            );
            break;
          default:
            this.caseSearchField.formControl.setValue("");
            this.caseSearchField.formControl.disable();
            break;
        }
      });

    this.caseSearchField.formControl.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe((value) => {
        this.isValid = value?.length >= 2;
      });
  }

  searchCases() {}

  selectCase() {}

  closeDrawer() {
    this.sideNav.close();
    this.reset();
  }

  private reset() {}

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
