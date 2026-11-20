/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import {
  Component,
  computed,
  effect,
  inject,
  input,
  OnInit,
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
import { MatExpansionPanelActionRow } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { UtilService } from "../../core/service/util.service";
import {
  addDays,
  DateInput,
  isAfter,
  isPickedDate,
  PickedDate,
  startOfDay,
  toApiDate,
  toPickedDate,
} from "../../date-time";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { ZacDate } from "../../shared/form/date/date";
import { ZacDocuments } from "../../shared/form/documents/documents";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { FormHelper } from "../../shared/form/helpers";
import { ZacInput } from "../../shared/form/input/input";
import { ZacTextarea } from "../../shared/form/textarea/textarea";
import { injectMutation } from "../../shared/http/inject-mutation";
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
    MatExpansionPanelActionRow,
    MatIconModule,
    MatToolbarModule,
    ReactiveFormsModule,
    TranslatePipe,
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
  private readonly formBuilder = inject(FormBuilder);

  protected readonly besluit = input.required<GeneratedType<"RestBesluit">>();
  protected readonly zaak = input.required<GeneratedType<"RestZaak">>();
  protected readonly sideNav = input.required<MatDrawer>();
  protected readonly besluitGewijzigd = output<boolean>();

  protected readonly showPublicationSection = computed(
    () => this.besluit().besluittype?.publication.isEnabled ?? false,
  );

  protected readonly documentenQuery = injectQuery(() => {
    const besluittypeId = this.besluit().besluittype?.id;
    return {
      ...this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
        zaakUUID: this.zaak().uuid,
        besluittypeUUID: besluittypeId!,
      }),
      enabled: Boolean(besluittypeId),
    };
  });

  protected readonly form = this.formBuilder.group({
    besluittype: this.formBuilder.control<string | null>({
      value: null,
      disabled: true,
    }),
    ingangsdatum: this.formBuilder.control<PickedDate | null>(
      null,
      Validators.required,
    ),
    vervaldatum: this.formBuilder.control<PickedDate | null>(null),
    toelichting: this.formBuilder.control<string | null>(
      null,
      Validators.maxLength(1000),
    ),
    documenten: this.formBuilder.control<
      GeneratedType<"RestEnkelvoudigInformatieobject">[]
    >([]),
    publicationDate: this.formBuilder.control<PickedDate | null>(null),
    lastResponseDate: this.formBuilder.control<PickedDate | null>(null),
    reden: this.formBuilder.control<string | null>(null, [
      Validators.required,
      Validators.maxLength(80),
    ]),
  });

  private vervaldatumMinValidator: ValidatorFn | null = null;
  private lastResponseDateMinValidator: ValidatorFn | null = null;
  private lastResponseDateMinDate: PickedDate | null = null;
  private documentsInitialised = false;

  protected readonly updateBesluitMutation = injectMutation(
    () => this.zakenService.updateBesluit(),
    {
      onSuccess: () => this.besluitGewijzigd.emit(true),
    },
  );

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
      .subscribe((value) => {
        this.setVervaldatumMinDate(value);
        this.form.controls.vervaldatum.markAsTouched();
      });

    this.form.controls.publicationDate.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) {
          this.resetLastResponseDate();
          return;
        }

        const responseTermDays =
          this.besluit().besluittype?.publication.responseTermDays ?? 0;
        const lastResponseDate = addDays(value, responseTermDays);

        this.form.controls.lastResponseDate.setValue(lastResponseDate);
        this.setLastResponseDateMinDate(lastResponseDate);
      });
  }

  ngOnInit() {
    const besluit = this.besluit();

    this.form.patchValue(
      {
        besluittype: besluit.besluittype?.naam ?? null,
        ingangsdatum: toPickedDate(besluit.ingangsdatum),
        vervaldatum: toPickedDate(besluit.vervaldatum),
        toelichting: besluit.toelichting ?? null,
        publicationDate: toPickedDate(besluit.publicationDate),
        lastResponseDate: toPickedDate(besluit.lastResponseDate),
      },
      { emitEvent: false },
    );

    this.setVervaldatumMinDate(this.form.controls.ingangsdatum.value);

    if (besluit.besluittype?.publication.isEnabled && besluit.vervaldatum) {
      this.setLastResponseDateMinDate(besluit.vervaldatum);
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
      ingangsdatum: toApiDate(ingangsdatum),
      vervaldatum: toApiDate(vervaldatum),
      informatieobjecten:
        documenten?.map(({ uuid }) => uuid!).filter(Boolean) ?? [],
      ...(besluit.besluittype?.publication.isEnabled
        ? {
            publicationDate: toApiDate(publicationDate),
            lastResponseDate: toApiDate(lastResponseDate),
          }
        : {}),
      reden: reden!,
    });
  }

  private readonly vervaldatumNotBeforeIngangsdatum: ValidatorFn = (
    control,
  ) => {
    const ingangsdatum = this.form.controls.ingangsdatum.value;
    const vervaldatum = control.value;

    if (!isPickedDate(ingangsdatum) || !isPickedDate(vervaldatum)) {
      return null;
    }

    return isAfter(ingangsdatum, vervaldatum, "day")
      ? FormHelper.CustomErrorMessage(
          "msg.error.date.invalid.datum.vervaldatum-voor-ingangsdatum",
        )
      : null;
  };

  private readonly lastResponseDateNotBeforeMin: ValidatorFn = (control) => {
    const value = control.value;

    if (!this.lastResponseDateMinDate || !isPickedDate(value)) {
      return null;
    }

    return isAfter(this.lastResponseDateMinDate, value, "day")
      ? FormHelper.CustomErrorMessage(
          "msg.error.date.invalid.datum.reactiedatum-voor-publicatiedatum",
        )
      : null;
  };

  private setVervaldatumMinDate(value: PickedDate | null) {
    if (this.vervaldatumMinValidator) {
      this.form.controls.vervaldatum.removeValidators(
        this.vervaldatumMinValidator,
      );
      this.vervaldatumMinValidator = null;
    }

    if (value) {
      this.vervaldatumMinValidator = Validators.min(
        startOfDay(value).valueOf(),
      );
      this.form.controls.vervaldatum.addValidators(
        this.vervaldatumMinValidator,
      );
    }

    this.form.controls.vervaldatum.updateValueAndValidity();
  }

  private setLastResponseDateMinDate(value: DateInput) {
    this.lastResponseDateMinDate = startOfDay(value);

    if (this.lastResponseDateMinValidator) {
      this.form.controls.lastResponseDate.removeValidators(
        this.lastResponseDateMinValidator,
      );
    }

    this.lastResponseDateMinValidator = Validators.min(
      startOfDay(value).valueOf(),
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
