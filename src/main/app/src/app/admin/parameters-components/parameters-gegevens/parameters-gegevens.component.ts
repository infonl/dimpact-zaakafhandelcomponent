/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatStepperModule } from "@angular/material/stepper";
import { TranslateModule } from "@ngx-translate/core";
import { Observable } from "rxjs";
import { MaterialFormBuilderModule } from "../../../shared/material-form-builder/material-form-builder.module";
import { StaticTextComponent } from "../../../shared/static-text/static-text.component";
import { GeneratedType } from "../../../shared/utils/generated-types";

export interface ParametersGegevensFormControls {
  defaultGroep: FormControl<GeneratedType<"RestGroup"> | null>;
  defaultBehandelaar: FormControl<GeneratedType<"RestUser"> | null>;
  productaanvraagtype: FormControl<string | null>;
}

/**
 * Shape shared by `RestZaaktype` and `RestZaaktypeOverzicht`, the two generated
 * types the CMMN and BPMN parameters carry their zaaktype in.
 */
export interface ZaaktypeIdentificatie {
  uuid: string;
  identificatie?: string | null;
  doel?: string | null;
  omschrijving?: string | null;
  servicenorm?: boolean | null;
}

/**
 * Fields shared by the CMMN and BPMN "Gegevens" step. The process-definition select
 * (`caseDefinition` vs. `bpmnDefinition`) and any variant-only fields are provided by
 * the caller via content projection, since their form keys and option types differ.
 */
@Component({
  selector: "zac-parameters-gegevens",
  templateUrl: "./parameters-gegevens.component.html",
  styleUrls: ["./parameters-gegevens.component.less"],
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatStepperModule,
    TranslateModule,
    MaterialFormBuilderModule,
    StaticTextComponent,
  ],
})
export class ParametersGegevensComponent {
  @Input({ required: true }) form!: FormGroup<ParametersGegevensFormControls>;
  @Input({ required: true }) zaaktype!: ZaaktypeIdentificatie;
  @Input({ required: true }) zaakspecifiekAutoriseerbaar: boolean | undefined;
  @Input({ required: true })
  groepen!: Observable<GeneratedType<"RestGroup">[]>;
  @Input({ required: true }) medewerkers!: GeneratedType<"RestLoggedInUser">[];
  @Input({ required: true }) isValid!: boolean;
  @Input() isLoading = false;
  @Output() opslaan = new EventEmitter<void>();
}
