/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  Output,
  computed,
  effect,
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
import { MatProgressSpinner } from "@angular/material/progress-spinner";
import { MatDrawer } from "@angular/material/sidenav";
import { MatTableModule } from "@angular/material/table";
import { MatToolbar } from "@angular/material/toolbar";
import { MatTooltip } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { UtilService } from "src/app/core/service/util.service";
import { ZacAutoComplete } from "src/app/shared/form/auto-complete/auto-complete";
import { ZacInput } from "src/app/shared/form/input/input";
import { ZacSelect } from "src/app/shared/form/select/select";
import { EmptyPipe } from "src/app/shared/pipes/empty.pipe";
import { DateRangeFilterComponent } from "src/app/shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZoekenService } from "src/app/zoeken/zoeken.service";
import { injectMutation } from "../../shared/http/inject-mutation";
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
    ReactiveFormsModule,
    TranslateModule,
    MatToolbar,
    MatIconButton,
    MatIcon,
    MatProgressSpinner,
    MatDivider,
    MatButton,
    MatTableModule,
    MatExpansionModule,
    ZacSelect,
    ZacInput,
    ZacAutoComplete,
    DateRangeFilterComponent,
    EmptyPipe,
    MatTooltip,
  ],
})
export class ZaakLinkComponent {
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

  private readonly searchParams = signal<
    Parameters<ZoekenService["findLinkableZaken"]>[0] | null
  >(null);

  protected readonly casesQuery = injectQuery(() => {
    const searchParams = this.searchParams();
    if (!searchParams) {
      return {
        queryKey: ["koppelbare-zaken", "nog-niet-gezocht"],
        enabled: false,
      };
    }
    return this.zoekenService.findLinkableZaken(searchParams);
  });

  protected readonly cases = computed(
    () => this.casesQuery.data()?.resultaten ?? [],
  );
  protected readonly totalCases = computed(
    () => this.casesQuery.data()?.totaal ?? 0,
  );
  protected readonly hasSearched = computed(() => this.searchParams() !== null);

  protected readonly caseColumns = [
    "identificatie",
    "zaaktypeOmschrijving",
    "statustypeOmschrijving",
    "omschrijving",
    "acties",
  ] as const;

  protected caseRelationOptionsList = [
    caseRelationOption("DEELZAAK"),
    caseRelationOption("HOOFDZAAK"),
    caseRelationOption("GERELATEERD"),
  ] as const;

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

  private readonly caseTypesQuery = injectQuery(() =>
    this.zakenService.listZaaktypesToLinkQuery(),
  );
  protected readonly caseTypes = computed(
    () => this.caseTypesQuery.data() ?? [],
  );

  protected readonly startdatum = signal<GeneratedType<"RestDatumRange">>({
    van: null,
    tot: null,
  });
  protected readonly einddatum = signal<GeneratedType<"RestDatumRange">>({
    van: null,
    tot: null,
  });

  constructor() {
    effect(() => {
      this.caseRelationType();
      this.clearSearchResult();
    });

    effect(() => {
      this.utilService.setLoading(this.casesQuery.isFetching());
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

    this.searchParams.set({
      zaakUuid: this.zaak().uuid,
      zoekZaakIdentifier: caseNumberToSearchFor,
      zoekZaakOmschrijving: caseDescriptionToSearchFor,
      zoekZaakTypeOmschrijving: caseTypeToSearchFor?.omschrijving,
      relationType: caseRelationType.value,
      startdatum: { ...this.startdatum() },
      einddatum: { ...this.einddatum() },
    });
  }

  protected selectCase(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    if (row.nietKoppelbaarReden) return;
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

  protected readonly relationTypeUnavailableReason = computed(() => {
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

  protected linkButtonDisabled(
    row: GeneratedType<"RestZaakKoppelenZoekObject">,
  ): boolean {
    return !!row.nietKoppelbaarReden || this.isLinking(row);
  }

  protected rowTooltip(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    return row.nietKoppelbaarReden
      ? `zaak.koppelen.niet-koppelbaar.${row.nietKoppelbaarReden}`
      : "actie.zaak.koppelen";
  }

  protected rowTooltipParams(row: GeneratedType<"RestZaakKoppelenZoekObject">) {
    const isCurrentZaakHoofdzaak =
      this.form.controls.caseRelationType.value?.value === "DEELZAAK";
    const currentZaaktype = this.zaak().zaaktype.omschrijving;
    return {
      hoofdzaakZaaktype: isCurrentZaakHoofdzaak
        ? currentZaaktype
        : row.zaaktypeOmschrijving,
      deelzaakZaaktype: isCurrentZaakHoofdzaak
        ? row.zaaktypeOmschrijving
        : currentZaaktype,
      zaaktype: row.zaaktypeOmschrijving,
    };
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
    this.searchParams.set(null);
  }
}
