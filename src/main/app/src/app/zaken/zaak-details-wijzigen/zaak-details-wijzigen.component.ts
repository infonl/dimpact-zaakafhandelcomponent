/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnInit } from "@angular/core";
import {
  AbstractControl,
  FormBuilder,
  ValidatorFn,
  Validators,
} from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import moment, { Moment } from "moment";
import { Observable, of } from "rxjs";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { UtilService } from "src/app/core/service/util.service";
import { Vertrouwelijkheidaanduiding } from "src/app/informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { DateFormField } from "src/app/shared/material-form-builder/form-components/date/date-form-field";
import { DateFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/date/date-form-field-builder";
import { InputFormField } from "src/app/shared/material-form-builder/form-components/input/input-form-field";
import { AbstractFormField } from "src/app/shared/material-form-builder/model/abstract-form-field";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { IdentityService } from "../../identity/identity.service";
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

  private startDatumField!: DateFormField;
  private einddatumGeplandField!: DateFormField | InputFormField;
  private uiterlijkeEinddatumAfdoeningField!: DateFormField;

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

    // Form stuff
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
    this.form.valueChanges.subscribe((form) => {
      console.log(form);
      if (!this.form.dirty) return;
      this.form.controls.reden.enable();

      // TODO handle validity of dates
    });

    console.log(
      this.zaak,
      this.confidentialityDesignations,
      this.zaak.vertrouwelijkheidaanduiding,
    );

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
      // TODO: validate this actually works
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

    // TODO move this logic to state change
    this.formFields.flat().forEach((field) => {
      // Subscription(s) to revalidate 'other' enabled date field(s) after a date change, so 'other' date error messages are updated
      if (field instanceof DateFormField && field.formControl.enabled) {
        if (field.formControl.enabled) {
          field.formControl.valueChanges.subscribe(() => {
            this.formFields
              .flat()
              .filter(
                (f) =>
                  f instanceof DateFormField &&
                  f.id !== field.id &&
                  f.formControl.hasError("custom"),
              )
              .forEach((otherDateField) =>
                otherDateField.formControl.updateValueAndValidity({
                  emitEvent: false,
                }),
              );
          });
        }
      }
    });
  }

  private createDateFormField(
    id: string,
    enabled: boolean,
    validators: ValidatorFn[],
    value?: string | null,
  ): DateFormField {
    return new DateFormFieldBuilder(value)
      .id(id)
      .label(id)
      .validators(...validators)
      .disabled(!enabled)
      .build();
  }

  private validateStartDatum(control: AbstractControl) {
    const startDatum = moment(control.value);
    const einddatumGepland = moment(
      this.einddatumGeplandField.formControl.value,
    );
    const uiterlijkeEinddatumAfdoening = moment(
      this.uiterlijkeEinddatumAfdoeningField.formControl.value,
    );

    if (startDatum.isAfter(uiterlijkeEinddatumAfdoening)) {
      return {
        custom: { message: "msg.error.date.invalid.datum.start-na-fatale" },
      };
    }

    if (!this.einddatumGeplandField.formControl.value) return null;

    if (startDatum.isAfter(einddatumGepland)) {
      return {
        custom: { message: "msg.error.date.invalid.datum.start-na-streef" },
      };
    }

    return null;
  }

  private validateEinddatumGepland(control: AbstractControl) {
    const startDatum = moment(this.startDatumField.formControl.value);
    const einddatumGepland = moment(control.value);
    const uiterlijkeEinddatumAfdoening = moment(
      this.uiterlijkeEinddatumAfdoeningField.formControl.value,
    );

    if (einddatumGepland.isBefore(startDatum)) {
      return {
        custom: { message: "msg.error.date.invalid.datum.streef-voor-start" },
      };
    }

    if (einddatumGepland.isAfter(uiterlijkeEinddatumAfdoening)) {
      return {
        custom: { message: "msg.error.date.invalid.datum.streef-na-fatale" },
      };
    }

    return null;
  }

  private validateUiterlijkeEinddatumAfdoening(control: AbstractControl) {
    const startDatum = moment(this.startDatumField.formControl.value);
    const einddatumGepland = moment(
      this.einddatumGeplandField.formControl.value,
    );
    const uiterlijkeEinddatumAfdoening = moment(control.value);

    if (uiterlijkeEinddatumAfdoening.isBefore(startDatum)) {
      return {
        custom: { message: "msg.error.date.invalid.datum.fatale-voor-start" },
      };
    }

    if (!this.einddatumGeplandField.formControl.value) return null;

    if (uiterlijkeEinddatumAfdoening.isBefore(einddatumGepland)) {
      return {
        custom: { message: "msg.error.date.invalid.datum.fatale-voor-streef" },
      };
    }

    return null;
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
