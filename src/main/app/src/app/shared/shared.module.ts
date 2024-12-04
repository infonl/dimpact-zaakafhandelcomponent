/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DragDropModule } from "@angular/cdk/drag-drop";
import { APP_INITIALIZER, Injector, NgModule } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { MatPaginatorIntl } from "@angular/material/paginator";
import { Title } from "@angular/platform-browser";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { ConfirmDialogComponent } from "./confirm-dialog/confirm-dialog.component";
import { DialogComponent } from "./dialog/dialog.component";
import { OutsideClickDirective } from "./directives/outside-click.directive";
import { DocumentViewerComponent } from "./document-viewer/document-viewer.component";
import { ColumnPickerComponent } from "./dynamic-table/column-picker/column-picker.component";
import { NoStickyColumnPipe } from "./dynamic-table/pipes/no-sticky-column.pipe";
import { SortPipe } from "./dynamic-table/pipes/sort.pipe";
import { TooltipListPipe } from "./dynamic-table/pipes/tooltip-list.pipe";
import { EditAutocompleteComponent } from "./edit/edit-autocomplete/edit-autocomplete.component";
import { EditBehandelaarComponent } from "./edit/edit-behandelaar/edit-behandelaar.component";
import { EditDatumGroepComponent } from "./edit/edit-datum-groep/edit-datum-groep.component";
import { EditDatumComponent } from "./edit/edit-datum/edit-datum.component";
import { EditGroepBehandelaarComponent } from "./edit/edit-groep-behandelaar/edit-groep-behandelaar.component";
import { EditGroepComponent } from "./edit/edit-groep/edit-groep.component";
import { EditInputComponent } from "./edit/edit-input/edit-input.component";
import { EditSelectComponent } from "./edit/edit-select/edit-select.component";
import { EditTekstComponent } from "./edit/edit-tekst/edit-tekst.component";
import { ExportButtonComponent } from "./export-button/export-button.component";
import { BesluitIndicatiesComponent } from "./indicaties/besluit-indicaties/besluit-indicaties.component";
import { PersoonIndicatiesComponent } from "./indicaties/persoon-indicaties/persoon-indicaties.component";
import { ZaakIndicatiesComponent } from "./indicaties/zaak-indicaties/zaak-indicaties.component";
import { MaterialFormBuilderModule } from "./material-form-builder/material-form-builder.module";
import { MaterialModule } from "./material/material.module";
import { ZacNarrowMatCheckboxDirective } from "./material/narrow-checkbox.directive";
import { BackButtonDirective } from "./navigation/back-button.directive";
import { NotificationDialogComponent } from "./notification-dialog/notification-dialog.component";
import { paginatorLanguageInitializerFactory } from "./paginator/paginator-language-initializer";
import { PaginatorTranslator } from "./paginator/paginator-translator";
import { PipesModule } from "./pipes/pipes.module";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "./pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { ReadMoreComponent } from "./read-more/read-more.component";
import { SideNavComponent } from "./side-nav/side-nav.component";
import { StaticTextComponent } from "./static-text/static-text.component";
import { DateRangeFilterComponent } from "./table-zoek-filters/date-range-filter/date-range-filter.component";
import { FacetFilterComponent } from "./table-zoek-filters/facet-filter/facet-filter.component";
import { TekstFilterComponent } from "./table-zoek-filters/tekst-filter/tekst-filter.component";
import { ToggleFilterComponent } from "./table-zoek-filters/toggle-filter/toggle-filter.component";
import { VersionComponent } from "./version/version.component";

@NgModule({
  declarations: [
    SideNavComponent,
    BackButtonDirective,
    StaticTextComponent,
    ReadMoreComponent,
    OutsideClickDirective,
    EditBehandelaarComponent,
    EditGroepComponent,
    EditGroepBehandelaarComponent,
    EditDatumComponent,
    EditDatumGroepComponent,
    EditAutocompleteComponent,
    EditInputComponent,
    EditTekstComponent,
    EditSelectComponent,
    DateRangeFilterComponent,
    FacetFilterComponent,
    TekstFilterComponent,
    ToggleFilterComponent,
    ConfirmDialogComponent,
    DialogComponent,
    NoStickyColumnPipe,
    ColumnPickerComponent,
    TooltipListPipe,
    DocumentViewerComponent,
    NotificationDialogComponent,
    ExportButtonComponent,
    BesluitIndicatiesComponent,
    PersoonIndicatiesComponent,
    ZaakIndicatiesComponent,
    VersionComponent,
    SortPipe,
    ZacNarrowMatCheckboxDirective,
  ],
  imports: [
    FormsModule,
    BrowserAnimationsModule,
    RouterModule,
    PipesModule,
    MaterialModule,
    MaterialFormBuilderModule.forRoot(),
    TranslateModule,
    VertrouwelijkaanduidingToTranslationKeyPipe,
  ],
  exports: [
    BrowserAnimationsModule,
    FormsModule,
    TranslateModule,
    DragDropModule,
    SideNavComponent,
    BackButtonDirective,
    StaticTextComponent,
    ReadMoreComponent,
    PipesModule,
    MaterialModule,
    MaterialFormBuilderModule,
    EditBehandelaarComponent,
    EditGroepComponent,
    EditGroepBehandelaarComponent,
    EditDatumComponent,
    EditDatumGroepComponent,
    EditAutocompleteComponent,
    EditInputComponent,
    EditTekstComponent,
    EditSelectComponent,
    DateRangeFilterComponent,
    FacetFilterComponent,
    TekstFilterComponent,
    ToggleFilterComponent,
    DialogComponent,
    ConfirmDialogComponent,
    NoStickyColumnPipe,
    TooltipListPipe,
    DocumentViewerComponent,
    ColumnPickerComponent,
    ExportButtonComponent,
    BesluitIndicatiesComponent,
    PersoonIndicatiesComponent,
    ZaakIndicatiesComponent,
    VersionComponent,
    SortPipe,
    ZacNarrowMatCheckboxDirective,
    VertrouwelijkaanduidingToTranslationKeyPipe,
  ],
  providers: [
    Title,
    {
      provide: MatPaginatorIntl,
      deps: [TranslateService],
      useFactory: (translateService: TranslateService) =>
        new PaginatorTranslator(translateService).getTranslatedPaginator(),
    },
    {
      provide: APP_INITIALIZER,
      useFactory: paginatorLanguageInitializerFactory,
      deps: [TranslateService, Injector],
      multi: true,
    },
    {
      provide: VertrouwelijkaanduidingToTranslationKeyPipe,
      useClass: VertrouwelijkaanduidingToTranslationKeyPipe,
    },
  ],
})
export class SharedModule {}
