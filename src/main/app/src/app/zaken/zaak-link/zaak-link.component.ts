/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input, output } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, FormControl, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { injectMutation } from "@tanstack/angular-query-experimental";
import { UtilService } from "src/app/core/service/util.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-link",
  templateUrl: "./zaak-link.component.html",
  styleUrls: ["./zaak-link.component.less"],
})
export class ZaakLinkComponent {
  private readonly zoekenService = inject(ZoekenService);
  private readonly zakenService = inject(ZakenService);
  private readonly utilService = inject(UtilService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();
  protected readonly sideNav = input.required<MatDrawer>();

  readonly zaakLinked = output();

  private readonly koppelZaakMutation = injectMutation(() => ({
    ...this.zakenService.koppelZaak(),
    onSuccess: ({ identificatie }) => {
      this.utilService.openSnackbar("msg.zaak.gekoppeld", {
        case: identificatie,
      });
      this.zaakLinked.emit();
      this.close();
    },
  }));

  public cases = new MatTableDataSource<
    GeneratedType<"RestZaakKoppelenZoekObject">
  >();
  public totalCases = 0;
  public caseColumns = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "omschrijving",
    "acties",
  ] as const;
  public loading = false;

  protected caseRelationOptionsList: {
    label: `zaak.koppelen.link.type.${GeneratedType<"RelatieType">}`;
    value: GeneratedType<"RelatieType">;
  }[] = [
    {
      label: "zaak.koppelen.link.type.DEELZAAK",
      value: "DEELZAAK",
    },
    {
      label: "zaak.koppelen.link.type.HOOFDZAAK",
      value: "HOOFDZAAK",
    },
  ];

  protected readonly form = this.formBuilder.group({
    caseRelationType: new FormControl<
      (typeof this.caseRelationOptionsList)[number] | null
    >(null, [Validators.required]),
    caseToSearchFor: new FormControl<string>("", [
      Validators.required,
      Validators.minLength(2),
    ]),
  });

  constructor() {
    this.form.controls.caseToSearchFor.disable();

    this.form.controls.caseRelationType.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        this.form.controls.caseToSearchFor.reset();
        this.form.controls.caseToSearchFor.enable();
        this.clearSearchResult();
      });

    this.form.controls.caseToSearchFor.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => {
        if (
          this.cases.data?.length > 0 &&
          this.form.controls.caseToSearchFor.value === null
        )
          this.clearSearchResult();
      });
  }

  protected searchCases() {
    this.loading = true;
    this.utilService.setLoading(true);

    this.zoekenService
      .findLinkableZaken(
        this.zaak().uuid,
        this.form.controls.caseToSearchFor.value!,
        this.form.controls.caseRelationType.value!.value!,
      )
      .subscribe(
        (result) => {
          this.cases.data = result.resultaten;
          this.totalCases = result.totaal ?? 0;
          this.loading = false;
          this.utilService.setLoading(false);
        },
        () => {
          // error handling
          this.loading = false;
          this.utilService.setLoading(false);
        },
      );
  }

  protected selectCase(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    if (!row.id || !this.form.controls.caseRelationType.value?.value) return;

    void this.koppelZaakMutation.mutateAsync({
      zaakUuid: this.zaak().uuid,
      teKoppelenZaakUuid: row.id,
      relatieType: this.form.controls.caseRelationType.value.value,
    });
  }

  protected rowDisabled(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    return !row.isKoppelbaar || row.identificatie === this.zaak().identificatie;
  }

  protected close() {
    void this.sideNav().close();
    this.reset();
  }

  protected reset() {
    this.form.reset();
    this.clearSearchResult();
  }

  private clearSearchResult() {
    this.cases.data = [];
    this.totalCases = 0;
    this.loading = false;
    this.utilService.setLoading(false);
  }
}
