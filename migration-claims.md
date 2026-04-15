<!--
  COORDINATION FILE — branch: chore/anguklar-19-migration--collaboration-list--no-merging_keep_me
  This branch is NEVER merged into main. It is a shared whiteboard only.
  Protocol: pull → claim your batch → push → switch back to your work branch.
-->

# Standalone Migration — Claims

## Marcel

### Batch 1 — PR: `chore/PZ-10681--FE--Angular-v19-migration--DateRangeFilterComponent--ParametersComponent`
- [x] `shared/table-zoek-filters/date-range-filter/date-range-filter.component.ts`
- [x] `admin/parameters/parameters.component.ts`

### Batch 2 — done (branch: `temp/standalone-migration`)
- [x] `admin/parameters-select-process-model-method/parameters-select-process-model-method.component.ts`
- [x] `admin/parameters-edit-bpmn/parameters-edit-bpmn.component.ts`
- [x] `admin/parameters-edit-shell/parameters-edit-shell.component.ts`

### Batch 4 — done (branch: `temp/standalone-migration`)
- [x] `zoeken/zoek-object/zoek-object-link/zoek-object-link.component.ts`

### Batch 5 — done (branch: `temp/standalone-migration`)
- [x] `zoeken/zoek/filters/date-filter/date-filter.component.ts`

### Batch 3 — done (branch: `temp/standalone-migration`)
- [x] `shared/indicaties/indicaties.component.ts` (abstract base)
- [x] `shared/indicaties/besluit-indicaties/besluit-indicaties.component.ts`
- [x] `shared/indicaties/persoon-indicaties/persoon-indicaties.component.ts`
- [x] `shared/indicaties/zaak-indicaties/zaak-indicaties.component.ts`

### In progress
- [x] FacetFilterComponent
- [x] TaakEditComponent
- [x] IdentityComponent
- [x] ZoekopdrachtComponent
- [x] SignaleringenSettingsComponent
- [x] FoutAfhandelingComponent

### Batch 6 — claimed
- [x] KlantContactmomentenTabelComponent
- [x] ZaakBetrokkeneFilterComponent
- [x] OntvangstbevestigingComponent
- [x] TakenVrijgevenDialogComponent
- [x] ZoekopdrachtSaveDialogComponent

### PZ-10533 feature branch — done
- [x] `klanten/persoonsgegevens/persoonsgegevens.component.ts`
- [x] `klanten/bedrijfsgegevens/bedrijfsgegevens.component.ts`

### PZ-10629 feature branch — done
- [x] `mail/mail-create/mail-create.component.ts`
- [x] `shared/form/form-actions/form-actions.component.ts`
- [x] `zaken/intake-afronden-dialog/intake-afronden-dialog.component.ts`
- [x] `zaken/zaak-afhandelen-dialog/zaak-afhandelen-dialog.component.ts`

### Batch 7 — claimed
- [ ] DialogComponent
- [ ] EditComponent
- [ ] ColumnPickerComponent
- [ ] TakenCardComponent
- [x] InboxDocumentenListComponent
- [x] OntkoppeldeDocumentenListComponent
- [ ] InboxProductaanvragenListComponent

### Bundle size — done
Standalone + lazy-load OpenLayers/proj4 map components via route-lazy BAGModule:
- [x] `shared/pipes/empty.pipe.ts` — made standalone (enabler for BagZoekComponent)
- [x] `bag/zoek/bag-zoek/bag-zoek.component.ts` — made standalone (enabler for BAGModule lazy load)
- [x] `bag/bag-locatie/bag-locatie.component.ts` — lazy-loaded via BAGModule `loadChildren`
- [x] `bag/bag-zaken-tabel/bag-zaken-tabel.component.ts`
- [x] `bag/bag-view/bag-view.component.ts`
- [x] `zaken/zaak-locatie-tonen/zaak-locatie-tonen.component.ts`
- [x] `zaken/zaak-locatie-wijzigen/zaak-locatie-wijzigen.component.ts`

## Dax

### Batch 1 — PR: `chore/PZ-10683--FE--Angular-v19-migration--NotificationDialogComponent--TekstFilterComponent--ConfirmDialogComponent`
- ⏭ `shared/material/mat-zac-error.ts` — skipped, declared in ATOS `MaterialFormBuilderModule`
- [x] `shared/notification-dialog/notification-dialog.component.ts`
- [x] `shared/table-zoek-filters/tekst-filter/tekst-filter.component.ts`
- [x] `shared/confirm-dialog/confirm-dialog.component.ts`

### Batch 2 — done (branch: `temp/standalone-migration`)
- [x] `fout-afhandeling/dialog/fout-dialog.component.ts`
- [x] `fout-afhandeling/dialog/actie-onmogelijk-dialog.component.ts`

### Batch 3 — done (branch: `temp/standalone-migration`)
- [x] `taken/taken-verdelen-dialog/taken-verdelen-dialog.component.ts`
- [x] `notities/notities.component.ts`

## Colleague

<!-- Add your claimed components here before starting a batch -->
