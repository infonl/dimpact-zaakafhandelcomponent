/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, OnInit, output } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { TranslateModule } from "@ngx-translate/core";
import { ZacInput } from "src/app/shared/form/input/input";
import { ZacSelect } from "src/app/shared/form/select/select";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { KlantenService } from "../../../klanten.service";
import { KlantGegevens } from "../../../model/klanten/klant-gegevens";
import { BedrijfZoekComponent } from "../../../zoek/bedrijven/bedrijf-zoek.component";
import { PersoonZoekComponent } from "../../../zoek/personen/persoon-zoek.component";

@Component({
  selector: "zac-klant-koppel-betrokkene-persoon",
  imports: [
    TranslateModule,
    ReactiveFormsModule,
    PersoonZoekComponent,
    BedrijfZoekComponent,
    ZacSelect,
    ZacInput,
  ],
  template: `
    <div>
      <form [formGroup]="form">
        <fieldset class="pt-3">
          <section class="row">
            <zac-select
              class="col-6"
              [form]="form"
              key="betrokkeneRoltype"
              [options]="betrokkeneRoltypen"
              optionDisplayValue="naam"
            />
            <zac-input class="col-6" [form]="form" key="toelichting" />
          </section>
        </fieldset>
      </form>
      @if (type() === "persoon") {
        <zac-persoon-zoek
          [blockSearch]="form.invalid"
          [syncEnabled]="true"
          isSelectable
          (persoon)="klantGeselecteerd($event)"
          [zaaktypeUUID]="zaaktypeUUID()"
        ></zac-persoon-zoek>
      }
      @if (type() === "bedrijf") {
        <zac-bedrijf-zoek
          [blockSearch]="form.invalid"
          [syncEnabled]="true"
          isSelectable
          (bedrijf)="klantGeselecteerd($event)"
        ></zac-bedrijf-zoek>
      }
    </div>
  `,
})
export class KlantKoppelBetrokkeneComponent implements OnInit {
  readonly type = input.required<"persoon" | "bedrijf">();
  readonly zaaktypeUUID = input<string | null | undefined>(null);
  readonly klantGegevens = output<KlantGegevens>();

  protected readonly form = this.formBuilder.group({
    betrokkeneRoltype:
      this.formBuilder.control<GeneratedType<"RestRoltype"> | null>(
        null,
        Validators.required,
      ),
    toelichting: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(75),
    ]),
  });
  protected betrokkeneRoltypen: GeneratedType<"RestRoltype">[] = [];

  constructor(
    private readonly klantenService: KlantenService,
    private readonly formBuilder: FormBuilder,
  ) {}

  ngOnInit() {
    const zaaktypeUUID = this.zaaktypeUUID();
    if (!zaaktypeUUID) return;

    this.klantenService
      .listBetrokkeneRoltypen(zaaktypeUUID)
      .subscribe((betrokkeneRoltypen) => {
        this.betrokkeneRoltypen = betrokkeneRoltypen;
      });
  }

  klantGeselecteerd(klant: GeneratedType<"RestPersoon" | "RestBedrijf">) {
    const { value } = this.form;
    this.klantGegevens.emit({
      klant,
      betrokkeneRoltype: value.betrokkeneRoltype!,
      betrokkeneToelichting: value.toelichting ?? "",
    });
  }
}
