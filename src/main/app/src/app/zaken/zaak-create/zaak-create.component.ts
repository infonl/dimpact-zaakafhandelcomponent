/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnDestroy, ViewChild } from "@angular/core";
import { FormGroup, Validators } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { Observable, Subject, of } from "rxjs";
import { catchError, filter, takeUntil } from "rxjs/operators";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { BAGObject } from "../../bag/model/bagobject";
import { UtilService } from "../../core/service/util.service";
import { Vertrouwelijkheidaanduiding } from "../../informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { KlantenService } from "../../klanten/klanten.service";
import { Klant } from "../../klanten/model/klanten/klant";
import { InboxProductaanvraag } from "../../productaanvragen/model/inbox-productaanvraag";
import { ActionIcon } from "../../shared/edit/action-icon";
import { AutocompleteFormFieldBuilder } from "../../shared/material-form-builder/form-components/autocomplete/autocomplete-form-field-builder";
import { DateFormFieldBuilder } from "../../shared/material-form-builder/form-components/date/date-form-field-builder";
import { HeadingLevel } from "../../shared/material-form-builder/form-components/heading/heading-form-field";
import { HeadingFormFieldBuilder } from "../../shared/material-form-builder/form-components/heading/heading-form-field-builder";
import { InputFormField } from "../../shared/material-form-builder/form-components/input/input-form-field";
import { InputFormFieldBuilder } from "../../shared/material-form-builder/form-components/input/input-form-field-builder";
import { MedewerkerGroepFieldBuilder } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { MedewerkerGroepFormField } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { SelectFormField } from "../../shared/material-form-builder/form-components/select/select-form-field";
import { SelectFormFieldBuilder } from "../../shared/material-form-builder/form-components/select/select-form-field-builder";
import { TextareaFormField } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field";
import { TextareaFormFieldBuilder } from "../../shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { FormComponent } from "../../shared/material-form-builder/form/form/form.component";
import { AbstractFormField } from "../../shared/material-form-builder/model/abstract-form-field";
import { FieldType } from "../../shared/material-form-builder/model/field-type.enum";
import { FormConfig } from "../../shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "../../shared/material-form-builder/model/form-config-builder";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { OrderUtil } from "../../shared/order/order-util";
import { Zaak } from "../model/zaak";
import { ZaakAanmaakGegevens } from "../model/zaak-aanmaak-gegevens";
import { Zaaktype } from "../model/zaaktype";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-create",
  templateUrl: "./zaak-create.component.html",
  styleUrls: ["./zaak-create.component.less"],
})
export class ZaakCreateComponent implements OnDestroy {
  static KANAAL_E_FORMULIER = "E-formulier";
  createZaakFields: Array<AbstractFormField[]>;
  bagObjecten: BAGObject[] = [];
  formConfig: FormConfig;
  @ViewChild("actionsSideNav") actionsSidenav!: MatSidenav;
  @ViewChild("mfbForm") mfbForm!: FormComponent;
  activeSideAction: string | null = null;
  private initiatorField: InputFormField;
  private toelichtingField: TextareaFormField;
  private bagObjectenField: InputFormField;
  private medewerkerGroepFormField: MedewerkerGroepFormField;
  private vertrouwelijkheidaanduidingField: SelectFormField;
  private vertrouwelijkheidaanduidingen: { label: string; value: string }[];
  private ngDestroy = new Subject<void>();
  private initiatorToevoegenIcon = new ActionIcon(
    "person",
    "actie.initiator.toevoegen",
    new Subject<void>(),
  );
  private bagObjectenToevoegenIcon = new ActionIcon(
    "gps_fixed",
    "actie.bagObject.toevoegen",
    new Subject<void>(),
  );
  private initiator: Klant | null = null;
  private readonly inboxProductaanvraag: InboxProductaanvraag;
  private communicatiekanalen: Observable<string[]>;
  private communicatiekanaalField: SelectFormField;

