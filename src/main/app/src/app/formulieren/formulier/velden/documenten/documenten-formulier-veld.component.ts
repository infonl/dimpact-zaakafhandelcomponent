/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import { Component, Input, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatTableDataSource } from "@angular/material/table";
import { of } from "rxjs";
import {
  mapDocumentenToString,
  mapStringToDocumentenStrings,
} from "../../../../documenten/document-utils";
import { InformatieObjectenService } from "../../../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-documenten-formulier-veld",
  templateUrl: "./documenten-formulier-veld.component.html",
  styleUrls: ["./documenten-formulier-veld.component.less"],
  standalone: false,
})
export class DocumentenFormulierVeldComponent implements OnInit {
  @Input({ required: true })
  veldDefinitie!: GeneratedType<"RESTFormulierVeldDefinitie">;
  @Input({ required: true }) control!: FormControl;
  @Input({ required: true }) zaak!: GeneratedType<"RestZaak">;
  columns = [
    "select",
    "titel",
    "documentType",
    "status",
    "versie",
    "auteur",
    "creatiedatum",
    "bestandsomvang",
    "indicaties",
    "url",
  ] as const;
  loading = false;

  dataSource = new MatTableDataSource<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();
  selection = new SelectionModel<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >(true, []);

  constructor(
    private readonly informatieObjectenService: InformatieObjectenService,
  ) {}

  ngOnInit() {
    this.ophalenDocumenten();
  }

  ophalenDocumenten() {
    const observable = this.getDocumentenObservable();
    observable.subscribe((documenten) => {
      this.dataSource.data = documenten;
      documenten.forEach((document) => {
        if (
          document.uuid &&
          this.veldDefinitie.defaultWaarde?.includes(document.uuid)
        ) {
          this.selection.toggle(document);
        }
      });
    });
  }

  private getDocumentenObservable() {
    switch (this.veldDefinitie.meerkeuzeOpties) {
      case "ZAAK_VERZENDBAAR":
        return this.informatieObjectenService.listInformatieobjectenVoorVerzenden(
          this.zaak.uuid,
        );
      case "ZAAK":
        return this.informatieObjectenService.listEnkelvoudigInformatieobjecten(
          {
            zaakUUID: this.zaak.uuid,
          },
        );
      default:
        return this.getDocumentenVariable();
    }
  }

  toggleCheckbox(document: GeneratedType<"RestEnkelvoudigInformatieobject">) {
    this.selection.toggle(document);
    const selected = this.selection.selected.map((value) => value.uuid);
    this.control.setValue(mapDocumentenToString(selected));
  }

  selectDisabled() {
    return this.control.disabled;
  }

  isSelected(document: GeneratedType<"RestEnkelvoudigInformatieobject">) {
    return this.selection.isSelected(document);
  }

  getDocumentenVariable() {
    if (!this.veldDefinitie.meerkeuzeOpties) return of([]);
    if (!this.zaak.zaakdata) return of([]);
    const uuids = this.zaak.zaakdata[this.veldDefinitie.meerkeuzeOpties];
    if (!uuids) return of([]);

    return this.informatieObjectenService.listEnkelvoudigInformatieobjecten({
      informatieobjectUUIDs: mapStringToDocumentenStrings(String(uuids)),
    });
  }
}
