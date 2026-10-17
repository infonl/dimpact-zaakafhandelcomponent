/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import {
  provideHttpClient,
  withInterceptorsFromDi,
  withJsonpSupport,
} from "@angular/common/http";
import { ModuleWithProviders, NgModule } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { GoogleMapsModule } from "@angular/google-maps";
import {
  MAT_MOMENT_DATE_ADAPTER_OPTIONS,
  MomentDateAdapter,
} from "@angular/material-moment-adapter";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatButtonModule } from "@angular/material/button";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatChipsModule } from "@angular/material/chips";
import {
  DateAdapter,
  MAT_DATE_FORMATS,
  MAT_DATE_LOCALE,
} from "@angular/material/core";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatListModule } from "@angular/material/list";
import { MatMenuModule } from "@angular/material/menu";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatRadioModule } from "@angular/material/radio";
import { MatSelectModule } from "@angular/material/select";
import { MatTableModule } from "@angular/material/table";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { NgxEditorModule } from "ngx-editor";
import { FileDragAndDropDirective } from "../directives/file-drag-and-drop.directive";
import { DocumentIconComponent } from "../document-icon/document-icon.component";
import { InformatieObjectIndicatiesComponent } from "../indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { EnhanceMatErrorDirective } from "../material/mat-zac-error";
import { CapitalizeFirstLetterPipe } from "../pipes/capitalizeFirstLetter.pipe";
import { PipesModule } from "../pipes/pipes.module";
import { AutocompleteComponent } from "./form-components/autocomplete/autocomplete.component";
import { CheckboxComponent } from "./form-components/checkbox/checkbox.component";
import { DateComponent } from "./form-components/date/date.component";
import { DividerComponent } from "./form-components/divider/divider.component";
import { DocumentenLijstComponent } from "./form-components/documenten-lijst/documenten-lijst.component";
import { DocumentenOndertekenenComponent } from "./form-components/documenten-ondertekenen/documenten-ondertekenen.component";
import { FileComponent } from "./form-components/file/file.component";
import { GoogleMapsComponent } from "./form-components/google-maps/google-maps.component";
import { HeadingComponent } from "./form-components/heading/heading.component";
import { HiddenComponent } from "./form-components/hidden/hidden.component";
import { HtmlEditorVariabelenKiesMenuComponent } from "./form-components/html-editor/html-editor-variabelen-kies-menu.component";
import { HtmlEditorComponent } from "./form-components/html-editor/html-editor.component";
import { InputComponent } from "./form-components/input/input.component";
import { MedewerkerGroepComponent } from "./form-components/medewerker-groep/medewerker-groep.component";
import { MessageComponent } from "./form-components/message/message.component";
import { ParagraphComponent } from "./form-components/paragraph/paragraph.component";
import { RadioComponent } from "./form-components/radio/radio.component";
import { ReadonlyComponent } from "./form-components/readonly/readonly.component";
import { SelectComponent } from "./form-components/select/select.component";
import { TaakDocumentUploadComponent } from "./form-components/taak-document-upload/taak-document-upload.component";
import { TextareaComponent } from "./form-components/textarea/textarea.component";
import { FormFieldComponent } from "./form/form-field/form-field.component";
import { FormFieldDirective } from "./form/form-field/form-field.directive";
import { FormComponent } from "./form/form/form.component";
import {
  BUILDER_CONFIG,
  MaterialFormBuilderConfig,
} from "./material-form-builder-config";

@NgModule({
  declarations: [
    FormComponent,
    FormFieldComponent,
    DateComponent,
    HeadingComponent,
    HtmlEditorComponent,
    HtmlEditorVariabelenKiesMenuComponent,
    InputComponent,
    FileComponent,
    SelectComponent,
    MedewerkerGroepComponent,
    CheckboxComponent,
    TextareaComponent,
    GoogleMapsComponent,
    FormFieldDirective,
    ReadonlyComponent,
    AutocompleteComponent,
    DocumentenLijstComponent,
    DocumentenOndertekenenComponent,
    TaakDocumentUploadComponent,
    RadioComponent,
    ParagraphComponent,
    DividerComponent,
    HiddenComponent,
    MessageComponent,
    EnhanceMatErrorDirective,
    CapitalizeFirstLetterPipe,
  ],
  exports: [
    FileDragAndDropDirective,
    FormComponent,
    FormFieldComponent,
    DateComponent,
    HeadingComponent,
    HtmlEditorComponent,
    InputComponent,
    FileComponent,
    SelectComponent,
    MedewerkerGroepComponent,
    CheckboxComponent,
    TextareaComponent,
    GoogleMapsComponent,
    AutocompleteComponent,
    DocumentenLijstComponent,
    DocumentenOndertekenenComponent,
    TaakDocumentUploadComponent,
    RadioComponent,
    ParagraphComponent,
    MessageComponent,
  ],
  imports: [
    FileDragAndDropDirective,
    CommonModule,
    ReactiveFormsModule,
    BrowserAnimationsModule,
    GoogleMapsModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatIconModule,
    MatButtonModule,
    MatSelectModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatAutocompleteModule,
    MatChipsModule,
    TranslateModule,
    PipesModule,
    MatTableModule,
    RouterModule,
    FormsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    NgxEditorModule,
    MatMenuModule,
    MatListModule,
    DocumentIconComponent,
    InformatieObjectIndicatiesComponent,
  ],
  providers: [
    {
      provide: DateAdapter,
      useClass: MomentDateAdapter,
      deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS],
    },
    {
      provide: MAT_MOMENT_DATE_ADAPTER_OPTIONS,
      useValue: {
        strict: false,
      },
    },
    {
      provide: MAT_DATE_FORMATS,
      useValue: {
        parse: {
          dateInput: "DD-MM-YYYY",
        },
        display: {
          dateInput: "DD-MM-YYYY",
          monthYearLabel: "MMMM YYYY",
          dateA11yLabel: "LL",
          monthYearA11yLabel: "MMMM YYYY",
        },
      },
    },
    provideHttpClient(withInterceptorsFromDi(), withJsonpSupport()),
  ],
})
export class MaterialFormBuilderModule {
  static forRoot(
    config?: MaterialFormBuilderConfig,
  ): ModuleWithProviders<MaterialFormBuilderModule> {
    return {
      ngModule: MaterialFormBuilderModule,
      providers: [{ provide: BUILDER_CONFIG, useValue: config }],
    };
  }
}
