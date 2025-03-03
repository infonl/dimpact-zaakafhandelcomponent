/*
 * SPDX-FileCopyrightText: 2025 Lifely
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
// @ts-ignore-next-line allow to import moment
import moment from "moment";
import { Observable, Subject, Subscription, forkJoin, takeUntil } from "rxjs";
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
import { Geometry } from "../model/geometry";
import { Zaak } from "../model/zaak";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-case-details-edit",
  templateUrl: "./zaak-details-wijzigen.component.html",
  styleUrls: ["./zaak-details-wijzigen.component.less"],
})
export class CaseDetailsEditComponent implements OnDestroy, OnInit {
  @Input({ required: true }) zaak!: Zaak; // GeneratedType<"RestZaak">;
  @Input({ required: true }) loggedInUser!: GeneratedType<"RestLoggedInUser">;
  @Input({ required: true }) sideNav!: MatDrawer;

  formFields: Array<AbstractFormField[]> = [];
  formConfig: FormConfig;

  private medewerkerGroepFormField: MedewerkerGroepFormField;
  private communicatiekanalen: Observable<string[]>;
  private communicatiekanaalField: SelectFormField;
  private startDatumField: DateFormField;
  private einddatumGeplandField: DateFormField | InputFormField;
  private uiterlijkeEinddatumAfdoeningField: DateFormField;
  private vertrouwelijkheidaanduidingField: SelectFormField;
  private vertrouwelijkheidaanduidingenList: { label: string; value: string }[];
  private omschrijving: TextareaFormField;
  private toelichtingField: TextareaFormField;
  reasonField: TextareaFormField;
  private ngDestroy = new Subject<void>();
  private initialZaakGeometry: Geometry;

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
      .build();

    // Forcing the set value to sync tabs
    this.reasonField.formControl.valueChanges.subscribe((value) => {
      this.reasonField.formControl.setValue(value, { emitEvent: false });
    });
  }

  ngOnInit() {
    this.initialZaakGeometry = this.zaak.zaakgeometrie;

    this.medewerkerGroepFormField = this.getMedewerkerGroupFormField(
      !this.zaak.isOpen || !this.zaak.rechten.toekennen,
      this.zaak?.groep.id,
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
      this.zaak.startdatum,
      !this.zaak.rechten.wijzigen || this.zaak.isProcesGestuurd,
      [Validators.required, (control) => this.validateStartDatum(control)],
    );

    this.einddatumGeplandField = this.createDateFormField(
      "einddatumGepland",
      this.zaak.einddatumGepland,
      this.zaak.einddatumGepland &&
        (!this.zaak.rechten.wijzigen || this.zaak.isProcesGestuurd),
      [
        this.zaak.einddatumGepland
          ? Validators.required
          : Validators.nullValidator,
        (control) => this.validateEinddatumGepland(control),
      ],
    );

    this.uiterlijkeEinddatumAfdoeningField = this.createDateFormField(
      "uiterlijkeEinddatumAfdoening",
      this.zaak.uiterlijkeEinddatumAfdoening,
      !this.zaak.rechten.wijzigen || this.zaak.isProcesGestuurd,
      [
        Validators.required,
        (control) => this.validateUiterlijkeEinddatumAfdoening(control),
      ],
    );

    this.vertrouwelijkheidaanduidingField = new SelectFormFieldBuilder(
      this.vertrouwelijkheidaanduidingenList.find(
        (o) => o.value === this.zaak.vertrouwelijkheidaanduiding.toLowerCase(),
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
    value: any,
    disabled: boolean,
    validators: ValidatorFn[],
  ): DateFormField {
    return new DateFormFieldBuilder(value)
      .id(id)
      .label(id)
      .validators(...validators)
      .disabled(disabled)
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

  saveFromFormView(formGroup: FormGroup): void {
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

  saveFromMapView() {
    const updates = this.formFields.reduce((acc, fields) => {
      fields.forEach((field) => {
        const value = field.formControl.value;
        acc[field.id] =
          field.id === "vertrouwelijkheidaanduiding" ? value.value : value;
      });
      return acc;
    }, {});

    void this.updateZaak(this.createZaakPatch(updates));
  }

  locationChanged(update: Geometry) {
    this.zaak.zaakgeometrie = update;
  }

  private createZaakPatch(update: Record<string, any>) {
    const { assignment, reason, ...updates } = update;

    return {
      ...updates,
      groep: assignment.groep,
      behandelaar: assignment.medewerker,
    } satisfies Partial<Zaak>;
  }

  private async updateZaak(zaak: Partial<Zaak>) {
    const reason = this.reasonField.formControl.value;
    const subscriptions: Subscription[] = [];

    subscriptions.push(this.patchBehandelaar(zaak, reason));

    this.patchLocation(reason).subscribe(() => {
      // To prevent a race condition we need to first update the `zaakgeometrie` and then the other fields
      subscriptions.push(
        this.zakenService
          .updateZaak(this.zaak.uuid, { zaak, reden: reason })
          .subscribe(() => {}),
      );

      forkJoin([subscriptions]).subscribe(() => this.sideNav.close());
    });
  }

  private patchLocation(reason?: string) {
    if (
      JSON.stringify(this.zaak.zaakgeometrie) ===
      JSON.stringify(this.initialZaakGeometry)
    ) {
      return new Observable((observer) => {
        observer.next(null);
        observer.complete();
      });
    }

    return this.zakenService.updateZaakLocatie(
      this.zaak.uuid,
      this.zaak.zaakgeometrie,
      reason,
    );
  }

  private patchBehandelaar(zaak: Partial<Zaak>, reason?: string) {
    if (zaak.behandelaar?.id === this.zaak.behandelaar?.id) {
      return;
    }

    if (zaak.behandelaar?.id === this.loggedInUser.id) {
      return this.zakenService
        .toekennenAanIngelogdeMedewerker(this.zaak.uuid, reason)
        .subscribe((updatedZaak) => {
          this.utilService.openSnackbar("msg.zaak.toegekend", {
            behandelaar: updatedZaak.behandelaar?.naam,
          });
        });
    }

    return this.zakenService
      .toekennen(this.zaak.uuid, {
        reason,
        groupId: zaak.groep?.id,
        behandelaarId: zaak?.behandelaar?.id,
      })
      .subscribe((updatedZaak) => {
        if (updatedZaak.behandelaar?.id) {
          this.utilService.openSnackbar("msg.zaak.toegekend", {
            behandelaar: updatedZaak.behandelaar?.naam,
          });
        } else {
          this.utilService.openSnackbar("msg.vrijgegeven.zaak");
        }
      });
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
      .build();
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
