/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  OnInit,
  computed,
  effect,
  inject,
  input,
  output,
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
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";
import {
  injectMutation,
  injectQuery,
} from "@tanstack/angular-query-experimental";
import moment, { Moment } from "moment";
import { firstValueFrom } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { UtilService } from "../../core/service/util.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { ZacDate } from "../../shared/form/date/date";
import { ZacDocuments } from "../../shared/form/documents/documents";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { FormHelper } from "../../shared/form/helpers";
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
    MatIconModule,
    MatToolbarModule,
    ReactiveFormsModule,
    TranslateModule,
    ZacDate,
    ZacDocuments,
    ZacFormActions,
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

  protected readonly documentenQuery = injectQuery(() => {
    const besluittypeId = this.besluit().besluittype?.id;
    return {
      queryKey: ["besluit-documenten", this.zaak().uuid, besluittypeId],
      queryFn: () =>
        firstValueFrom(
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: this.zaak().uuid,
            besluittypeUUID: besluittypeId!,
          }),
        ),
      enabled: Boolean(besluittypeId),
    };
  });

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
  private lastResponseDateMinDate: Moment | null = null;
  private documentsInitialised = false;

  protected readonly updateBesluitMutation = injectMutation(() => ({
    ...this.zakenService.updateBesluit(),
    onSuccess: () => {
      this.utilService.openSnackbar("msg.besluit.gewijzigd");
      this.besluitGewijzigd.emit(true);
    },
    onError: (error) => this.foutAfhandelingService.foutAfhandelen(error),
  }));

  constructor() {
    this.form.controls.vervaldatum.addValidators(
      this.vervaldatumNotBeforeIngangsdatum,
    );
    this.form.controls.lastResponseDate.addValidators(
      this.lastResponseDateNotBeforeMin,
    );

    effect(() => {
      const documenten = this.documentenQuery.data();
      if (!documenten || this.documentsInitialised) return;
      this.documentsInitialised = true;

      const checkedUuids = new Set(
        this.besluit().informatieobjecten?.map(({ uuid }) => uuid),
      );
      this.form.controls.documenten.setValue(
        documenten.filter(({ uuid }) => checkedUuids.has(uuid)),
        { emitEvent: false },
      );
    });

    this.form.controls.ingangsdatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => this.setVervaldatumMinDate(value));

    this.form.controls.publicationDate.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) {
          this.resetLastResponseDate();
          return;
        }

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

    if (besluit.besluittype?.publication.enabled && besluit.vervaldatum) {
      this.setLastResponseDateMinDate(moment(besluit.vervaldatum));
    }
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
      informatieobjecten:
        documenten?.map(({ uuid }) => uuid!).filter(Boolean) ?? [],
      ...(besluit.besluittype?.publication.enabled
        ? {
            publicationDate: publicationDate?.toISOString(),
            lastResponseDate: lastResponseDate?.toISOString(),
          }
        : {}),
      reden,
    });
  }

  private readonly vervaldatumNotBeforeIngangsdatum: ValidatorFn = (
    control,
  ) => {
    const ingangsdatum = this.form.controls.ingangsdatum.value;
    const vervaldatum = control.value;

    if (!moment.isMoment(ingangsdatum) || !moment.isMoment(vervaldatum)) {
      return null;
    }

    return vervaldatum.isBefore(ingangsdatum, "day")
      ? FormHelper.CustomErrorMessage(
          "msg.error.date.invalid.datum.vervaldatum-voor-ingangsdatum",
        )
      : null;
  };

  private readonly lastResponseDateNotBeforeMin: ValidatorFn = (control) => {
    const value = control.value;

    if (!this.lastResponseDateMinDate || !moment.isMoment(value)) {
      return null;
    }

    return value.isBefore(this.lastResponseDateMinDate, "day")
      ? FormHelper.CustomErrorMessage(
          "msg.error.date.invalid.datum.reactiedatum-voor-publicatiedatum",
        )
      : null;
  };

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
    this.lastResponseDateMinDate = moment(value).startOf("day");

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
    this.form.controls.lastResponseDate.updateValueAndValidity();
  }

  private resetLastResponseDate() {
    this.form.controls.lastResponseDate.setValue(null);

    if (this.lastResponseDateMinValidator) {
      this.form.controls.lastResponseDate.removeValidators(
        this.lastResponseDateMinValidator,
      );
      this.lastResponseDateMinValidator = null;
    }
    this.lastResponseDateMinDate = null;
    this.form.controls.lastResponseDate.updateValueAndValidity();
  }
}
