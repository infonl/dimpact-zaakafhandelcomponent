/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2025 INFO.nl
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
import { FormBuilder, Validators } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { Router } from "@angular/router";
import { Subject, takeUntil } from "rxjs";
import { UtilService } from "../../../core/service/util.service";
import {
  BSN_LENGTH,
  KVK_LENGTH,
  POSTAL_CODE_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "../../../shared/utils/constants";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { CustomValidators } from "../../../shared/validators/customValidators";
import { KlantenService } from "../../klanten.service";
import { FormCommunicatieService } from "../form-communicatie-service";

@Component({
  selector: "zac-bedrijf-zoek",
  templateUrl: "./bedrijf-zoek.component.html",
  styleUrls: ["./bedrijf-zoek.component.less"],
})
export class BedrijfZoekComponent implements OnInit, OnDestroy {
  @Output() bedrijf? = new EventEmitter<GeneratedType<"RestBedrijf">>();
  @Input() sideNav?: MatSidenav;
  @Input() syncEnabled = false;

  protected blockSearch = input<boolean>(false);

  bedrijven = new MatTableDataSource<GeneratedType<"RestBedrijf">>();
  foutmelding?: string;
  bedrijfColumns = [
    "naam",
    "kvk",
    "vestigingsnummer",
    "type",
    "adres",
    "acties",
  ] as const;
  loading = false;
  types = [
    "HOOFDVESTIGING",
    "NEVENVESTIGING",
    "RECHTSPERSOON",
  ] satisfies GeneratedType<"BedrijfType">[];
  uuid = crypto.randomUUID();
  private readonly destroy$ = new Subject<void>();

  public readonly formGroup = this.formBuilder.group({
    kvkNummer: this.formBuilder.control<number | null>(null, [
      Validators.minLength(KVK_LENGTH),
      Validators.maxLength(KVK_LENGTH),
    ]),
    naam: this.formBuilder.control<string | null>(null, [
      CustomValidators.bedrijfsnaam,
      Validators.maxLength(100),
    ]),
    vestigingsnummer: this.formBuilder.control<string | null>(null, [
      Validators.minLength(VESTIGINGSNUMMER_LENGTH),
      Validators.maxLength(VESTIGINGSNUMMER_LENGTH),
    ]),
    rsin: this.formBuilder.control<string | null>(null, [
      Validators.minLength(BSN_LENGTH),
      Validators.maxLength(BSN_LENGTH),
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
    plaats: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(50),
    ]),
    type: this.formBuilder.control<GeneratedType<"BedrijfType"> | null>(null),
  });

  constructor(
    private readonly klantenService: KlantenService,
    private readonly utilService: UtilService,
    private readonly formCommunicationService: FormCommunicatieService,
    private readonly formBuilder: FormBuilder,
    private readonly router: Router
  ) {
    this.formGroup.controls.type.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe((type) => {
          const rsinRequired = type === "RECHTSPERSOON";
          if (rsinRequired) {
            this.formGroup.controls.rsin.addValidators([Validators.required]);
          } else {
            this.formGroup.controls.rsin.removeValidators([Validators.required]);
          }
        });

    this.formGroup.controls.postcode.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe((postcode) => {
          if (postcode) {
            this.formGroup.controls.huisnummer.addValidators([
              Validators.required,
            ]);
          } else {
            this.formGroup.controls.huisnummer.removeValidators([
              Validators.required,
            ]);
          }
        });

    this.formGroup.controls.huisnummer.valueChanges
        .pipe(takeUntilDestroyed())
        .subscribe((huisnummer) => {
          if (huisnummer) {
            this.formGroup.controls.postcode.addValidators([Validators.required]);
          } else {
            this.formGroup.controls.postcode.removeValidators([
              Validators.required,
            ]);
          }
        });
  }

  ngOnInit() {
    if (!this.syncEnabled) return;

    this.formCommunicationService.itemSelected$
      .pipe(takeUntil(this.destroy$))
      .subscribe(({ selected, uuid }) => {
        if (selected && uuid !== this.uuid) {
          this.wissen();
        }
      });
  }

  zoekBedrijven() {
    this.loading = true;
    this.utilService.setLoading(true);
    this.bedrijven.data = [];
    const data = this.formGroup.value;
    this.klantenService
      .listBedrijven({
        ...data,
        kvkNummer: data.kvkNummer ? String(data.kvkNummer) : null,
      })
      .subscribe((bedrijven) => {
        this.bedrijven.data = bedrijven.resultaten ?? [];
        this.foutmelding = bedrijven.foutmelding;
        this.loading = false;
        this.utilService.setLoading(false);
      });
  }

  openBedrijfPagina(bedrijf: GeneratedType<"RestBedrijf">) {
    this.sideNav?.close();
    void this.router.navigate(["/bedrijf/", bedrijf.identificatie]);
  }

  selectBedrijf(bedrijf: GeneratedType<"RestBedrijf">) {
    this.bedrijf?.emit(bedrijf);
    this.wissen();

    if (!this.syncEnabled) return;
    this.formCommunicationService.notifyItemSelected(this.uuid);
  }

  wissen() {
    this.formGroup.reset();
    this.bedrijven.data = [];
  }

  isValid() {
    const value = this.formGroup.value;
    const hasValues = Object.values(value).some((value) => value !== null);
    return hasValues && this.formGroup.valid && !this.blockSearch();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
