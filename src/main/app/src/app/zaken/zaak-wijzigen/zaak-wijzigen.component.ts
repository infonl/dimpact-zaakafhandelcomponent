/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from "@angular/core";
import { MatDrawer } from "@angular/material/sidenav";
import { FormGroup, Validators } from "@angular/forms";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { MedewerkerGroepFormField } from "src/app/shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { FormConfig } from "src/app/shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "src/app/shared/material-form-builder/model/form-config-builder";
import { Observable, Subject, forkJoin, takeUntil } from "rxjs";
import { SelectFormField } from "src/app/shared/material-form-builder/form-components/select/select-form-field";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { UtilService } from "src/app/core/service/util.service";
import { Vertrouwelijkheidaanduiding } from "src/app/informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { MedewerkerGroepFieldBuilder } from "src/app/shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { SelectFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/select/select-form-field-builder";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { TextareaFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { TextareaFormField } from "src/app/shared/material-form-builder/form-components/textarea/textarea-form-field";
import { AbstractFormField } from "src/app/shared/material-form-builder/model/abstract-form-field";
import { OrderUtil } from "src/app/shared/order/order-util";
import { DateFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/date/date-form-field-builder";
import { DateFormField } from "src/app/shared/material-form-builder/form-components/date/date-form-field";
import { InputFormField } from "src/app/shared/material-form-builder/form-components/input/input-form-field";
import { ZakenService } from "../zaken.service";
import { Zaak } from "../model/zaak";

@Component({
  selector: "zac-case-details-edit",
  templateUrl: "./zaak-wijzigen.component.html",
  styleUrls: ["./zaak-wijzigen.component.less"],
})
export class ZaakWijzigenComponent implements OnInit, OnDestroy {
  @Input() sideNav: MatDrawer;
  @Input() readonly: boolean;
  @Input() zaak: Zaak; // GeneratedType<"RestZaak">;
  @Input() loggedInUser: GeneratedType<"RestLoggedInUser">;
  @Output() caseEdit = new EventEmitter<any>();

  formFields: Array<AbstractFormField[]>;
  formConfig: FormConfig;

  private medewerkerGroepFormField: MedewerkerGroepFormField;
  private communicatiekanalen: Observable<string[]>;
  private communicatiekanaalField: SelectFormField;
  private startdatum: DateFormField;
  private einddatumGepland: DateFormField | InputFormField;
  private uiterlijkeEinddatumAfdoening: DateFormField;
  private vertrouwelijkheidaanduidingField: SelectFormField;
  private vertrouwelijkheidaanduidingen: { label: string; value: string }[];
  private omschrijving: TextareaFormField;
  private toelichtingField: TextareaFormField;
  private reasonField: TextareaFormField;
  private ngDestroy = new Subject<void>();

  constructor(
    private zakenService: ZakenService,
    private referentieTabelService: ReferentieTabelService,
    private utilService: UtilService,
  ) {}

  ngOnInit(): void {
    console.log("zaak, loggedInUser", this.zaak, this.loggedInUser);

    this.initForm();
  }

  private initForm(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.aanmaken")
      .cancelText("actie.annuleren")
      .requireUserChanges(true)
      .build();

    this.communicatiekanalen =
      this.referentieTabelService.listCommunicatiekanalen();

    this.vertrouwelijkheidaanduidingen = this.utilService.getEnumAsSelectList(
      "vertrouwelijkheidaanduiding",
      Vertrouwelijkheidaanduiding,
    );

    this.medewerkerGroepFormField = this.getMedewerkerGroupFormField(
      this.zaak?.groep.id,
      this.zaak?.behandelaar?.id,
    );

    this.communicatiekanaalField = new SelectFormFieldBuilder(
      this.zaak.communicatiekanaal,
    )
      .id("communicatiekanaal")
      .label("communicatiekanaal")
      .options(this.communicatiekanalen)
      .validators(Validators.required)
      .build();

    this.startdatum = new DateFormFieldBuilder(this.zaak.startdatum)
      .id("startdatum")
      .label("startdatum")
      .validators(Validators.required)
      .build();

    this.einddatumGepland = new DateFormFieldBuilder(this.zaak.einddatumGepland)
      .id("einddatumGepland")
      .label("einddatumGepland")
      .validators(
        this.zaak.einddatumGepland
          ? Validators.required
          : Validators.nullValidator,
      )
      .build();

    this.uiterlijkeEinddatumAfdoening = new DateFormFieldBuilder(
      this.zaak.uiterlijkeEinddatumAfdoening,
    )
      .id("uiterlijkeEinddatumAfdoening")
      .label("uiterlijkeEinddatumAfdoening")
      .validators(Validators.required)
      .build();

    this.vertrouwelijkheidaanduidingField = new SelectFormFieldBuilder(
      this.vertrouwelijkheidaanduidingen.find(
        (o) => o.value === this.zaak.vertrouwelijkheidaanduiding.toLowerCase(),
      ),
    )
      .id("vertrouwelijkheidaanduiding")
      .label("vertrouwelijkheidaanduiding")
      .optionLabel("label")
      .options(this.vertrouwelijkheidaanduidingen)
      .optionsOrder(OrderUtil.orderAsIs())
      .validators(Validators.required)
      .build();

    this.omschrijving = new InputFormFieldBuilder(this.zaak.omschrijving)
      .id("omschrijving")
      .label("omschrijving")
      .maxlength(80)
      .validators(Validators.required)
      .build();

    this.toelichtingField = new TextareaFormFieldBuilder(this.zaak.toelichting)
      .id("toelichting")
      .label("toelichting")
      .maxlength(1000)
      .build();

    this.reasonField = new InputFormFieldBuilder()
      .id("reason")
      .label("reden")
      .maxlength(80)
      .disabled()
      .validators(Validators.required)
      .build();

    this.formFields = [
      [this.medewerkerGroepFormField, this.communicatiekanaalField],
      [
        this.startdatum,
        this.einddatumGepland,
        this.uiterlijkeEinddatumAfdoening,
      ],
      [this.vertrouwelijkheidaanduidingField],
      [this.omschrijving],
      [this.toelichtingField],
      [this.reasonField],
    ];

    if (this.readonly) {
      this.formFields.flat().forEach((field) => {
        field.formControl.disable();
      });
    } else {
      this.formFields.flat().forEach((field) => {
        // Enable reason field when any other field is dirty
        const subscription$ = field.formControl.valueChanges
          .pipe(takeUntil(this.ngDestroy))
          .subscribe(() => {
            if (field.formControl.dirty) {
              this.reasonField.formControl.enable({ emitEvent: false });
              subscription$.unsubscribe();
            }
          });

        // Disable einddatumGepland field when startdatum is changed;
        // Workaround; the .disable() method does not work as expected
        if (field.id === "einddatumGepland") {
          field.formControl.disable();
          return;
        }
      });
    }
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const changedValues: Partial<
        GeneratedType<"RestZaak"> & {
          assignment: {
            groep: GeneratedType<"RestGroup">;
            medewerker: GeneratedType<"RestUser">;
          };
          reason: string;
        }
      > = {};
      for (const [key, control] of Object.entries(formGroup.controls)) {
        if (control.dirty) {
          changedValues[key] = control.value;
        }
      }
      const { reason, assignment, ...patchFields } = changedValues;

      console.log("assignment", assignment);
      console.log("changedValues", changedValues);

      const subscriptions = [];

      if (assignment) {
        this.zaak = {
          ...this.zaak,
          ...assignment.groep,
          behandelaar: assignment.medewerker,
        };

        if (this.zaak.behandelaar?.id === this.loggedInUser.id) {
          subscriptions.push(
            this.zakenService
              .toekennenAanIngelogdeMedewerker(this.zaak, reason)
              .subscribe((zaak) => {
                this.utilService.openSnackbar("msg.zaak.toegekend", {
                  behandelaar: zaak.behandelaar?.naam,
                });
              }),
          );
        } else {
          subscriptions.push(
            this.zakenService.toekennen(this.zaak, reason).subscribe((zaak) => {
              if (zaak?.behandelaar?.id) {
                this.utilService.openSnackbar("msg.zaak.toegekend", {
                  behandelaar: zaak.behandelaar.naam,
                });
              } else {
                this.utilService.openSnackbar("msg.vrijgegeven.zaak");
              }
            }),
          );
        }
      }

      if (patchFields) {
        const zaak: Zaak = new Zaak();
        Object.keys(patchFields).forEach((key) => {
          // circumvent the TypeScript type check (pattern copied from zaak-view.component.ts)
          zaak[key] = patchFields[key].value
            ? patchFields[key].value
            : patchFields[key];
        });

        console.log("zaak", zaak);

        subscriptions.push(
          this.zakenService
            .updateZaak(this.zaak.uuid, zaak, reason)
            .subscribe((updatedZaak) => {
              console.log("updatedZaak", updatedZaak);
            }),
        );
      }

      if (subscriptions.length > 0) {
        forkJoin([subscriptions]).subscribe(() => {
          this.sideNav.close();
        });
      }
    }
  }

  private getMedewerkerGroupFormField(
    groupId?: string,
    employeeId?: string,
  ): MedewerkerGroepFormField {
    return new MedewerkerGroepFieldBuilder(
      groupId
        ? ({ id: groupId, naam: "" } as GeneratedType<"RestGroup">)
        : null,
      employeeId
        ? ({ id: employeeId, naam: "" } as GeneratedType<"RestUser">)
        : null,
    )
      .id("assignment")
      .groepLabel("actie.zaak.toekennen.groep")
      .groepRequired()
      .medewerkerLabel("actie.zaak.toekennen.medewerker")
      .build();
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }
}
