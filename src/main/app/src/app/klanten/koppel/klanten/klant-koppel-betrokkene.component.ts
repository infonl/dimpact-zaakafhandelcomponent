/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  input,
  Input,
  OnInit,
  Output,
  ViewChild,
} from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialFormBuilderModule } from "src/app/shared/material-form-builder/material-form-builder.module";
import { SharedModule } from "src/app/shared/shared.module";
import { InputFormFieldBuilder } from "../../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { SelectFormField } from "../../../shared/material-form-builder/form-components/select/select-form-field";
import { SelectFormFieldBuilder } from "../../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { KlantenModule } from "../../klanten.module";
import { KlantenService } from "../../klanten.service";
import { KlantGegevens } from "../../model/klanten/klant-gegevens";
import { BedrijfZoekComponent } from "../../zoek/bedrijven/bedrijf-zoek.component";
import { PersoonZoekComponent } from "../../zoek/personen/persoon-zoek.component";

@Component({
  selector: "zac-klant-koppel-betrokkene-persoon",
  standalone: true,
  imports: [
    SharedModule,
    TranslateModule,
    MaterialFormBuilderModule,
    KlantenModule,
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
        [context]="this.context()"
        action="klant-koppelen-betrokkene"
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
  @Input({ required: true }) type: "persoon" | "bedrijf" = "persoon";
  @Input({ required: true }) zaaktypeUUID!: string;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();
  @ViewChild("zoek") zoek!: PersoonZoekComponent | BedrijfZoekComponent;

  context = input.required<string>();

  betrokkeneRoltype!: SelectFormField<GeneratedType<"RestRoltype">>;
  betrokkeneToelichting = new InputFormFieldBuilder()
    .id("betrokkenToelichting")
    .label("toelichting")
    .maxlength(75)
    .build();
  formGroup = this.formBuilder.group({
    rol: this.betrokkeneRoltype.formControl,
    toelichting: this.betrokkeneToelichting.formControl,
  });

  constructor(
    private klantenService: KlantenService,
    private formBuilder: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.betrokkeneRoltype = new SelectFormFieldBuilder()
      .id("betrokkeneType")
      .label("betrokkeneRoltype")
      .optionLabel("naam")
      .options(this.klantenService.listBetrokkeneRoltypen(this.zaaktypeUUID))
      .validators(Validators.required)
      .build();
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

  klantGeselecteerd(klant: GeneratedType<"RestPersoon" | "RestBedrijf">): void {
    const klantGegevens = new KlantGegevens(klant);
    klantGegevens.betrokkeneRoltype = this.betrokkeneRoltype.formControl.value!;
    klantGegevens.betrokkeneToelichting =
      this.betrokkeneToelichting.formControl.value!;

    this.klantGegevens.emit(klantGegevens);
  }
}
