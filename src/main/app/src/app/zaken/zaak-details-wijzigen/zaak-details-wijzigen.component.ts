/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnInit } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import moment, { Moment } from "moment";
import { Observable, of } from "rxjs";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { UtilService } from "src/app/core/service/util.service";
import { Vertrouwelijkheidaanduiding } from "src/app/informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { AbstractFormField } from "src/app/shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { IdentityService } from "../../identity/identity.service";
import { FormHelper } from "../../shared/form/helpers";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-case-details-edit",
  templateUrl: "./zaak-details-wijzigen.component.html",
})
export class CaseDetailsEditComponent implements OnInit {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) loggedInUser!: GeneratedType<"RestLoggedInUser">;
  @Input({ required: true }) sideNav!: MatDrawer;

  formFields: Array<AbstractFormField[]> = [];

  protected groups: Observable<GeneratedType<"RestGroup">[]> = of([]);
  protected users: GeneratedType<"RestUser">[] = [];
  protected communicationChannels =
    this.referentieTabelService.listCommunicatiekanalen();
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

  constructor(
    private readonly zakenService: ZakenService,
    private readonly referentieTabelService: ReferentieTabelService,
    private readonly utilService: UtilService,
    private readonly formBuilder: FormBuilder,
    private readonly identityService: IdentityService,
  ) {}

  ngOnInit() {
    const dateChangesAllowed = Boolean(
      !this.zaak.isProcesGestuurd &&
        this.zaak.rechten.wijzigen &&
        this.zaak.rechten.wijzigenDoorlooptijd,
    );

    this.groups = this.identityService.listGroups(this.zaak.zaaktype.uuid);

    if (!this.zaak.rechten.wijzigen) {
      this.form.controls.communicatiekanaal.disable();
      this.form.controls.vertrouwelijkheidaanduiding.disable();
      this.form.controls.omschrijving.disable();
      this.form.controls.toelichting.disable();
    }

    this.form.controls.behandelaar.disable();
    if (!this.zaak.rechten.toekennen) {
      this.form.controls.groep.disable();
    }

    if (!dateChangesAllowed) {
      this.form.controls.startdatum.disable();
      this.form.controls.uiterlijkeEinddatumAfdoening.disable();

      if (this.zaak.einddatumGepland) {
        this.form.controls.einddatumGepland.disable();
      }
    }

    this.form.controls.reden.disable();
    this.form.valueChanges.subscribe(() => {
      if (!this.form.dirty) return;
      this.form.controls.reden.enable({ emitEvent: false });
    });

    this.form.setValue({
      startdatum: moment(this.zaak.startdatum),
      uiterlijkeEinddatumAfdoening: moment(
        this.zaak.uiterlijkeEinddatumAfdoening,
      ),
      behandelaar: null,
      communicatiekanaal: this.zaak.communicatiekanaal ?? null,
      einddatumGepland: moment(this.zaak.einddatumGepland),
      groep: null,
      omschrijving: this.zaak.omschrijving,
      reden: null,
      toelichting: this.zaak.toelichting ?? null,
      vertrouwelijkheidaanduiding:
        this.confidentialityDesignations.find(
          ({ value }) =>
            value === this.zaak.vertrouwelijkheidaanduiding?.toLowerCase(),
        ) ?? null,
    });

    this.communicationChannels.subscribe((channels) => {
      this.communicationChannels = of(
        Array.from(new Set(...channels, this.zaak.communicatiekanaal)),
      );
    });

    this.form.controls.groep.valueChanges.subscribe((group) => {
      if (!group) {
        this.form.controls.behandelaar.reset();
        this.form.controls.behandelaar.disable();
        return;
      }

      if (this.zaak.rechten.toekennen) {
        this.form.controls.behandelaar.enable();
      }

      this.identityService.listUsersInGroup(group.id).subscribe((users) => {
        this.users = users;

        if (!this.zaak.behandelaar && !this.form.controls.behandelaar.value)
          return;

        const zaakUser = users.find(
          ({ id }) => id === this.zaak.behandelaar?.id,
        );
        const changedUser = users.find(
          ({ id }) => id === this.form.controls.behandelaar.value?.id,
        );
        this.form.controls.behandelaar.setValue(
          changedUser ?? zaakUser ?? null,
        );
      });
    });

    this.groups.subscribe((groups) => {
      const group = groups.find(({ id }) => id === this.zaak.groep?.id);
      this.form.controls.groep.setValue(group ?? null);
    });

    const { startdatum, einddatumGepland, uiterlijkeEinddatumAfdoening } =
      this.zaak;

    this.form.controls.startdatum.setValidators(
      startdatum ? [Validators.required] : [],
    );
    this.form.controls.einddatumGepland.setValidators(
      einddatumGepland ? [Validators.required] : [],
    );
    this.form.controls.uiterlijkeEinddatumAfdoening.setValidators(
      uiterlijkeEinddatumAfdoening ? [Validators.required] : [],
    );

    this.form.controls.startdatum.valueChanges.subscribe(
      this.validateDates.bind(this),
    );
    this.form.controls.einddatumGepland.valueChanges.subscribe(
      this.validateDates.bind(this),
    );
    this.form.controls.uiterlijkeEinddatumAfdoening.valueChanges.subscribe(
      this.validateDates.bind(this),
    );
  }

  private validateDates() {
    const { startdatum, einddatumGepland, uiterlijkeEinddatumAfdoening } =
      this.form.getRawValue();

    this.form.controls.startdatum.setErrors(null);
    this.form.controls.einddatumGepland.setErrors(null);
    this.form.controls.uiterlijkeEinddatumAfdoening.setErrors(null);

    if (
      startdatum &&
      einddatumGepland &&
      moment(startdatum).isAfter(moment(einddatumGepland))
    ) {
      this.form.controls.startdatum.setErrors(
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
      this.form.controls.startdatum.setErrors(
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
      this.form.controls.einddatumGepland.setErrors(
        FormHelper.CustomErrorMessage(
          "msg.error.date.invalid.datum.streef-na-fatale",
        ),
      );
    }
  }

  protected onSubmit(form: typeof this.form): void {
    const reason = this.form.controls.reden.value ?? undefined;

    this.patchBehandelaar(form.value, reason);

    this.zakenService
      .updateZaak(this.zaak.uuid, {
        zaak: {
          ...form.value,
          vertrouwelijkheidaanduiding:
            form.value.vertrouwelijkheidaanduiding?.value,
          startdatum: form.value.startdatum?.toISOString(),
          einddatumGepland: form.value.einddatumGepland?.toISOString(),
          uiterlijkeEinddatumAfdoening:
            form.value.einddatumGepland?.toISOString(),
          omschrijving: form.value.omschrijving ?? undefined,
        },
        reden: reason,
      })
      .subscribe({
        next: () => {
          void this.sideNav.close();
        },
        error: (err) => {
          console.error("Fout bij bijwerken zaak:", err);
        },
      });
  }

  private patchBehandelaar(
    zaak: Pick<GeneratedType<"RestZaak">, "behandelaar" | "groep">,
    reason?: string,
  ) {
    const isSameBehandelaar =
      zaak.behandelaar?.id === this.zaak.behandelaar?.id;
    const isSameGroup = zaak.groep?.id === this.zaak.groep?.id;
    if (isSameBehandelaar && isSameGroup) return;

    if (zaak.behandelaar?.id === this.loggedInUser.id) {
      this.zakenService.toekennenAanIngelogdeMedewerker(this.zaak.uuid, reason);
      return;
    }

    this.zakenService.toekennen(this.zaak.uuid, {
      reason,
      groupId: zaak.groep?.id,
      behandelaarId: zaak?.behandelaar?.id,
    });
  }
}
