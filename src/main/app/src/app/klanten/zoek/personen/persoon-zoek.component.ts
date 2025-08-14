/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  input,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { AbstractControl, FormBuilder, Validators } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { Router } from "@angular/router";
import moment from "moment";
import { Subject, takeUntil } from "rxjs";
import { ConfiguratieService } from "../../../configuratie/configuratie.service";
import { UtilService } from "../../../core/service/util.service";
import {
  BSN_LENGTH,
  POSTAL_CODE_LENGTH,
} from "../../../shared/utils/constants";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { CustomValidators } from "../../../shared/validators/customValidators";
import { KlantenService } from "../../klanten.service";
import { FormCommunicatieService } from "../form-communicatie-service";

@Component({
  selector: "zac-persoon-zoek",
  templateUrl: "./persoon-zoek.component.html",
  styleUrls: ["./persoon-zoek.component.less"],
})
export class PersoonZoekComponent implements OnInit, OnDestroy {
  @Output() persoon? = new EventEmitter<GeneratedType<"RestPersoon">>();
  @Input() sideNav?: MatSidenav;
  @Input() syncEnabled: boolean = false;

  protected action = input.required<string>();
  protected context = input.required<string>();
  protected blockSearch = input<boolean>(false);

  private readonly destroy$ = new Subject<void>();

  queries: GeneratedType<"RestPersonenParameters">[] = [];
  persoonColumns = [
    "bsn",
    "naam",
    "geboortedatum",
    "verblijfplaats",
    "acties",
  ] as const;
  personen = new MatTableDataSource<GeneratedType<"RestPersoon">>();
  foutmelding?: string;
  loading = false;
  uuid = crypto.randomUUID();

