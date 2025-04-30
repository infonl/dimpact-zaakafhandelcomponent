/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
} from "@angular/core";
import { FormBuilder, FormControl, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { Zaak } from "../model/zaak";
import { ZaakRelatietype } from "../model/zaak-relatietype";
import { ZakenService } from "../zaken.service";

/*
 enum/type below should come from our GeneratedType 
*/

@Component({
  selector: "zac-zaak-link",
  templateUrl: "./zaak-link.component.html",
  styleUrls: ["./zaak-link.component.less"],
})
export class ZaakLinkComponent implements OnDestroy {
  @Input({ required: true }) zaak!: Zaak; // GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() zaakLinked = new EventEmitter();

  public intro = "";
  public loading = false;

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

  protected caseRelationOptionsList = [
    {
      label: `zaak.koppelen.link.type.${ZaakRelatietype.HOOFDZAAK}`,
      value: ZaakRelatietype.HOOFDZAAK,
    },
    {
      label: `zaak.koppelen.link.type.${ZaakRelatietype.DEELZAAK}`,
      value: ZaakRelatietype.DEELZAAK,
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

  private ngDestroy = new Subject<void>();

  constructor(
    private zoekenService: ZoekenService,
    private zakenService: ZakenService,
    private translate: TranslateService,
    private utilService: UtilService,
    private readonly formBuilder: FormBuilder,
  ) {
    this.form.controls.caseToSearchFor.disable();

    this.form.controls.caseRelationType.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe(() => {
        this.form.controls.caseToSearchFor.reset();
        this.form.controls.caseToSearchFor.enable();
        this.clearSearchResult();
      });

    this.form.controls.caseToSearchFor.valueChanges
      .pipe(takeUntil(this.ngDestroy))
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
        this.zaak.uuid,
        this.form.controls.caseToSearchFor.value ?? "",
        this.form.controls.caseRelationType.value!.value ?? "",
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

    const caseLinkDetails: GeneratedType<"RestZaakLinkData"> = {
      zaakUuid: this.zaak.uuid,
      teKoppelenZaakUuid: row.id,
      relatieType: this.form.controls.caseRelationType.value.value,
    };

    this.zakenService.koppelZaak(caseLinkDetails).subscribe({
      next: () => {
        this.utilService.openSnackbar("msg.zaak.gekoppeld", {
          case: row.identificatie,
        });
        this.zaakLinked.emit();
        this.close();
      },
      error: () => {
        this.loading = false;
        this.utilService.setLoading(false);
      },
    });
  }

  protected rowDisabled(
    row: GeneratedType<"RestZaakKoppelenZoekObject">,
  ): boolean {
    return (
      !row.documentKoppelbaar || row.identificatie === this.zaak.identificatie
    );
  }

  protected close() {
    void this.sideNav.close();
    this.reset();
  }

  protected reset() {
    this.form.reset();
    this.clearSearchResult();
  }

  protected clearSearchResult() {
    this.cases.data = [];
    this.totalCases = 0;
    this.loading = false;
    this.utilService.setLoading(false);
  }

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }

  protected readonly ZaakRelatietype = ZaakRelatietype;
}
