/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgIf } from "@angular/common";
import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  inject,
} from "@angular/core";
import {
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatButton, MatIconButton } from "@angular/material/button";
import { MatDivider } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIcon } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { MatToolbar } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { Subject, takeUntil } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { ZacInput } from "src/app/shared/form/input/input";
import { ZacSelect } from "src/app/shared/form/select/select";
import { EmptyPipe } from "src/app/shared/pipes/empty.pipe";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-link",
  templateUrl: "./zaak-link.component.html",
  styleUrls: ["./zaak-link.component.less"],
  standalone: true,
  imports: [
    NgClass,
    NgIf,
    ReactiveFormsModule,
    TranslateModule,
    MatToolbar,
    MatIconButton,
    MatIcon,
    MatDivider,
    MatButton,
    MatTableModule,
    MatSortModule,
    MatExpansionModule,
    ZacSelect,
    ZacInput,
    EmptyPipe,
  ],
})
export class ZaakLinkComponent implements OnDestroy {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() zaakLinked = new EventEmitter();

  private readonly formBuilder = inject(FormBuilder);
  private readonly zoekenService = inject(ZoekenService);
  private readonly zakenService = inject(ZakenService);
  private readonly utilService = inject(UtilService);

  private ngDestroy = new Subject<void>();

  protected cases = new MatTableDataSource<
    GeneratedType<"RestZaakKoppelenZoekObject">
  >();
  protected totalCases = 0;
  protected readonly caseColumns = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "omschrijving",
    "acties",
  ] as const;
  protected loading = false;

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
    return !row.isKoppelbaar || row.identificatie === this.zaak.identificatie;
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
}
