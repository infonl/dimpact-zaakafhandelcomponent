/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import moment, { Moment } from "moment";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { DocumentenLijstFieldBuilder } from "../../shared/material-form-builder/form-components/documenten-lijst/documenten-lijst-field-builder";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-besluit-create",
  templateUrl: "./besluit-create.component.html",
  styleUrls: ["./besluit-create.component.less"],
})
export class BesluitCreateComponent implements OnInit {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() besluitVastgelegd = new EventEmitter<boolean>();

  protected resultaattypes: GeneratedType<"RestResultaattype">[] = [];
  protected besluittypes: GeneratedType<"RestDecisionType">[] = [];

  protected documentenField = new DocumentenLijstFieldBuilder()
    .id("documenten")
    .label("documenten")
    .build();

  protected form = this.formBuilder.group({
    resultaat:
      this.formBuilder.control<GeneratedType<"RestResultaattype"> | null>(
        null,
        Validators.required,
      ),
    besluit: this.formBuilder.control<GeneratedType<"RestDecisionType"> | null>(
      null,
      Validators.required,
    ),
    toelichting: this.formBuilder.control<string | null>(
      null,
      Validators.maxLength(1000),
    ),
    ingangsdatum: this.formBuilder.control(moment(), Validators.required),
    vervaldatum: this.formBuilder.control<Moment | null>(null, [
      Validators.min(moment().startOf("day").valueOf()),
    ]),
    publicationEnabled: this.formBuilder.control(false),
    publicatiedatum: this.formBuilder.control<Moment | null>(moment()),
    uiterlijkereactiedatum: this.formBuilder.control<Moment | null>(null),
  });

  constructor(
    private readonly zakenService: ZakenService,
    private readonly utilService: UtilService,
    private readonly informatieObjectenService: InformatieObjectenService,
    private readonly formBuilder: FormBuilder,
  ) {
    this.form.controls.resultaat.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (value) {
          this.form.controls.vervaldatum.addValidators([Validators.required]);
          return;
        }

        this.form.controls.vervaldatum.removeValidators([Validators.required]);
      });

    this.form.controls.ingangsdatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (value) {
          this.form.controls.vervaldatum.addValidators([
            Validators.min(moment(value).startOf("day").valueOf()),
          ]);
          return;
        }

        this.form.controls.vervaldatum.removeValidators([Validators.min(0)]);
      });

    this.form.controls.besluit.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) return;

        this.informatieObjectenService
          .listEnkelvoudigInformatieobjecten({
            zaakUUID: this.zaak.uuid,
            besluittypeUUID: value.id,
          })
          .subscribe((documenten) => {
            this.documentenField.formControl.setValue(
              documenten.map((document) => document.uuid).join(";"),
            );
          });

        this.form.controls.publicationEnabled.setValue(
          value.publication.enabled ?? null,
        );
        if (!value.publication.enabled) return;
        this.setUiterlijkereactiedatum(
          moment(),
          value.publication.responseTermDays,
        );
      });

    this.form.controls.publicatiedatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) {
          this.form.controls.uiterlijkereactiedatum.setValue(null);
          this.form.controls.uiterlijkereactiedatum.clearValidators();
          return;
        }

        this.setUiterlijkereactiedatum(
          value,
          this.form.controls.besluit.value?.publication.responseTermDays,
        );
      });
  }

  private setUiterlijkereactiedatum(
    date: Moment,
    responseTermDays?: number | null,
  ) {
    const uiterlijkereactiedatum = date.add(responseTermDays ?? 0, "days");
    this.form.controls.uiterlijkereactiedatum.setValue(uiterlijkereactiedatum);
    this.form.controls.uiterlijkereactiedatum.addValidators(
      Validators.min(moment(uiterlijkereactiedatum).startOf("day").valueOf()),
    );
  }

  ngOnInit() {
    this.zakenService
      .listResultaattypes(this.zaak.zaaktype.uuid)
      .subscribe((resultaattypes) => {
        this.resultaattypes = resultaattypes;
      });

    this.zakenService
      .listBesluittypes(this.zaak.zaaktype.uuid)
      .subscribe((besluittypes) => {
        this.besluittypes = besluittypes;
      });

    this.form.patchValue({
      resultaat: this.zaak.resultaat?.resultaattype,
    });
  }

  submit() {
    const value = this.form.value;

    this.zakenService
      .createBesluit({
        zaakUuid: this.zaak.uuid,
        resultaattypeUuid: value.resultaat!.id,
        besluittypeUuid: value.besluit!.id,
        toelichting: value.toelichting ?? undefined,
        ingangsdatum: value.ingangsdatum?.toISOString(),
        vervaldatum: value.vervaldatum?.toISOString(),
        informatieobjecten: this.documentenField.value.toString().split(";"),
        ...(value.publicationEnabled
          ? {
              publicationDate: value.publicatiedatum?.toISOString(),
              lastResponseDate: value.uiterlijkereactiedatum?.toISOString(),
            }
          : {}),
      })
      .subscribe(() => {
        this.utilService.openSnackbar("msg.besluit.vastgelegd");
        this.besluitVastgelegd.emit(true);
      });
  }
}
