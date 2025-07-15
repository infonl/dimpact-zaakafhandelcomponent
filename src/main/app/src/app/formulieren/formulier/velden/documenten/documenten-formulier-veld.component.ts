/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import { Component, Input, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatTableDataSource } from "@angular/material/table";
import { of } from "rxjs";
import { InformatieObjectenService } from "../../../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../../../shared/utils/generated-types";

@Component({
  selector: "zac-documenten-formulier-veld",
  templateUrl: "./documenten-formulier-veld.component.html",
  styleUrls: ["./documenten-formulier-veld.component.less"],
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
    this.control.setValue(
      this.selection.selected.map((value) => value.uuid).join(";"),
    );
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
      informatieobjectUUIDs: String(uuids).split(";"),
    });
  }
}
