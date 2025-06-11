/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
} from "@angular/core";
import { FormControl, Validators } from "@angular/forms";
import { MatDrawer, MatSidenav } from "@angular/material/sidenav";
import { MatTable, MatTableDataSource } from "@angular/material/table";
import { Router } from "@angular/router";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BAGService } from "../../bag.service";

@Component({
  selector: "zac-bag-zoek",
  templateUrl: "./bag-zoek.component.html",
  styleUrls: ["./bag-zoek.component.less"],
})
export class BagZoekComponent {
  @Output() bagObject = new EventEmitter<GeneratedType<"RESTBAGObject">>();
  @Input() gekoppeldeBagObjecten:
    | GeneratedType<"RESTBAGObject">[]
    | FormControl<GeneratedType<"RESTBAGObject">[] | null> = [];
  @Input({ required: true }) sideNav!: MatSidenav | MatDrawer;
  @ViewChild(MatTable) table!: MatTable<GeneratedType<"RESTBAGObject">>;
  trefwoorden = new FormControl("", [Validators.maxLength(255)]);
  bagObjecten = new MatTableDataSource<
    GeneratedType<"RESTBAGObject"> | GeneratedType<"RESTBAGAdres">
  >();
  loading = false;
  columns: string[] = ["expand", "id", "type", "omschrijving", "acties"];

  constructor(
    private bagService: BAGService,
    private utilService: UtilService,
    private router: Router,
  ) {}

  zoek(): void {
    this.bagObjecten.data = [];
    if (this.trefwoorden.value) {
      this.loading = true;
      this.utilService.setLoading(true);
      this.bagService
        .listAdressen({
          trefwoorden: this.trefwoorden.value,
        })

        .subscribe((adressen) => {
          this.bagObjecten.data = adressen.resultaten ?? [];
          this.loading = false;
          this.utilService.setLoading(false);
        });
    }
  }

  selectBagObject(bagObject: GeneratedType<"RESTBAGObject">): void {
    if (this.gekoppeldeBagObjecten instanceof FormControl) {
      this.gekoppeldeBagObjecten.setValue([
        ...(this.gekoppeldeBagObjecten.value ?? []),
        bagObject,
      ]);
    } else {
      this.gekoppeldeBagObjecten.push(bagObject);
    }
    this.bagObject.emit(bagObject);
  }

  expandable(bagObject: GeneratedType<"RESTBAGObject">) {
    if (bagObject.bagObjectType !== "ADRES") {
      return false;
    }

    const adres: GeneratedType<"RESTBAGAdres"> = bagObject;
    return (
      adres.openbareRuimte ||
      adres.nummeraanduiding ||
      adres.woonplaats ||
      adres.panden?.length
    );
  }

  expand(
    bagObject: GeneratedType<"RESTBAGObject" | "RESTBAGAdres"> & {
      expanded: boolean;
    },
  ) {
    this.bagObjecten.data = this.bagObjecten.data.filter(
      (b) => (b as { child?: boolean })["child"] !== true,
    );
    if (bagObject.expanded) {
      bagObject.expanded = false;
      return;
    }

    this.bagObjecten.data.forEach(
      (b) => ((b as { expanded?: boolean })["expanded"] = false),
    );
    bagObject.expanded = true;

    const children: (GeneratedType<"RESTBAGObject" | "RESTBAGAdres"> & {
      expanded?: boolean;
      child?: boolean;
    })[] = [];
    if (bagObject.bagObjectType === "ADRES") {
      const adres: GeneratedType<"RESTBAGAdres"> = bagObject;
      if (adres.nummeraanduiding) {
        children.push(adres.nummeraanduiding);
      }
      if (adres.openbareRuimte) {
        children.push(adres.openbareRuimte);
      }
      if (adres.woonplaats) {
        children.push(adres.woonplaats);
      }
      if (adres.panden?.length) {
        adres.panden.forEach((p) => children.push(p));
      }
    }
    children.forEach((d) => (d["child"] = true));
    this.bagObjecten.data.splice(
      this.bagObjecten.data.indexOf(bagObject) + 1,
      0,
      ...children,
    );
    this.table.renderRows();
  }

  reedsGekoppeld(row: GeneratedType<"RESTBAGObject">): boolean {
    const objects =
      this.gekoppeldeBagObjecten instanceof FormControl
        ? (this.gekoppeldeBagObjecten.value ?? [])
        : this.gekoppeldeBagObjecten;
    return objects.some(
      (b) =>
        b.identificatie === row.identificatie &&
        b.bagObjectType === row.bagObjectType,
    );
  }

  openBagTonenPagina(bagObject: GeneratedType<"RESTBAGObject">): void {
    this.sideNav?.close();
    this.router.navigate([
      "/bag-objecten",
      bagObject.bagObjectType?.toLowerCase(),
      bagObject.identificatie,
    ]);
  }

  wissen() {
    this.trefwoorden.reset();
    this.bagObjecten.data = [];
  }
}