  public formGroup = this.formBuilder.group({
    bsn: this.formBuilder.control<string | null>(null, [
      CustomValidators.bsn,
      Validators.maxLength(BSN_LENGTH),
    ]),
    geboortedatum: this.formBuilder.control<moment.Moment | null>(null),
    voornamen: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(50),
    ]),
    voorvoegsel: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(10),
    ]),
    geslachtsnaam: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(50),
    ]),
    gemeenteVanInschrijving: this.formBuilder.control<string | null>(null, [
      Validators.min(1),
      Validators.max(9999),
      Validators.maxLength(4),
    ]),
    straat: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(55),
    ]),
    postcode: this.formBuilder.control<string | null>(null, [
      CustomValidators.postcode,
      Validators.maxLength(POSTAL_CODE_LENGTH),
    ]),
    huisnummer: this.formBuilder.control<number | null>(null, [
      Validators.min(1),
      Validators.max(99999),
      Validators.maxLength(5),
    ]),
  });

  constructor(
    private readonly klantenService: KlantenService,
    private readonly utilService: UtilService,
    private readonly formBuilder: FormBuilder,
    private readonly router: Router,
    private readonly configuratieService: ConfiguratieService,
    private readonly formCommunicationService: FormCommunicatieService,
  ) {
    this.klantenService
      .getPersonenParameters()
      .subscribe((personenParameters) => {
        this.queries = personenParameters;
      });

    this.formGroup.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        const parameters = {
          ...value,
          geboortedatum: value.geboortedatum?.toISOString(),
        };
        this.updateControls(this.getValidQueries(parameters, false));
      });
  }

  ngOnInit() {
    if (!this.syncEnabled) return;

    this.uuid = crypto.randomUUID();

    this.formCommunicationService.itemSelected$
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ selected, uuid }) => {
        if (!selected) return;
        if (uuid === this.uuid) return;
        this.clearFormAndData();
      });
  }

  isValid() {
    const parameters = {
      ...this.formGroup.value,
      geboortedatum: this.formGroup.value.geboortedatum?.toISOString(),
    };

    return (
      this.formGroup.valid &&
      this.getValidQueries(parameters, true).length &&
      !this.blockSearch()
    );
  }

  private getValidQueries(
    parameters: GeneratedType<"RestListPersonenParameters">,
    compleet: boolean,
  ) {
    return Object.keys(this.formGroup.controls).reduce((acc, key) => {
      if (parameters[key as keyof typeof parameters]) {
        // Verwijder alle queries die met dit gevulde veld niet kunnen
        return this.getQueriesWithoutCardinality(
          acc,
          key as keyof GeneratedType<"RestPersonenParameters">,
          "NON",
        );
      } else if (compleet) {
        // Verwijder alles queries die zonder dit lege veld niet kunnen
        return this.getQueriesWithoutCardinality(
          acc,
          key as keyof typeof parameters,
          "REQ",
        );
      }
      return acc;
    }, this.queries);
  }

  private getQueriesWithoutCardinality(
    queries: GeneratedType<"RestPersonenParameters">[],
    key: keyof GeneratedType<"RestPersonenParameters">,
    cardinality: GeneratedType<"Cardinaliteit">,
  ) {
    return queries.filter((query) => query[key] !== cardinality);
  }

  private getQueriesWithCardinality(
    queries: GeneratedType<"RestPersonenParameters">[],
    key: keyof GeneratedType<"RestPersonenParameters">,
    cardinality: GeneratedType<"Cardinaliteit">,
  ) {
    return queries.filter((query) => query[key] === cardinality);
  }

  private allQueriesHaveCardinality(
    queries: GeneratedType<"RestPersonenParameters">[],
    key: keyof GeneratedType<"RestPersonenParameters">,
    cardinality: GeneratedType<"Cardinaliteit">,
  ) {
    return (
      this.getQueriesWithCardinality(queries, key, cardinality).length ===
      queries.length
    );
  }

  protected setGemeenteVanInschrijvingToCurrentGemeente() {
    this.configuratieService.readGemeenteCode().subscribe((gemeenteCode) => {
      this.formGroup.controls.gemeenteVanInschrijving?.setValue(gemeenteCode);
    });
  }

  private updateControls(potential: GeneratedType<"RestPersonenParameters">[]) {
    const hasValues = Object.values(potential).some(Boolean);
    for (const [key, control] of Object.entries(this.formGroup.controls)) {
      if (!hasValues) {
        this.enableField(control, true);
      } else if (
        this.allQueriesHaveCardinality(
          potential,
          key as keyof GeneratedType<"RestPersonenParameters">,
          "NON",
        )
      ) {
        this.requireField(control, false);
        this.enableField(control, false);
      } else {
        this.requireField(
          control,
          this.allQueriesHaveCardinality(
            potential,
            key as keyof GeneratedType<"RestPersonenParameters">,
            "REQ",
          ),
        );
        this.enableField(control, true);
      }
    }
    this.formGroup.updateValueAndValidity({ emitEvent: false });
  }

  private requireField(control: AbstractControl, required: boolean) {
    if (required) {
      control.addValidators(Validators.required);
    } else {
      control.removeValidators(Validators.required);
    }
  }

  private enableField(control: AbstractControl, enabled: boolean) {
    if (enabled) {
      control.enable({ emitEvent: false });
    } else {
      control.disable({ emitEvent: false });
      control.setValue(null, { emitEvent: false });
    }
  }

  zoekPersonen() {
    this.loading = true;
    this.utilService.setLoading(true);
    this.personen.data = [];
    const data = this.formGroup.value;
    this.klantenService
      .listPersonen(
        {
          ...data,
          geboortedatum: data.geboortedatum?.toISOString() ?? null,
          gemeenteVanInschrijving:
            data.gemeenteVanInschrijving?.toString() ?? null,
        },
        {
          context: this.context(),
          action: this.action(),
        },
      )
      .subscribe({
        next: (personen) => {
          this.personen.data = personen.resultaten ?? [];
          this.foutmelding = personen.foutmelding;
          this.loading = false;
          this.utilService.setLoading(false);
        },
        error: () => {
          this.loading = false;
          this.utilService.setLoading(false);
        },
      });
  }

  protected selectPersoon(persoon: GeneratedType<"RestPersoon">) {
    this.persoon?.emit(persoon);
    this.clearFormAndData();

    if (this.syncEnabled) {
      this.formCommunicationService.notifyItemSelected(this.uuid);
    }
  }

  protected openPersoonPagina(persoon: GeneratedType<"RestPersoon">) {
    this.sideNav?.close();
    void this.router.navigate(["/persoon/", persoon.identificatie]);
  }

  protected clearFormAndData() {
    this.formGroup.reset();
    this.personen.data = [];
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
