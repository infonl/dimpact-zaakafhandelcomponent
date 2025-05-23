/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import {
  AbstractControl,
  FormGroup,
  ValidatorFn,
  Validators,
} from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import moment from "moment";
import { Observable, Subject, takeUntil } from "rxjs";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { UtilService } from "src/app/core/service/util.service";
import { Vertrouwelijkheidaanduiding } from "src/app/informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { DateFormField } from "src/app/shared/material-form-builder/form-components/date/date-form-field";
import { DateFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/date/date-form-field-builder";
import { InputFormField } from "src/app/shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { MedewerkerGroepFieldBuilder } from "src/app/shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { MedewerkerGroepFormField } from "src/app/shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { SelectFormField } from "src/app/shared/material-form-builder/form-components/select/select-form-field";
import { SelectFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/select/select-form-field-builder";
import { TextareaFormField } from "src/app/shared/material-form-builder/form-components/textarea/textarea-form-field";
import { TextareaFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { AbstractFormField } from "src/app/shared/material-form-builder/model/abstract-form-field";
import { FormConfig } from "src/app/shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "src/app/shared/material-form-builder/model/form-config-builder";
import { OrderUtil } from "src/app/shared/order/order-util";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-case-details-edit",
  templateUrl: "./zaak-details-wijzigen.component.html",
  styleUrls: ["./zaak-details-wijzigen.component.less"],
})
export class CaseDetailsEditComponent implements OnDestroy, OnInit {
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  @Input({ required: true }) loggedInUser!: GeneratedType<"RestLoggedInUser">;
  @Input({ required: true }) sideNav!: MatDrawer;

  formFields: Array<AbstractFormField[]> = [];
  formConfig: FormConfig;
  reasonField: TextareaFormField;

  private medewerkerGroepFormField!: MedewerkerGroepFormField;
  private communicatiekanalen: Observable<string[]>;
  private communicatiekanaalField!: SelectFormField<string>;
  private startDatumField!: DateFormField;
  private einddatumGeplandField!: DateFormField | InputFormField;
  private uiterlijkeEinddatumAfdoeningField!: DateFormField;
  private vertrouwelijkheidaanduidingField!: SelectFormField<{
    label: string;
    value: string;
  }>;
  private vertrouwelijkheidaanduidingenList: { label: string; value: string }[];
  private omschrijving!: TextareaFormField;
  private toelichtingField!: TextareaFormField;
  private ngDestroy = new Subject<void>();
  private initialZaakGeometry?: GeneratedType<"RestGeometry"> | null = null;
  private dateChangesALlowed = false;

  constructor(
    private zakenService: ZakenService,
    private referentieTabelService: ReferentieTabelService,
    private utilService: UtilService,
  ) {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.opslaan")
      .cancelText("actie.annuleren")
      .requireUserChanges(true)
      .build();

    this.communicatiekanalen =
      this.referentieTabelService.listCommunicatiekanalen();

    this.vertrouwelijkheidaanduidingenList =
      this.utilService.getEnumAsSelectList(
        "vertrouwelijkheidaanduiding",
        Vertrouwelijkheidaanduiding,
      );

    this.reasonField = new InputFormFieldBuilder()
      .id("reason")
      .label("reden")
      .maxlength(80)
      .validators(Validators.required)
      .disabled()
      .build();

    // Forcing the set value to sync tabs
    this.reasonField.formControl.valueChanges.subscribe((value) => {
      this.reasonField.formControl.setValue(value, { emitEvent: false });
    });
  }

  ngOnInit() {
    this.initialZaakGeometry = this.zaak.zaakgeometrie;

    this.dateChangesALlowed = Boolean(
      !this.zaak.isProcesGestuurd &&
        this.zaak.rechten.wijzigen &&
        this.zaak.rechten.wijzigenDoorlooptijd,
    );

    this.medewerkerGroepFormField = this.getMedewerkerGroupFormField(
      !this.zaak.rechten.toekennen,
      this.zaak?.groep?.id,
      this.zaak?.behandelaar?.id,
    );

    this.communicatiekanaalField = new SelectFormFieldBuilder(
      this.zaak.communicatiekanaal,
    )
      .id("communicatiekanaal")
      .label("communicatiekanaal")
      .options(this.communicatiekanalen)
      .disabled(!this.zaak.rechten.wijzigen)
      .validators(Validators.required)
      .build();

    this.startDatumField = this.createDateFormField(
      "startdatum",
      this.dateChangesALlowed,
      [Validators.required, (control) => this.validateStartDatum(control)],
      this.zaak.startdatum,
    );

    this.einddatumGeplandField = this.createDateFormField(
      "einddatumGepland",
      !!this.zaak.einddatumGepland && this.dateChangesALlowed,
      [
        this.zaak.einddatumGepland
          ? Validators.required
          : Validators.nullValidator,
        (control) => this.validateEinddatumGepland(control),
      ],
      this.zaak.einddatumGepland,
    );

    this.uiterlijkeEinddatumAfdoeningField = this.createDateFormField(
      "uiterlijkeEinddatumAfdoening",
      this.dateChangesALlowed,
      [
        Validators.required,
        (control) => this.validateUiterlijkeEinddatumAfdoening(control),
      ],
      this.zaak.uiterlijkeEinddatumAfdoening,
    );

    this.vertrouwelijkheidaanduidingField = new SelectFormFieldBuilder(
      this.vertrouwelijkheidaanduidingenList.find(
        ({ value }) =>
          value === this.zaak.vertrouwelijkheidaanduiding?.toLowerCase(),
      ),
    )
      .id("vertrouwelijkheidaanduiding")
      .label("vertrouwelijkheidaanduiding")
      .optionLabel("label")
      .options(this.vertrouwelijkheidaanduidingenList)
      .optionsOrder(OrderUtil.orderAsIs())
      .disabled(!this.zaak.rechten.wijzigen)
      .validators(Validators.required)
      .build();

    this.omschrijving = new InputFormFieldBuilder(this.zaak.omschrijving)
      .id("omschrijving")
      .label("omschrijving")
      .maxlength(80)
      .disabled(!this.zaak.rechten.wijzigen)
      .validators(Validators.required)
      .build();

    this.toelichtingField = new TextareaFormFieldBuilder(this.zaak.toelichting)
      .id("toelichting")
      .label("toelichting")
      .maxlength(1000)
      .disabled(!this.zaak.rechten.wijzigen)
      .build();

    this.formFields = [
      [this.medewerkerGroepFormField, this.communicatiekanaalField],
      [
        this.startDatumField,
        this.einddatumGeplandField,
        this.uiterlijkeEinddatumAfdoeningField,
      ],
      [this.vertrouwelijkheidaanduidingField],
      [this.omschrijving],
      [this.toelichtingField],
      [this.reasonField],
    ];

    this.formFields.flat().forEach((field) => {
      //Subscriptions to enable reason field on field changes
      if (field.formControl.enabled) {
        field.formControl.valueChanges
          .pipe(takeUntil(this.ngDestroy))
          .subscribe(() => {
            if (
              field.formControl.dirty &&
              this.reasonField.formControl.disabled
            ) {
              this.reasonField.formControl.enable({ emitEvent: false });
            }
          });
      }

      // Subscription(s) to revalidate 'other' enabled date field(s) after a date change, so 'other' date error messages are updated
      if (field instanceof DateFormField && field.formControl.enabled) {
        if (field.formControl.enabled) {
          field.formControl.valueChanges
            .pipe(takeUntil(this.ngDestroy))
            .subscribe(() => {
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

  onSubmit(formGroup?: FormGroup): void {
    if (!formGroup) {
      void this.sideNav.close();
      return;
    }
    const updates = Object.entries(formGroup.controls).reduce(
      (acc, [key, control]) => {
        const value = control.value;
        acc[key] = key === "vertrouwelijkheidaanduiding" ? value.value : value;
        return acc;
      },
      {},
    );

    void this.updateZaak(this.createZaakPatch(updates));
  }

  private createZaakPatch(update: Record<string, unknown>) {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { assignment, reason, ...updates } = update;

    return {
      ...updates,
      groep: (assignment as { groep: GeneratedType<"RestGroup"> }).groep,
      behandelaar: (assignment as { medewerker: GeneratedType<"RestUser"> })
        .medewerker,
    } satisfies Partial<GeneratedType<"RestZaak">>;
  }

  private async updateZaak(zaak: Partial<GeneratedType<"RestZaak">>) {
    const reason = this.reasonField.formControl.value;

    this.patchBehandelaar(zaak, reason);

    this.zakenService
      .updateZaak(this.zaak.uuid, { zaak, reden: reason })
      .pipe(takeUntil(this.ngDestroy))
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
    zaak: Partial<GeneratedType<"RestZaak">>,
    reason?: string,
  ) {
    if (
      zaak.behandelaar?.id === this.zaak.behandelaar?.id &&
      zaak.groep?.id === this.zaak.groep?.id
    ) {
      return;
    }

    if (zaak.behandelaar?.id === this.loggedInUser.id) {
      return this.zakenService
        .toekennenAanIngelogdeMedewerker(this.zaak.uuid, reason)
        .subscribe(() => {});
    }

    return this.zakenService
      .toekennen(this.zaak.uuid, {
        reason,
        groupId: zaak.groep?.id,
        behandelaarId: zaak?.behandelaar?.id,
      })
      .subscribe(() => {});
  }

  exit() {
    this.zaak.zaakgeometrie = this.initialZaakGeometry;
    void this.sideNav.close();
  }

  private getMedewerkerGroupFormField(
    disabled: boolean = false,
    groupId?: string,
    employeeId?: string,
  ): MedewerkerGroepFormField {
    return new MedewerkerGroepFieldBuilder(
      groupId
        ? ({ id: groupId, naam: "" } as GeneratedType<"RestGroup">)
        : undefined,
      employeeId
        ? ({ id: employeeId, naam: "" } as GeneratedType<"RestUser">)
        : undefined,
    )
      .id("assignment")
      .groepLabel("actie.zaak.toekennen.groep")
      .medewerkerLabel("actie.zaak.toekennen.medewerker")
      .groepRequired()
      .styleClass("row case-details-edit-form")
      .disabled(disabled)
      .setZaaktypeUuid(this.zaak.zaaktype.uuid)
      .build();
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
