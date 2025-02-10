/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  Input,
  OnDestroy,
  OnInit,
} from "@angular/core";
import { MatDrawer } from "@angular/material/sidenav";
import { FormGroup, Validators } from "@angular/forms";
import { FormControl } from "@angular/forms";
import {} from "../../shared/location/location.service";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { MedewerkerGroepFormField } from "src/app/shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { FormConfig } from "src/app/shared/material-form-builder/model/form-config";
import { FormConfigBuilder } from "src/app/shared/material-form-builder/model/form-config-builder";
import { Observable, Subject } from "rxjs";
import { Select } from "ol/interaction";
import { SelectFormField } from "src/app/shared/material-form-builder/form-components/select/select-form-field";
import { ReferentieTabelService } from "src/app/admin/referentie-tabel.service";
import { TranslateService } from "@ngx-translate/core";
import { UtilService } from "src/app/core/service/util.service";
import { Vertrouwelijkheidaanduiding } from "src/app/informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { MedewerkerGroepFieldBuilder } from "src/app/shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-field-builder";
import { SelectFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/select/select-form-field-builder";
import { InputFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/input/input-form-field-builder";
import { TextareaFormFieldBuilder } from "src/app/shared/material-form-builder/form-components/textarea/textarea-form-field-builder";
import { TextareaFormField } from "src/app/shared/material-form-builder/form-components/textarea/textarea-form-field";
import { AbstractFormField } from "src/app/shared/material-form-builder/model/abstract-form-field";
import { NavigationService } from "src/app/shared/navigation/navigation.service";
import { OrderUtil } from "src/app/shared/order/order-util";

@Component({
  selector: "zac-case-details-edit",
  templateUrl: "./zaak-wijzigen.component.html",
  styleUrls: ["./zaak-wijzigen.component.less"],
})
export class ZaakWijzigenComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() sideNav: MatDrawer;
  @Input() readonly: boolean;
  @Input() zaak: GeneratedType<"RestZaak">;

  formFields: Array<AbstractFormField[]>;
  formConfig: FormConfig;

  private medewerkerGroepFormField: MedewerkerGroepFormField;
  private communicatiekanalen: Observable<string[]>;
  private communicatiekanaalField: SelectFormField;
  private vertrouwelijkheidaanduidingField: SelectFormField;
  private vertrouwelijkheidaanduidingen: { label: string; value: string }[];
  private toelichtingField: TextareaFormField;
  private ngDestroy = new Subject<void>();

  constructor(
    private referentieTabelService: ReferentieTabelService,
    private navigation: NavigationService,
    private translateService: TranslateService,
    private utilService: UtilService,
  ) {}

  ngOnInit(): void {
    console.log("zaak", this.zaak);

    this.initForm();
  }

  ngAfterViewInit(): void {}

  private initForm(): void {
    this.formConfig = new FormConfigBuilder()
      .saveText("actie.aanmaken")
      .cancelText("actie.annuleren")
      .build();

    this.communicatiekanalen = this.referentieTabelService
      .listCommunicatiekanalen
      // this.inboxProductaanvraag != null,
      ();

    this.vertrouwelijkheidaanduidingen = this.utilService.getEnumAsSelectList(
      "vertrouwelijkheidaanduiding",
      Vertrouwelijkheidaanduiding,
    );

    this.medewerkerGroepFormField = this.getMedewerkerGroupFormField(
      this.zaak?.groep.id,
      this.zaak?.behandelaar.id,
    );

    this.communicatiekanaalField = new SelectFormFieldBuilder(
      this.zaak.communicatiekanaal,
    )
      // ZaakCreateComponent.KANAAL_E_FORMULIER,
      .id("communicatiekanaal")
      .label("communicatiekanaal")
      .options(this.communicatiekanalen)
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

    const omschrijving = new InputFormFieldBuilder(this.zaak.omschrijving)
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

    this.formFields = [
      [this.medewerkerGroepFormField],
      [this.communicatiekanaalField, this.vertrouwelijkheidaanduidingField],
      [omschrijving],
      [this.toelichtingField],
    ];
  }

  onFormSubmit(formGroup: FormGroup): void {
    if (formGroup) {
      this.navigation.back();
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
      .id("toekenning")
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
