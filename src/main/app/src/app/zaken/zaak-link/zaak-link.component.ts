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
  computed,
  inject,
  input,
  signal,
} from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
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
import { QueryClient } from "@tanstack/angular-query-experimental";
import { Subject, takeUntil } from "rxjs";
import { UtilService } from "src/app/core/service/util.service";
import { ZacAutoComplete } from "src/app/shared/form/auto-complete/auto-complete";
import { ZacInput } from "src/app/shared/form/input/input";
import { ZacSelect } from "src/app/shared/form/select/select";
import { EmptyPipe } from "src/app/shared/pipes/empty.pipe";
import { DateRangeFilterComponent } from "src/app/shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { injectMutation } from "../../shared/http/inject-mutation";
import { runQuery } from "../../shared/http/run-query";
import { ZakenService } from "../zaken.service";

const caseRelationOption = <T extends GeneratedType<"RelatieType">>(value: T) =>
  ({
    label: `zaak.koppelen.link.type.${value}`,
    value,
  }) as const;

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
    ZacAutoComplete,
    DateRangeFilterComponent,
    EmptyPipe,
  ],
})
export class ZaakLinkComponent implements OnDestroy {
  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  @Input({ required: true }) sideNav!: MatDrawer;
  @Output() zaakLinked = new EventEmitter<void>();

  private readonly formBuilder = inject(FormBuilder);
  private readonly zoekenService = inject(ZoekenService);
  private readonly zakenService = inject(ZakenService);
  private readonly utilService = inject(UtilService);

  protected readonly koppelZaakMutation = injectMutation(() =>
    this.zakenService.koppelZaakMutation(),
  );

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
  protected hasSearched = false;

  protected caseRelationOptionsList = [
    caseRelationOption("DEELZAAK"),
    caseRelationOption("HOOFDZAAK"),
    caseRelationOption("GERELATEERD"),
  ];

  protected readonly form = this.formBuilder.group({
    caseRelationType: new FormControl<
      (typeof this.caseRelationOptionsList)[number] | null
    >(null, [Validators.required]),
    caseNumberToSearchFor: new FormControl<string>(""),
    caseDescriptionToSearchFor: new FormControl<string>("", [
      Validators.minLength(2),
    ]),
    caseTypeToSearchFor: new FormControl<GeneratedType<"RestZaaktype"> | null>(
      null,
    ),
  });

  protected caseTypes = this.zakenService.listZaaktypesToLink();

  protected readonly startdatum = signal<GeneratedType<"RestDatumRange">>({
    van: null,
    tot: null,
  });
  protected readonly einddatum = signal<GeneratedType<"RestDatumRange">>({
    van: null,
    tot: null,
  });

  private readonly queryClient = inject(QueryClient);

  constructor() {
    this.form.controls.caseRelationType.valueChanges
      .pipe(takeUntil(this.ngDestroy))
      .subscribe(() => {
        this.clearSearchResult();
      });
  }

  protected searchCases() {
    const {
      caseNumberToSearchFor,
      caseDescriptionToSearchFor,
      caseRelationType,
      caseTypeToSearchFor,
    } = this.form.getRawValue();

    if (!caseRelationType?.value) return;

    this.loading = true;
    this.utilService.setLoading(true);
    runQuery(
      this.queryClient,
      this.zoekenService.findLinkableZaken({
        zaakUuid: this.zaak().uuid,
        zoekZaakIdentifier: caseNumberToSearchFor,
        zoekZaakOmschrijving: caseDescriptionToSearchFor,
        zoekZaakTypeOmschrijving: caseTypeToSearchFor?.omschrijving,
        relationType: caseRelationType.value,
        startdatum: { ...this.startdatum() },
        einddatum: { ...this.einddatum() },
      }),
    ).subscribe({
      next: (result) => {
        this.cases.data = result.resultaten ?? [];
        this.totalCases = result.totaal ?? 0;
        this.hasSearched = true;
        this.loading = false;
        this.utilService.setLoading(false);
      },
      error: () => {
        this.loading = false;
        this.utilService.setLoading(false);
      },
    });
  }

  protected selectCase(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    if (this.koppelZaakMutation.isPending()) return;
    if (!row.id || !this.form.controls.caseRelationType.value?.value) return;

    this.koppelZaakMutation.mutate(
      {
        zaakUuid: this.zaak().uuid,
        teKoppelenZaakUuid: row.id,
        relatieType: this.form.controls.caseRelationType.value.value,
      },
      {
        onSuccess: () => {
          this.utilService.openSnackbar("msg.zaak.gekoppeld", {
            case: row.identificatie,
          });
          this.zaakLinked.emit();
          this.close();
        },
      },
    );
  }

  private readonly caseRelationType = toSignal(
    this.form.controls.caseRelationType.valueChanges,
    { initialValue: this.form.controls.caseRelationType.value },
  );

  private readonly formValue = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly hasSearchCriteria = computed(() => {
    const {
      caseNumberToSearchFor,
      caseDescriptionToSearchFor,
      caseTypeToSearchFor,
    } = this.formValue();
    return Boolean(
      caseNumberToSearchFor ||
        caseDescriptionToSearchFor ||
        caseTypeToSearchFor ||
        this.startdatum().van ||
        this.startdatum().tot ||
        this.einddatum().van ||
        this.einddatum().tot,
    );
  });

  protected dateRangeChanged(
    range: ReturnType<typeof this.startdatum>,
    target: typeof this.startdatum,
  ) {
    target.set({ ...range });
  }

  protected readonly ownZaakBlockedReason = computed(() => {
    switch (this.caseRelationType()?.value) {
      case "HOOFDZAAK":
        if (this.zaak().isDeelzaak) {
          return "zaak.koppelen.geblokkeerd.al-deelzaak-van-andere-zaak";
        }
        if (this.zaak().isHoofdzaak) {
          return "zaak.koppelen.geblokkeerd.heeft-al-deelzaken";
        }
        return null;
      case "DEELZAAK":
        return this.zaak().isDeelzaak
          ? "zaak.koppelen.geblokkeerd.is-zelf-deelzaak"
          : null;
      default:
        return null;
    }
  });

  protected rowDisabled(
    row: GeneratedType<"RestZaakKoppelenZoekObject">,
  ): boolean {
    return !row.isKoppelbaar || row.identificatie === this.zaak().identificatie;
  }

  protected isLinking(
    row: GeneratedType<"RestZaakKoppelenZoekObject">,
  ): boolean {
    return (
      this.koppelZaakMutation.isPending() &&
      this.koppelZaakMutation.variables()?.teKoppelenZaakUuid === row.id
    );
  }

  protected close() {
    void this.sideNav.close();
    this.reset();
  }

  protected reset() {
    this.form.reset();
    this.startdatum.set({ van: null, tot: null });
    this.einddatum.set({ van: null, tot: null });
    this.clearSearchResult();
  }

  protected clearSearchResult() {
    this.cases.data = [];
    this.totalCases = 0;
    this.hasSearched = false;
    this.loading = false;
    this.utilService.setLoading(false);
  }

  ngOnDestroy() {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
