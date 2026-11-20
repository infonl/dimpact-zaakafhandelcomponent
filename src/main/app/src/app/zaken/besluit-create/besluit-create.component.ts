/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, input, output, signal } from "@angular/core";
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
import { MatDrawer } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import {
  addDays,
  now,
  PickedDate,
  startOfDay,
  toApiDate,
  today,
} from "../../date-time";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { trackControl } from "../../shared/form/control-state";
import { ZacDate } from "../../shared/form/date/date";
import { ZacDocuments } from "../../shared/form/documents/documents";
import { ZacFormActions } from "../../shared/form/form-actions/form-actions.component";
import { ZacSelect } from "../../shared/form/select/select";
import { ZacTextarea } from "../../shared/form/textarea/textarea";
import { injectMutation } from "../../shared/http/inject-mutation";
import { runQuery } from "../../shared/http/run-query";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-besluit-create",
  templateUrl: "./besluit-create.component.html",
  styleUrls: ["./besluit-create.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule,
    MatExpansionModule,
    TranslatePipe,
    ZacSelect,
    ZacDate,
    ZacTextarea,
    ZacFormActions,
    ZacDocuments,
  ],
})
export class BesluitCreateComponent {
  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly sideNav = input.required<MatDrawer>();
  readonly besluitVastgelegd = output<boolean>();

  private readonly zakenService = inject(ZakenService);
  private readonly informatieObjectenService = inject(
    InformatieObjectenService,
  );
  private readonly formBuilder = inject(FormBuilder);

  protected readonly besluittypesQuery = injectQuery(() =>
    this.zakenService.listBesluittypes(this.zaak().zaaktype.uuid),
  );
  protected readonly documents = signal<
    GeneratedType<"RestEnkelvoudigInformatieobject">[]
  >([]);

  protected form = this.formBuilder.group({
    besluit: this.formBuilder.control<GeneratedType<"RestBesluitType"> | null>(
      null,
      Validators.required,
    ),
    toelichting: this.formBuilder.control<string | null>(
      null,
      Validators.maxLength(1000),
    ),
    ingangsdatum: this.formBuilder.control<PickedDate | null>(
      now(),
      Validators.required,
    ),
    vervaldatum: this.formBuilder.control<PickedDate | null>(null, [
      Validators.min(today().valueOf()),
    ]),
    publicationEnabled: this.formBuilder.control(false),
    publicatiedatum: this.formBuilder.control<PickedDate | null>(now()),
    uiterlijkereactiedatum: this.formBuilder.control<PickedDate | null>(null),
    documenten: this.formBuilder.control<
      GeneratedType<"RestEnkelvoudigInformatieobject">[]
    >([]),
  });

  private readonly readForm = trackControl(signal(this.form));
  protected readonly publication = this.readForm(
    (form) => form.controls.besluit.value?.publication,
  );

  // For dynamically add minimum date validators
  private uiterlijkereactiedatumMinDateValidator: ValidatorFn | null = null;
  private vervaldatumMinDateValidator: ValidatorFn | null = null;

  protected readonly createBesluitMutation = injectMutation(
    () => this.zakenService.createBesluit(),
    {
      onSuccess: () => this.besluitVastgelegd.emit(true),
    },
  );

  private readonly queryClient = inject(QueryClient);

  constructor() {
    this.form.controls.ingangsdatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (this.vervaldatumMinDateValidator) {
          this.form.controls.vervaldatum.removeValidators([
            this.vervaldatumMinDateValidator,
          ]);
          this.vervaldatumMinDateValidator = null;
        }

        if (value) {
          this.vervaldatumMinDateValidator = Validators.min(
            startOfDay(value).valueOf(),
          );
          this.form.controls.vervaldatum.addValidators([
            this.vervaldatumMinDateValidator,
          ]);
          return;
        }
      });

    this.form.controls.besluit.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) return;

        runQuery(
          this.queryClient,
          this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
            zaakUUID: this.zaak().uuid,
            besluittypeUUID: value.id,
          }),
        ).subscribe((documents) => {
          this.documents.set(documents);
        });

        this.form.controls.publicationEnabled.setValue(
          value.publication.isEnabled ?? null,
        );
        if (!value.publication.isEnabled) return;
        this.setUiterlijkereactiedatum(
          now(),
          value.publication.responseTermDays,
        );
      });

    this.form.controls.publicatiedatum.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) {
          this.form.controls.uiterlijkereactiedatum.setValue(null);
          this.form.controls.uiterlijkereactiedatum.clearValidators();
          return;
        }

        this.setUiterlijkereactiedatum(
          value,
          this.form.controls.besluit.value?.publication.responseTermDays,
        );
      });
  }

  private setUiterlijkereactiedatum(
    date: PickedDate,
    responseTermDays?: number | null,
  ) {
    const uiterlijkereactiedatum = addDays(date, responseTermDays ?? 0);
    this.form.controls.uiterlijkereactiedatum.setValue(uiterlijkereactiedatum);

    if (this.uiterlijkereactiedatumMinDateValidator) {
      this.form.controls.uiterlijkereactiedatum.removeValidators([
        this.uiterlijkereactiedatumMinDateValidator,
      ]);
    }
    this.uiterlijkereactiedatumMinDateValidator = Validators.min(
      startOfDay(uiterlijkereactiedatum).valueOf(),
    );
    this.form.controls.uiterlijkereactiedatum.addValidators([
      this.uiterlijkereactiedatumMinDateValidator,
    ]);
  }

  submit() {
    const { value } = this.form;

    this.createBesluitMutation.mutate({
      ...value,
      zaakUuid: this.zaak().uuid,
      besluittypeUuid: value.besluit!.id,
      ingangsdatum: toApiDate(value.ingangsdatum),
      vervaldatum: toApiDate(value.vervaldatum),
      informatieobjecten: value.documenten
        ?.map(({ uuid }) => uuid!)
        .filter(Boolean),
      ...(value.publicationEnabled
        ? {
            publicationDate: toApiDate(value.publicatiedatum),
            lastResponseDate: toApiDate(value.uiterlijkereactiedatum),
          }
        : {}),
    });
  }
}
