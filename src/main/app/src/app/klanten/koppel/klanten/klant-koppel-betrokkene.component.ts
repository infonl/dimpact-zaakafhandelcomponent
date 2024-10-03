/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { MatIcon } from "@angular/material/icon";
import { MatTabLabel } from "@angular/material/tabs";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialFormBuilderModule } from "src/app/shared/material-form-builder/material-form-builder.module";
import { SharedModule } from "src/app/shared/shared.module";
import { InputFormField } from "../../../shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "../../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormField } from "../../../shared/material-form-builder/form-components/select/select-form-field";
import { SelectFormFieldBuilder } from "../../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { KlantenModule } from "../../klanten.module";
import { KlantenService } from "../../klanten.service";
import { Klant } from "../../model/klanten/klant";
import { KlantGegevens } from "../../model/klanten/klant-gegevens";
import { BedrijfZoekComponent } from "../../zoek/bedrijven/bedrijf-zoek.component";
import { PersoonZoekComponent } from "../../zoek/personen/persoon-zoek.component";

@Component({
  selector: "zac-klant-koppel-betrokkene-persoon",
  standalone: true,
  imports: [
    SharedModule,
    MatIcon,
    TranslateModule,
    MaterialFormBuilderModule,
    KlantenModule,
    MatTabLabel,
  ],
  template: `
    <div class="form">
      <div class="flex-row flex-col-sm gap-10">
        <mfb-form-field
          class="flex-1"
          [field]="betrokkeneRoltype"
        ></mfb-form-field>
        <mfb-form-field
          class="flex-1"
          [field]="betrokkeneToelichting"
        ></mfb-form-field>
      </div>
    </div>
    @if (type === "persoon") {
      <zac-persoon-zoek
        #zoek
        [syncEnabled]="true"
        (persoon)="klantGeselecteerd($event)"
      ></zac-persoon-zoek>
    }
    @if (type === "bedrijf") {
      <zac-bedrijf-zoek
        #zoek
        [syncEnabled]="true"
        (bedrijf)="klantGeselecteerd($event)"
      ></zac-bedrijf-zoek>
    }
  `,
})
export class KlantKoppelBetrokkeneComponent implements OnInit, AfterViewInit {
  @Input() type: "persoon" | "bedrijf" = "persoon";
  @Input() zaaktypeUUID: string;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();
  @ViewChild("zoek") zoek: PersoonZoekComponent | BedrijfZoekComponent;

  betrokkeneRoltype: SelectFormField;
  betrokkeneToelichting: InputFormField;
  formGroup: FormGroup;

  constructor(
    private klantenService: KlantenService,
    private formBuilder: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.buildFormFields();
    this.formGroup = this.formBuilder.group({
      rol: this.betrokkeneRoltype.formControl,
      toelichting: this.betrokkeneToelichting.formControl,
    });
  }

  ngAfterViewInit(): void {
    this.zoek.formGroup.addControl(
      "betrokkeneRoltype",
      this.betrokkeneRoltype.formControl,
    );
    this.zoek.formGroup.addControl(
      "betrokkenToelichting",
      this.betrokkeneToelichting.formControl,
    );
  }

  private buildFormFields() {
    this.betrokkeneRoltype = new SelectFormFieldBuilder()
      .id("betrokkeneType")
      .label("betrokkeneRoltype")
      .optionLabel("naam")
      .options(this.klantenService.listBetrokkeneRoltypen(this.zaaktypeUUID))
      .validators(Validators.required)
      .build();
    this.betrokkeneToelichting = new InputFormFieldBuilder()
      .id("betrokkenToelichting")
      .label("toelichting")
      .maxlength(75)
      .build();
  }

  klantGeselecteerd(klant: Klant): void {
    const klantGegevens: KlantGegevens = new KlantGegevens(klant);
    klantGegevens.betrokkeneRoltype = this.betrokkeneRoltype.formControl.value;
    klantGegevens.betrokkeneToelichting =
      this.betrokkeneToelichting.formControl.value;

    this.klantGegevens.emit(klantGegevens);
  }
}