  constructor(
    private zakenService: ZakenService,
    private router: Router,
    private navigation: NavigationService,
    private klantenService: KlantenService,
    private referentieTabelService: ReferentieTabelService,
    private translateService: TranslateService,
    private utilService: UtilService,
  ) {
    this.inboxProductaanvraag =
      this.router.getCurrentNavigation()?.extras?.state?.inboxProductaanvraag;

    this.utilService.setTitle("title.zaak.aanmaken");

    this.formConfig = new FormConfigBuilder()
      .saveText("actie.aanmaken")
      .cancelText("actie.annuleren")
      .build();
    this.communicatiekanalen =
      this.referentieTabelService.listCommunicatiekanalen(
        this.inboxProductaanvraag != null,
      );
    this.vertrouwelijkheidaanduidingen = this.utilService.getEnumAsSelectList(
      "vertrouwelijkheidaanduiding",
      Vertrouwelijkheidaanduiding,
    );

    const titel = new HeadingFormFieldBuilder()
      .id("aanmakenZaak")
      .label("actie.zaak.aanmaken")
      .level(HeadingLevel.H1)
      .build();

    const toekennenGegevensTitel = new HeadingFormFieldBuilder()
      .id("toekennengegevens")
      .label("gegevens.toekennen")
      .level(HeadingLevel.H2)
      .build();

    const overigeGegevensTitel = new HeadingFormFieldBuilder()
      .id("overigegegevens")
      .label("gegevens.overig")
      .level(HeadingLevel.H2)
      .build();

    const zaaktype = new AutocompleteFormFieldBuilder()
      .id("zaaktype")
      .label("zaaktype")
      .validators(Validators.required)
      .optionLabel("omschrijving")
      .options(this.zakenService.listZaaktypes())
      .hint("zaps.step.algemeen.zaaktype.hint")
      .build();

    zaaktype.formControl.valueChanges
      .pipe(
        filter((zt) => typeof zt !== "string"),
        takeUntil(this.ngDestroy),
      )
      .subscribe((v) => this.zaaktypeGeselecteerd(v));

    const startdatum = new DateFormFieldBuilder(moment())
      .id("startdatum")
      .label("startdatum")
      .validators(Validators.required)
      .build();

    this.medewerkerGroepFormField = this.getMedewerkerGroupFormField();

    this.initiatorField = new InputFormFieldBuilder()
      .id("initiatorIdentificatie")
      .styleClass("input-fake-enabled")
      .icon(this.initiatorToevoegenIcon)
      .externalInput()
      .label("initiator")
      .build();

    // if the list of communicatiekanalen includes E-formulier, it should be set as default
    this.communicatiekanaalField = new SelectFormFieldBuilder(
      ZaakCreateComponent.KANAAL_E_FORMULIER,
    )
      .id("communicatiekanaal")
      .label("communicatiekanaal")
      .options(this.communicatiekanalen)
      .validators(Validators.required)
      .build();

    this.vertrouwelijkheidaanduidingField = new SelectFormFieldBuilder()
      .id("vertrouwelijkheidaanduiding")
      .label("vertrouwelijkheidaanduiding")
      .optionLabel("label")
      .options(this.vertrouwelijkheidaanduidingen)
      .optionsOrder(OrderUtil.orderAsIs())
      .validators(Validators.required)
      .build();

    const omschrijving = new InputFormFieldBuilder()
      .id("omschrijving")
      .label("omschrijving")
      .maxlength(80)
      .validators(Validators.required)
      .build();
    this.toelichtingField = new TextareaFormFieldBuilder()
      .id("toelichting")
      .label("toelichting")
      .maxlength(1000)
      .build();

    this.bagObjectenField = new InputFormFieldBuilder()
      .id("bagObjecten")
      .styleClass("input-fake-enabled")
      .icon(this.bagObjectenToevoegenIcon)
      .externalInput()
      .label("bagObjecten")
      .build();

    this.initiatorField.clicked.subscribe(
      this.iconNext("actie.initiator.toevoegen"),
    );
    this.initiatorField.onClear.subscribe(() => {
      this.initiator = null;
      this.initiatorField.reset();
    });

    this.bagObjectenField.clicked.subscribe(
      this.iconNext("actie.bagObject.toevoegen"),
    );
    this.bagObjectenField.onClear.subscribe(() => {
      this.bagObjecten = [];
      this.bagObjectenField.reset();
    });

    this.createZaakFields = [
      [titel],
      [zaaktype, this.initiatorField],
      [startdatum, this.bagObjectenField],
      [toekennenGegevensTitel],
      [this.medewerkerGroepFormField],
      [overigeGegevensTitel],
      [this.communicatiekanaalField, this.vertrouwelijkheidaanduidingField],
      [omschrijving],
      [this.toelichtingField],
    ];

    if (this.inboxProductaanvraag) {
      this.verwerkInboxProductaanvraagGegevens();
    }
  }

