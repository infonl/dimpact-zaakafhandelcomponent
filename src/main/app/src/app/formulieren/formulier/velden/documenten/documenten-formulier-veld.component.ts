/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import { Component, Input, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatCheckboxChange } from "@angular/material/checkbox";
import { MatTableDataSource } from "@angular/material/table";
import { Observable, of } from "rxjs";
import { FormulierVeldDefinitie } from "../../../../admin/model/formulieren/formulier-veld-definitie";
import { InformatieObjectenService } from "../../../../informatie-objecten/informatie-objecten.service";
import { InformatieobjectZoekParameters } from "../../../../informatie-objecten/model/informatieobject-zoek-parameters";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { Zaak } from "../../../../zaken/model/zaak";

@Component({
  selector: "zac-documenten-formulier-veld",
  templateUrl: "./documenten-formulier-veld.component.html",
  styleUrls: ["./documenten-formulier-veld.component.less"],
})
export class DocumentenFormulierVeldComponent implements OnInit {
  @Input() veldDefinitie: FormulierVeldDefinitie;
  @Input() control: FormControl;
  @Input() zaak: Zaak;
  columns: string[] = [
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
  ];
  loading = false;

  dataSource = new MatTableDataSource<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >();
  selection = new SelectionModel<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >(true, []);

  constructor(public informatieObjectenService: InformatieObjectenService) {}

  ngOnInit(): void {
    this.ophalenDocumenten();
  }

  ophalenDocumenten() {
    let observable: Observable<
      GeneratedType<"RestEnkelvoudigInformatieobject">[]
    >;
    if (this.veldDefinitie.meerkeuzeOpties === "ZAAK_VERZENDBAAR") {
      observable =
        this.informatieObjectenService.listInformatieobjectenVoorVerzenden(
          this.zaak.uuid,
        );
    } else if (this.veldDefinitie.meerkeuzeOpties === "ZAAK") {
      const zoekparameters = new InformatieobjectZoekParameters();
      zoekparameters.zaakUUID = this.zaak.uuid;
      observable =
        this.informatieObjectenService.listEnkelvoudigInformatieobjecten(
          zoekparameters,
        );
    } else {
      observable = this.getDocumentenVariable();
    }
    observable.subscribe((documenten) => {
      this.dataSource.data = documenten;
      documenten.forEach((document) => {
        if (this.veldDefinitie.defaultWaarde?.includes(document.uuid)) {
          this.selection.toggle(document);
        }
      });
    });
  }

  toggleCheckbox(
    $event: MatCheckboxChange,
    document: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ): void {
    this.selection.toggle(document);
    this.control.setValue(
      this.selection.selected.map((value) => value.uuid).join(";"),
    );
  }

  selectDisabled(): boolean {
    return this.control.disabled;
  }

  isSelected(
    document: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ): boolean {
    return this.selection.isSelected(document);
  }

  getDocumentenVariable(): Observable<
    GeneratedType<"RestEnkelvoudigInformatieobject">[]
  > {
    const uuids: string =
      this.zaak.zaakdata[this.veldDefinitie.meerkeuzeOpties];
    if (uuids) {
      const zoekparameters = new InformatieobjectZoekParameters();
      zoekparameters.informatieobjectUUIDs = uuids.split(";");
      return this.informatieObjectenService.listEnkelvoudigInformatieobjecten(
        zoekparameters,
      );
    }
    return of([]);
  }
}
