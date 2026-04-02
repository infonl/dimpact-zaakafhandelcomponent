/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
} from "@angular/core";
import { FormControl, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatDrawer, MatSidenav } from "@angular/material/sidenav";
import { MatSortModule } from "@angular/material/sort";
import {
  MatTable,
  MatTableDataSource,
  MatTableModule,
} from "@angular/material/table";
import { MatToolbarModule } from "@angular/material/toolbar";
import { Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../../core/service/util.service";
import { EmptyPipe } from "../../../shared/pipes/empty.pipe";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BAGService } from "../../bag.service";

@Component({
  selector: "zac-bag-zoek",
  templateUrl: "./bag-zoek.component.html",
  styleUrls: ["./bag-zoek.component.less"],
  standalone: true,
  imports: [
    CommonModule,
    EmptyPipe,
    MatButtonModule,
    MatDividerModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSortModule,
    MatTableModule,
    MatToolbarModule,
    ReactiveFormsModule,
    TranslateModule,
  ],
})
export class BagZoekComponent {
  @Output() bagObject = new EventEmitter<GeneratedType<"RESTBAGObject">>();
  @Input() gekoppeldeBagObjecten:
    | GeneratedType<"RESTBAGObject">[]
    | FormControl<GeneratedType<"RESTBAGObject">[] | null> = [];
  @Input({ required: true }) sideNav!: MatSidenav | MatDrawer;
  @ViewChild(MatTable) private table!: MatTable<GeneratedType<"RESTBAGObject">>;
  protected trefwoorden = new FormControl("", [Validators.maxLength(255)]);
  protected bagObjecten = new MatTableDataSource<
    GeneratedType<"RESTBAGObject"> | GeneratedType<"RESTBAGAdres">
  >();
  protected loading = false;
  protected columns: string[] = ["expand", "id", "type", "omschrijving", "acties"];

  constructor(
    private bagService: BAGService,
    private utilService: UtilService,
    private router: Router,
  ) {}

  protected zoek() {
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

  protected selectBagObject(bagObject: GeneratedType<"RESTBAGObject">) {
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

  protected expandable(bagObject: GeneratedType<"RESTBAGObject">) {
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

  protected expand(
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

  protected reedsGekoppeld(row: GeneratedType<"RESTBAGObject">): boolean {
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

  protected openBagTonenPagina(bagObject: GeneratedType<"RESTBAGObject">) {
    this.sideNav?.close();
    this.router.navigate([
      "/bag-objecten",
      bagObject.bagObjectType?.toLowerCase(),
      bagObject.identificatie,
    ]);
  }

  protected wissen() {
    this.trefwoorden.reset();
    this.bagObjecten.data = [];
  }
}
