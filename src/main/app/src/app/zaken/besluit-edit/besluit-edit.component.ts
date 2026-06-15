/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  OnInit,
  computed,
  inject,
  input,
  output,
  signal,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import {
  FormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import moment, { Moment } from "moment";
import { lastValueFrom } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { ZacDate } from "../../shared/form/date/date";
import { ZacDocuments } from "../../shared/form/documents/documents";
import { ZacInput } from "../../shared/form/input/input";
import { ZacTextarea } from "../../shared/form/textarea/textarea";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-besluit-edit",
  templateUrl: "./besluit-edit.component.html",
  styleUrls: ["./besluit-edit.component.less"],
  standalone: true,
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatExpansionModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    ReactiveFormsModule,
    TranslateModule,
    ZacDate,
    ZacDocuments,
    ZacInput,
    ZacTextarea,
  ],
})
export class BesluitEditComponent implements OnInit {
  private readonly zakenService = inject(ZakenService);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly utilService = inject(UtilService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly besluit = input.required<GeneratedType<"RestBesluit">>();
  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly besluitGewijzigd = output<boolean>();

  protected readonly showPublicationSection = computed(
    () => this.besluit().besluittype?.publication.enabled ?? false,
  );

  protected readonly documenten = signal<
    GeneratedType<"RestEnkelvoudigInformatieobject">[]
  >([]);

  protected readonly form = this.formBuilder.group({
    besluittype: this.formBuilder.control<string | null>({
      value: null,
      disabled: true,
    }),
    ingangsdatum: this.formBuilder.control<Moment | null>(
      null,
      Validators.required,
    ),
    vervaldatum: this.formBuilder.control<Moment | null>(null),
    toelichting: this.formBuilder.control<string | null>(
      null,
      Validators.maxLength(1000),
    ),
    documenten: this.formBuilder.control<
      GeneratedType<"RestEnkelvoudigInformatieobject">[]
    >([]),
    publicationDate: this.formBuilder.control<Moment | null>(null),
    lastResponseDate: this.formBuilder.control<Moment | null>(null),
    reden: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(80),
    ]),
  });

  private vervaldatumMinValidator: ValidatorFn | null = null;
  private lastResponseDateMinValidator: ValidatorFn | null = null;

  protected readonly updateBesluitMutation = injectMutation(() => ({
    mutationFn: (data: GeneratedType<"RestBesluitChangeData">) =>
      lastValueFrom(this.zakenService.updateBesluit(data)),
    onSuccess: () => {
      this.utilService.openSnackbar("msg.besluit.gewijzigd");
      this.besluitGewijzigd.emit(true);
    },
    onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
  }));

  constructor() {
    this.form.controls.ingangsdatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => this.setVervaldatumMinDate(value));

    this.form.controls.publicationDate.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) return;

        const responseTermDays =
          this.besluit().besluittype?.publication.responseTermDays ?? 0;
        const lastResponseDate = moment(value)
          .clone()
          .add(responseTermDays, "days");

        this.form.controls.lastResponseDate.setValue(lastResponseDate);
        this.setLastResponseDateMinDate(lastResponseDate);
      });
  }

  ngOnInit() {
    const besluit = this.besluit();

    this.form.patchValue(
      {
        besluittype: besluit.besluittype?.naam ?? null,
        ingangsdatum: besluit.ingangsdatum
          ? moment(besluit.ingangsdatum)
          : null,
        vervaldatum: besluit.vervaldatum ? moment(besluit.vervaldatum) : null,
        toelichting: besluit.toelichting ?? null,
        publicationDate: besluit.publicationDate
          ? moment(besluit.publicationDate)
          : null,
        lastResponseDate: besluit.lastResponseDate
          ? moment(besluit.lastResponseDate)
          : null,
      },
      { emitEvent: false },
    );

    this.setVervaldatumMinDate(this.form.controls.ingangsdatum.value);

    const besluittypeId = besluit.besluittype?.id;
    if (!besluittypeId) return;

    this.listInformatieObjecten(besluittypeId).subscribe((documenten) => {
      this.documenten.set(documenten);

      const checkedUuids = new Set(
        besluit.informatieobjecten?.map(({ uuid }) => uuid),
      );
      this.form.controls.documenten.setValue(
        documenten.filter(({ uuid }) => checkedUuids.has(uuid)),
      );
    });
  }

  protected submit() {
    const besluit = this.besluit();
    const {
      ingangsdatum,
      vervaldatum,
      toelichting,
      documenten,
      publicationDate,
      lastResponseDate,
      reden,
    } = this.form.getRawValue();

    this.updateBesluitMutation.mutate({
      besluitUuid: besluit.uuid,
      toelichting,
      ingangsdatum: ingangsdatum?.toISOString(),
      vervaldatum: vervaldatum?.toISOString(),
      informatieobjecten: documenten?.map(({ uuid }) => uuid!) ?? [],
      ...(besluit.besluittype?.publication.enabled
        ? {
            publicationDate: publicationDate?.toISOString(),
            lastResponseDate: lastResponseDate?.toISOString(),
          }
        : {}),
      reden,
    });
  }

  private listInformatieObjecten(besluittypeUUID: string) {
    return this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
      zaakUUID: this.zaak().uuid,
      besluittypeUUID,
    });
  }

  private setVervaldatumMinDate(value: Moment | null) {
    if (this.vervaldatumMinValidator) {
      this.form.controls.vervaldatum.removeValidators(
        this.vervaldatumMinValidator,
      );
      this.vervaldatumMinValidator = null;
    }

    if (value) {
      this.vervaldatumMinValidator = Validators.min(
        moment(value).startOf("day").valueOf(),
      );
      this.form.controls.vervaldatum.addValidators(
        this.vervaldatumMinValidator,
      );
    }

    this.form.controls.vervaldatum.updateValueAndValidity();
  }

  private setLastResponseDateMinDate(value: Moment) {
    if (this.lastResponseDateMinValidator) {
      this.form.controls.lastResponseDate.removeValidators(
        this.lastResponseDateMinValidator,
      );
    }

    this.lastResponseDateMinValidator = Validators.min(
      moment(value).startOf("day").valueOf(),
    );
    this.form.controls.lastResponseDate.addValidators(
      this.lastResponseDateMinValidator,
    );
    this.form.controls.lastResponseDate.updateValueAndValidity({
      emitEvent: false,
    });
  }
}