  ngOnDestroy(): void {
    this.ngDestroy.next();
    this.ngDestroy.complete();
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      const zaak: Zaak = new Zaak();
      Object.keys(formGroup.controls).forEach((key) => {
        switch (key) {
          case "vertrouwelijkheidaanduiding":
            zaak[key] = formGroup.controls[key].value?.value;
            break;
          case "initiatorIdentificatie":
            if (this.initiator != null) {
              zaak["initiatorIdentificatieType"] =
                this.initiator.identificatieType;
              zaak[key] = this.initiator.identificatie;
            }
            break;
          case "bagObjecten":
            // skip
            break;
          case "toekenning":
            if (this.medewerkerGroepFormField.formControl.value.medewerker) {
              zaak.behandelaar =
                this.medewerkerGroepFormField.formControl.value.medewerker;
            }
            if (this.medewerkerGroepFormField.formControl.value.groep) {
              zaak.groep =
                this.medewerkerGroepFormField.formControl.value.groep;
            }
            break;
          default:
            zaak[key] = formGroup.controls[key].value;
            break;
        }
      });
      this.zakenService
        .createZaak(
          new ZaakAanmaakGegevens(
            zaak,
            this.inboxProductaanvraag,
            this.bagObjecten,
          ),
        )
        .pipe(
          catchError(() => {
            this.mfbForm.reset();
            return of();
          }),
        )
        .subscribe((newZaak) => {
          this.router.navigate(["/zaken/", newZaak.identificatie]);
        });
    } else {
      this.navigation.back();
    }
  }

  initiatorGeselecteerd(initiator: Klant): void {
    this.initiator = initiator;
    this.initiatorField.formControl.setValue(initiator?.naam);
    this.actionsSidenav.close();
  }

  getMedewerkerGroupFormField(
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
      .id("toekenning")
      .groepLabel("actie.zaak.toekennen.groep")
      .groepRequired()
      .medewerkerLabel("actie.zaak.toekennen.medewerker")
      .build();
  }

  zaaktypeGeselecteerd(zaaktype: Zaaktype): void {
    if (zaaktype) {
      this.medewerkerGroepFormField = this.getMedewerkerGroupFormField(
        zaaktype.zaakafhandelparameters.defaultGroepId,
        zaaktype.zaakafhandelparameters.defaultBehandelaarId,
      );
      const index = this.createZaakFields.findIndex((formRow) =>
        formRow.find(
          (formField) => formField.fieldType === FieldType.MEDEWERKER_GROEP,
        ),
      );
      this.createZaakFields[index] = [this.medewerkerGroepFormField];

      // update reference of the array to apply changes
      this.createZaakFields = [...this.createZaakFields];

      this.vertrouwelijkheidaanduidingField.formControl.setValue(
        this.vertrouwelijkheidaanduidingen.find(
          (o) => o.value === zaaktype.vertrouwelijkheidaanduiding,
        ),
      );
    }
  }

  private iconNext(action: string) {
    return () => {
      this.activeSideAction = action;
      this.actionsSidenav.open();
    };
  }

  private verwerkInboxProductaanvraagGegevens(): void {
    const bsnLength = 9;
    const vestigingsnummerLength = 12;
    const defaultToelichting =
      "Vanuit productaanvraag van type " + this.inboxProductaanvraag.type;
    if (this.inboxProductaanvraag.initiatorID) {
      if (this.inboxProductaanvraag.initiatorID.length === bsnLength) {
        this.initiatorField.formControl.setValue(
          this.inboxProductaanvraag.initiatorID,
        );
        this.klantenService
          .readPersoon(this.inboxProductaanvraag.initiatorID)
          .subscribe((initiator) => {
            this.initiator = initiator as Klant;
            this.initiatorField.formControl.setValue(initiator.naam);
          });
      } else if (
        this.inboxProductaanvraag.initiatorID.length === vestigingsnummerLength
      ) {
        this.initiatorField.formControl.setValue(
          this.inboxProductaanvraag.initiatorID,
        );
        this.klantenService
          .readVestiging(this.inboxProductaanvraag.initiatorID)
          .subscribe((initiator) => {
            this.initiator = initiator;
            this.initiatorField.formControl.setValue(initiator.naam);
          });
      }
    }
    this.toelichtingField.formControl.setValue(defaultToelichting);
  }

  bagGeselecteerd(): void {
    this.bagObjectenField.formControl.setValue(
      this.bagObjecten.map((b) => b.omschrijving).join(" | "),
    );
    if (this.bagObjectenField.formControl.value.length > 100) {
      this.translateService
        .get("msg.aantal.bagObjecten.geselecteerd", {
          aantal: this.bagObjecten.length,
        })
        .subscribe((v) => {
          this.bagObjectenField.formControl.setValue(v);
        });
    }
  }
}
