/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import {
  booleanAttribute,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
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
import { MatTableModule } from "@angular/material/table";
import { MatToolbarModule } from "@angular/material/toolbar";
import { Router } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { catchError, EMPTY, finalize } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { TypedMatCellDefDirective } from "../../shared/directives/typed-mat-cell-def.directive";
import { trackControl } from "../../shared/form/control-state";
import { runQuery } from "../../shared/http/run-query";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BAGService } from "../bag.service";

type BagZoekRij = GeneratedType<"RestBagObject" | "RestBagAdres"> & {
  expanded?: boolean;
  child?: boolean;
};

type BagObjectTypeSchemas = {
  ADRES: "RestBagAdres";
  NUMMERAANDUIDING: "RestNummeraanduiding";
  PAND: "RestPand";
  OPENBARE_RUIMTE: "RestOpenbareRuimte";
  WOONPLAATS: "RestWoonplaats";
};

@Component({
  selector: "zac-bag-zoek",
  templateUrl: "./bag-zoek.component.html",
  styleUrls: ["./bag-zoek.component.less"],
  standalone: true,
  imports: [
    I18nKeyPipe,
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
    TypedMatCellDefDirective,
    MatToolbarModule,
    ReactiveFormsModule,
    TranslatePipe,
  ],
})
export class BagZoekComponent {
  private readonly bagService = inject(BAGService);
  private readonly utilService = inject(UtilService);
  private readonly router = inject(Router);

  readonly bagObject = output<GeneratedType<"RestBagObject">>();
  readonly isSelectable = input(false, { transform: booleanAttribute });
  readonly gekoppeldeBagObjecten = input<
    | GeneratedType<"RestBagObject">[]
    | FormControl<GeneratedType<"RestBagObject">[] | null>
  >([]);
  readonly sideNav = input.required<MatSidenav | MatDrawer>();
  protected trefwoorden = new FormControl("", [Validators.maxLength(255)]);
  private readonly readTrefwoorden = trackControl(signal(this.trefwoorden));
  protected readonly trefwoordenInvalid = this.readTrefwoorden(
    (control) => control.invalid,
  );
  protected readonly trefwoordenLength = this.readTrefwoorden(
    (control) => control.value?.length ?? 0,
  );
  private readonly gekoppeldeBagObjectenControl = computed(() => {
    const gekoppeldeBagObjecten = this.gekoppeldeBagObjecten();
    return gekoppeldeBagObjecten instanceof FormControl
      ? gekoppeldeBagObjecten
      : null;
  });
  private readonly gekoppeldeBagObjectenControlValue = trackControl(
    this.gekoppeldeBagObjectenControl,
  )((control) => control.value);
  protected readonly bagObjecten = signal<BagZoekRij[]>([]);
  protected readonly loading = signal(false);
  protected columns: string[] = [
    "expand",
    "id",
    "type",
    "omschrijving",
    "acties",
  ];

  private readonly queryClient = inject(QueryClient);

  protected zoek() {
    this.bagObjecten.set([]);
    if (this.trefwoorden.value) {
      this.loading.set(true);
      this.utilService.setLoading(true);
      runQuery(
        this.queryClient,
        this.bagService.listAdressen({
          trefwoorden: this.trefwoorden.value,
        }),
      )
        .pipe(
          catchError(() => EMPTY),
          finalize(() => {
            this.loading.set(false);
            this.utilService.setLoading(false);
          }),
        )
        .subscribe((adressen) => {
          this.bagObjecten.set(adressen.resultaten ?? []);
        });
    }
  }

  protected selectBagObject(bagObject: GeneratedType<"RestBagObject">) {
    const gekoppeldeBagObjecten = this.gekoppeldeBagObjecten();
    if (gekoppeldeBagObjecten instanceof FormControl) {
      gekoppeldeBagObjecten.setValue([
        ...(gekoppeldeBagObjecten.value ?? []),
        bagObject,
      ]);
    } else {
      gekoppeldeBagObjecten.push(bagObject);
    }
    this.bagObject.emit(bagObject);
  }

  protected expandable(bagObject: GeneratedType<"RestBagObject">) {
    if (bagObject.bagObjectType !== "ADRES") {
      return false;
    }

    const adres: GeneratedType<"RestBagAdres"> = bagObject;
    return (
      adres.openbareRuimte ||
      adres.nummeraanduiding ||
      adres.woonplaats ||
      adres.panden?.length
    );
  }

  protected isBagObjectType<T extends keyof BagObjectTypeSchemas>(
    row: BagZoekRij,
    bagObjectType: T,
  ): row is BagZoekRij & GeneratedType<BagObjectTypeSchemas[T]> {
    return row.bagObjectType === bagObjectType;
  }

  protected expand(bagObject: BagZoekRij) {
    const rows = this.bagObjecten()
      .filter((row) => !row.child)
      .map((row) => (row.expanded ? { ...row, expanded: false } : row));
    if (bagObject.expanded) {
      this.bagObjecten.set(rows);
      return;
    }

    const children: BagZoekRij[] = [];
    if (bagObject.bagObjectType === "ADRES") {
      const adres: GeneratedType<"RestBagAdres"> = bagObject;
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
    const index = rows.indexOf(bagObject);
    rows.splice(
      index,
      1,
      { ...bagObject, expanded: true },
      ...children.map((child) => ({ ...child, child: true })),
    );
    this.bagObjecten.set(rows);
  }

  protected reedsGekoppeld(row: GeneratedType<"RestBagObject">): boolean {
    const gekoppeldeBagObjecten = this.gekoppeldeBagObjecten();
    const objects =
      gekoppeldeBagObjecten instanceof FormControl
        ? (this.gekoppeldeBagObjectenControlValue() ?? [])
        : gekoppeldeBagObjecten;
    return objects.some(
      (b) =>
        b.identificatie === row.identificatie &&
        b.bagObjectType === row.bagObjectType,
    );
  }

  protected openBagTonenPagina(bagObject: GeneratedType<"RestBagObject">) {
    this.sideNav().close();
    this.router.navigate([
      "/bag-objecten",
      bagObject.bagObjectType?.toLowerCase(),
      bagObject.identificatie,
    ]);
  }

  protected wissen() {
    this.trefwoorden.reset();
    this.bagObjecten.set([]);
  }
}
