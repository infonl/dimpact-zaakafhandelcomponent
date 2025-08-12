/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
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
      <zac-persoon-zoek
        *ngIf="type === 'persoon'"
        #zoek
        [context]="this.context()"
        [blockSearch]="form.invalid"
        action="klant-koppelen-betrokkene"
        [syncEnabled]="true"
        (persoon)="klantGeselecteerd($event)"
      ></zac-persoon-zoek>
      <zac-bedrijf-zoek
        *ngIf="type === 'bedrijf'"
        #zoek
        [blockSearch]="form.invalid"
        [syncEnabled]="true"
        (bedrijf)="klantGeselecteerd($event)"
      ></zac-bedrijf-zoek>
    </div>
  `,
})
export class KlantKoppelBetrokkeneComponent implements OnInit {
  @Input({ required: true }) type!: "persoon" | "bedrijf";
  @Input({ required: true }) zaaktypeUUID!: string;
  @Output() klantGegevens = new EventEmitter<KlantGegevens>();
  @ViewChild("zoek") zoek!: PersoonZoekComponent | BedrijfZoekComponent;

  context = input.required<string>();

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
    this.klantenService
      .listBetrokkeneRoltypen(this.zaaktypeUUID)
      .subscribe((betrokkeneRoltypen) => {
        this.betrokkeneRoltypen = betrokkeneRoltypen;
      });
  }

  klantGeselecteerd(klant: GeneratedType<"RestPersoon" | "RestBedrijf">) {
    const data = this.form.value;
    this.klantGegevens.emit({
      klant,
      betrokkeneRoltype: data.betrokkeneRoltype!,
      betrokkeneToelichting: data.toelichting ?? "",
    });
  }
}
