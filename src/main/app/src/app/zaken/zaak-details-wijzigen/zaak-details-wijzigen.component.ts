/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  OnInit,
  signal,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatDrawer, MatSidenavModule } from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { injectMutation } from "@tanstack/angular-query-experimental";
import moment, { Moment } from "moment";
import {
  defaultIfEmpty,
  EMPTY,
  firstValueFrom,
  map,
  Observable,
  of,
} from "rxjs";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { UtilService } from "src/app/core/service/util.service";
import { Vertrouwelijkheidaanduiding } from "src/app/informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { ZacDate } from "src/app/shared/form/date/date";
import { ZacInput } from "src/app/shared/form/input/input";
import { ZacSelect } from "src/app/shared/form/select/select";
import { ZacTextarea } from "src/app/shared/form/textarea/textarea";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { IdentityService } from "../../identity/identity.service";
import { FormHelper } from "../../shared/form/helpers";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-case-details-edit",
  templateUrl: "./zaak-details-wijzigen.component.html",
  standalone: true,
  imports: [
    MatButtonModule,
    MatDividerModule,
    MatExpansionModule,
    MatIconModule,
    MatSidenavModule,
    MatToolbarModule,
    ReactiveFormsModule,
    TranslatePipe,
    ZacDate,
    ZacInput,
    ZacSelect,
    ZacTextarea,
  ],
})
export class CaseDetailsEditComponent implements OnInit {
  private readonly zakenService = inject(ZakenService);
  private readonly referentieTabelService = inject(ReferentieTabelService);
  private readonly utilService = inject(UtilService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly identityService = inject(IdentityService);
  private readonly translateService = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  readonly zaak = input.required<GeneratedType<"RestZaak">>();
  readonly loggedInUser = input.required<GeneratedType<"RestLoggedInUser">>();
  readonly sideNav = input.required<MatDrawer>();

  protected groups: Observable<GeneratedType<"RestGroup">[]> = of([]);
  protected readonly groupDisplayValue = (
    group: GeneratedType<"RestGroup">,
  ): string =>
    group.active === false
      ? `${group.naam ?? ""} (${this.translateService.instant("inactief").toLowerCase()})`
      : (group.naam ?? "");
  protected readonly users = signal<GeneratedType<"RestUser">[]>([]);
  protected readonly communicationChannels = signal<string[]>([]);
  protected confidentialityDesignations = this.utilService.getEnumAsSelectList(
    "vertrouwelijkheidaanduiding",
    Vertrouwelijkheidaanduiding,
  );

  protected readonly form = this.formBuilder.group({
    groep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(null, [
      Validators.required,
    ]),
    behandelaar: this.formBuilder.control<GeneratedType<"RestUser"> | null>(
      null,
    ),
    communicatiekanaal: this.formBuilder.control<string | null>(null, [
      Validators.required,
    ]),
    startdatum: this.formBuilder.control<Moment>(moment(), [
      Validators.required,
    ]),
    einddatumGepland: this.formBuilder.control<Moment | null>(null),
    uiterlijkeEinddatumAfdoening: this.formBuilder.control<Moment | null>(null),
    vertrouwelijkheidaanduiding: this.formBuilder.control<{
      label: string;
      value: string;
    } | null>(null, [Validators.required]),
    omschrijving: this.formBuilder.control("", [
      Validators.required,
      Validators.maxLength(80),
    ]),
    toelichting: this.formBuilder.control<string | null>(null, [
      Validators.maxLength(1000),
    ]),
    reden: this.formBuilder.control("", [
      Validators.required,
      Validators.maxLength(80),
    ]),
  });

  protected readonly updateZaakMutation = injectMutation(() => ({
    ...this.zakenService.updateMutation(this.zaak().uuid),
    onSuccess: () => {
      void this.sideNav().close();
    },
    onError: (error) => {
      console.error(
        this.translateService.instant(
          "console.error.case-details-change.editing",
        ),
        error,
      );
    },
  }));

  protected readonly patchBehandelaarMutation = injectMutation(() => ({
    mutationFn: () => {
      const value = this.form.getRawValue();
      return firstValueFrom(
        this.patchBehandelaar(value, value.reden ?? "").pipe(
          defaultIfEmpty(null),
        ),
      );
    },
    onError: (error) => {
      console.error(
        this.translateService.instant(
          "console.error.case-details-change.assignment",
        ),
        error,
      );
    },
  }));

  /**
   * Locks the submit button for the whole save and keeps it locked after
   * success (so no second submit slips in before the sidenav closes), but
   * unlocks again on failure so the user can retry — derived from the mutations
   * themselves, no separate "submitting" flag to keep in sync.
   *
   * The two mutations run in sequence, so `updateZaak` can only error *after*
   * the patch succeeded; thus `isError()` implies patch `isSuccess()`. The XOR
   * (`!==`) is true across "patched → saving → saved" (locked) and false only
   * when the save failed (both true) or nothing ran yet (both false) (unlocked).
   */
  protected readonly isSaveLocked = computed(
    () =>
      this.patchBehandelaarMutation.isPending() ||
      this.patchBehandelaarMutation.isSuccess() !==
        this.updateZaakMutation.isError(),
  );

  ngOnInit() {
    const zaak = this.zaak();
    const dateChangesAllowed = Boolean(
      !zaak.isProcesGestuurd &&
      zaak.rechten.wijzigen &&
      zaak.rechten.wijzigenDoorlooptijd,
    );

    this.groups = this.identityService
      .listBehandelaarGroupsForZaaktype(zaak.zaaktype.omschrijving!)
      .pipe(
        map((groups) => {
          const currentGroup = zaak.groep;
          if (currentGroup && !groups.find((g) => g.id === currentGroup.id)) {
            return [currentGroup, ...groups];
          }
          return groups;
        }),
      );

    if (!zaak.rechten.wijzigen) {
      this.form.controls.communicatiekanaal.disable();
      this.form.controls.vertrouwelijkheidaanduiding.disable();
      this.form.controls.omschrijving.disable();
      this.form.controls.toelichting.disable();
    }

    if (!zaak.zaaktype.servicenorm) {
      this.form.controls.einddatumGepland.disable();
    }

    this.form.controls.behandelaar.disable();
    if (!zaak.rechten.toekennen) {
      this.form.controls.groep.disable();
    }

    if (!dateChangesAllowed) {
      this.form.controls.startdatum.disable();
      this.form.controls.uiterlijkeEinddatumAfdoening.disable();

      if (zaak.einddatumGepland) {
        this.form.controls.einddatumGepland.disable();
      }
    }

    this.form.controls.reden.disable();
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.form.dirty) return;
        this.form.controls.reden.enable({ emitEvent: false });
      });

    this.form.patchValue({
      ...zaak,
      startdatum: moment(zaak.startdatum),
      uiterlijkeEinddatumAfdoening: moment(zaak.uiterlijkeEinddatumAfdoening),
      einddatumGepland: zaak.einddatumGepland
        ? moment(zaak.einddatumGepland)
        : null,
      vertrouwelijkheidaanduiding:
        this.confidentialityDesignations.find(
          ({ value }) =>
            value === zaak.vertrouwelijkheidaanduiding?.toLowerCase(),
        ) ?? null,
    });

    this.referentieTabelService
      .listCommunicatiekanalen()
      .subscribe((channels) => {
        if (zaak.communicatiekanaal) {
          channels.push(zaak.communicatiekanaal);
        }
        this.communicationChannels.set(Array.from(new Set(channels)));
      });

    this.form.controls.groep.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((group) => {
        if (!group) {
          this.form.controls.behandelaar.reset();
          this.form.controls.behandelaar.disable();
          return;
        }

        if (zaak.rechten.toekennen) {
          this.form.controls.behandelaar.enable();
        }

        this.identityService.listUsersInGroup(group.id).subscribe((users) => {
          this.users.set(users);

          const zaakUser = users.find(({ id }) => id === zaak.behandelaar?.id);
          const changedUser = users.find(
            ({ id }) => id === this.form.controls.behandelaar.value?.id,
          );
          this.form.controls.behandelaar.setValue(
            changedUser ?? zaakUser ?? null,
          );
        });
      });

    this.groups.subscribe((groups) => {
      const group = groups.find(({ id }) => id === zaak.groep?.id);
      this.form.controls.groep.setValue(group ?? null);
    });

    const { startdatum, einddatumGepland, uiterlijkeEinddatumAfdoening } = zaak;

    this.form.controls.startdatum.setValidators(
      startdatum ? [Validators.required] : [],
    );
    this.form.controls.einddatumGepland.setValidators(
      einddatumGepland ? [Validators.required] : [],
    );
    this.form.controls.uiterlijkeEinddatumAfdoening.setValidators(
      uiterlijkeEinddatumAfdoening ? [Validators.required] : [],
    );

    this.form.controls.startdatum.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.validateDates("startdatum"));
    this.form.controls.einddatumGepland.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.validateDates("einddatumGepland"));
    this.form.controls.uiterlijkeEinddatumAfdoening.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.validateDates("uiterlijkeEinddatumAfdoening"));
  }

  private validateDates(
    changedField:
      "startdatum" | "einddatumGepland" | "uiterlijkeEinddatumAfdoening",
  ) {
    const { startdatum, einddatumGepland, uiterlijkeEinddatumAfdoening } =
      this.form.getRawValue();
    const changedControl = this.form.controls[changedField];

    this.form.controls.startdatum.setErrors(null);
    this.form.controls.einddatumGepland.setErrors(null);
    this.form.controls.uiterlijkeEinddatumAfdoening.setErrors(null);

    if (
      startdatum &&
      einddatumGepland &&
      moment(startdatum).isAfter(moment(einddatumGepland))
    ) {
      changedControl.setErrors(
        FormHelper.CustomErrorMessage(
          "msg.error.date.invalid.datum.start-na-streef",
        ),
      );
    }

    if (
      startdatum &&
      uiterlijkeEinddatumAfdoening &&
      startdatum.isAfter(moment(uiterlijkeEinddatumAfdoening))
    ) {
      changedControl.setErrors(
        FormHelper.CustomErrorMessage(
          "msg.error.date.invalid.datum.start-na-fatale",
        ),
      );
    }

    if (
      einddatumGepland &&
      uiterlijkeEinddatumAfdoening &&
      moment(einddatumGepland).isAfter(moment(uiterlijkeEinddatumAfdoening))
    ) {
      changedControl.setErrors(
        FormHelper.CustomErrorMessage(
          "msg.error.date.invalid.datum.streef-na-fatale",
        ),
      );
    }

    [
      this.form.controls.startdatum,
      this.form.controls.einddatumGepland,
      this.form.controls.uiterlijkeEinddatumAfdoening,
    ].forEach((control) => {
      if (!control.errors) {
        control.updateValueAndValidity({ emitEvent: false });
      }
    });

    // reset() clears dirty/touched state, so mat-error would not show even if the
    // control is invalid. Re-mark dirty so Angular Material renders the error message.
    changedControl.markAsDirty({ onlySelf: true });
  }

  protected async onSubmit() {
    if (this.isSaveLocked()) {
      return;
    }

    try {
      await this.patchBehandelaarMutation.mutateAsync();
    } catch {
      return; // failure already logged by the mutation's onError
    }

    const value = this.form.getRawValue();
    this.updateZaakMutation.mutate({
      reden: value.reden ?? "",
      zaak: {
        ...value,
        vertrouwelijkheidaanduiding: value.vertrouwelijkheidaanduiding?.value,
        startdatum: value.startdatum?.toISOString(),
        einddatumGepland: value.einddatumGepland?.toISOString(),
        uiterlijkeEinddatumAfdoening:
          value.uiterlijkeEinddatumAfdoening?.toISOString(),
        omschrijving: value.omschrijving ?? "",
      } as unknown as GeneratedType<"RestZaakCreateData">,
    });
  }

  private patchBehandelaar(
    zaak: Pick<GeneratedType<"RestZaak">, "behandelaar" | "groep">,
    reason?: string,
  ) {
    const currentZaak = this.zaak();
    const isSameBehandelaar =
      zaak.behandelaar?.id === currentZaak.behandelaar?.id;

    const isSameGroup = zaak.groep?.id === currentZaak.groep?.id;
    if (isSameBehandelaar && isSameGroup) return EMPTY;

    if (zaak.behandelaar?.id === this.loggedInUser().id) {
      return this.zakenService.toekennenAanIngelogdeMedewerker({
        zaakUUID: currentZaak.uuid,
        groepId: zaak.groep?.id as string,
        reden: reason,
      });
    }

    return this.zakenService.toekennen({
      zaakUUID: currentZaak.uuid,
      groepId: zaak.groep?.id as string,
      behandelaarGebruikersnaam: zaak.behandelaar?.id,
      reden: reason,
    });
  }
}
