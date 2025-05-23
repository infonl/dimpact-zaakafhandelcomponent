/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  ViewChild,
} from "@angular/core";

import { FormControl } from "@angular/forms";
import { MatPaginator } from "@angular/material/paginator";
import { MatSidenav } from "@angular/material/sidenav";
import { merge } from "rxjs";
import { map, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { DocumentZoekObject } from "../model/documenten/document-zoek-object";
import { TaakZoekObject } from "../model/taken/taak-zoek-object";
import { ZaakZoekObject } from "../model/zaken/zaak-zoek-object";
import { ZoekObject } from "../model/zoek-object";
import { ZoekObjectType } from "../model/zoek-object-type";
import { ZoekParameters } from "../model/zoek-parameters";
import { ZoekResultaat } from "../model/zoek-resultaat";
import { ZoekType } from "../model/zoek-type";
import { ZoekVeld } from "../model/zoek-veld";
import { ZoekenService } from "../zoeken.service";

@Component({
  selector: "zac-zoeken",
  templateUrl: "./zoek.component.html",
  styleUrls: ["./zoek.component.less"],
})
export class ZoekComponent implements AfterViewInit, OnInit {
  @ViewChild("paginator") paginator!: MatPaginator;
  @Input({ required: true }) zoekenSideNav!: MatSidenav;

  zoekType: ZoekType = ZoekType.ZAC;
  ZoekType = ZoekType;
  ZoekVeld = ZoekVeld;
  readonly zoekObjectType = ZoekObjectType;
  zoekResultaat = new ZoekResultaat<ZoekObject>();
  zoekParameters: ZoekParameters = new ZoekParameters();
  isLoadingResults = true;
  slow = false;
  zoekveldControl = new FormControl<ZoekVeld>(ZoekVeld.ALLE);
  trefwoordenControl = new FormControl("");
  zoek = new EventEmitter<void>();
  hasSearched = false;
  hasTaken = false;
  hasZaken = false;
  hasDocument = false;
  huidigZoekVeld: ZoekVeld = ZoekVeld.ALLE;

  constructor(
    private zoekService: ZoekenService,
    public utilService: UtilService,
  ) {}

  ngOnInit(): void {
    this.zoekService.trefwoorden$.subscribe((trefwoorden) => {
      if (this.trefwoordenControl.value !== trefwoorden) {
        this.trefwoordenControl.setValue(trefwoorden);
      }
    });
    this.trefwoordenControl.valueChanges.subscribe((trefwoorden) => {
      if (!trefwoorden) {
        return;
      }

      this.zoekService.trefwoorden$.next(trefwoorden);
    });
    this.zoekenSideNav.openedStart.subscribe(() => {
      if (!this.trefwoordenControl.value) {
        return;
      }

      this.zoek.emit();
    });
    this.zoekService.reset$.subscribe(() => this.reset());
  }

  ngAfterViewInit(): void {
    this.zoek.subscribe(() => {
      this.paginator.pageIndex = 0;
    });
    merge(this.paginator.page, this.zoek)
      .pipe(
        switchMap(() => {
          this.slow = false;
          setTimeout(() => {
            this.slow = true;
          }, 500);
          this.isLoadingResults = true;
          this.utilService.setLoading(true);
          return this.zoekService.list(this.getZoekParameters());
        }),
        map((data) => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
          return data;
        }),
      )
      .subscribe((data) => {
        this.paginator.length = data.totaal;
        this.hasSearched = true;
        this.zoekService.hasSearched$.next(true);
        this.zoekResultaat = data;
        this.bepaalContext();
      });
  }

  bepaalContext(): void {
    this.hasZaken = !!this.zoekResultaat.filters.TYPE?.find(
      ({ naam }) => naam === ZoekObjectType.ZAAK,
    )?.aantal;
    this.hasTaken = !!this.zoekResultaat.filters.TYPE?.find(
      ({ naam }) => naam === ZoekObjectType.TAAK,
    )?.aantal;
    this.hasDocument = !!this.zoekResultaat.filters.TYPE?.find(
      ({ naam }) => naam === ZoekObjectType.DOCUMENT,
    )?.aantal;

    if (this.zoekParameters.filters.TYPE?.values?.length) {
      if (this.hasZaken) {
        this.hasZaken = this.zoekParameters.filters.TYPE.values.includes(
          ZoekObjectType.ZAAK,
        );
      }
      if (this.hasTaken) {
        this.hasTaken = this.zoekParameters.filters.TYPE.values.includes(
          ZoekObjectType.TAAK,
        );
      }
      if (this.hasDocument) {
        this.hasDocument =
          this.zoekParameters.filters.TYPE?.values.includes(
            ZoekObjectType.DOCUMENT,
          ) ?? false;
      }
    }
  }

  getZoekParameters(): ZoekParameters {
    if (this.zoekveldControl.value && this.trefwoordenControl.value) {
      this.zoekParameters.zoeken[this.zoekveldControl.value] =
        this.trefwoordenControl.value;
    }

    this.zoekParameters.page = this.paginator.pageIndex;
    this.zoekParameters.rows = this.paginator.pageSize;
    return this.zoekParameters;
  }

  getZaakZoekObject(zoekObject: ZoekObject): ZaakZoekObject {
    return zoekObject as ZaakZoekObject;
  }

  getTaakZoekObject(zoekObject: ZoekObject): TaakZoekObject {
    return zoekObject as TaakZoekObject;
  }

  getDocumentZoekObject(zoekObject: ZoekObject): DocumentZoekObject {
    return zoekObject as DocumentZoekObject;
  }

  hasOption(options: string[]) {
    return options.length
      ? !(options.length === 1 && options[0] === "-NULL-")
      : false;
  }

  keywordsChange() {
    if (
      this.zoekveldControl.value &&
      this.trefwoordenControl.value !==
        this.zoekParameters.zoeken[this.zoekveldControl.value]
    ) {
      this.zoek.emit();
    }
  }

  originalOrder = () => 0;

  setZoektype(zoekType: ZoekType): void {
    this.zoekType = zoekType;
    if (zoekType === ZoekType.ZAC) {
      this.trefwoordenControl.enable();
    } else {
      this.trefwoordenControl.disable();
    }
  }

  reset(): void {
    this.zoekService.hasSearched$.next(false);
    this.paginator.length = 0;
    this.trefwoordenControl.setValue("");
    this.zoekveldControl.setValue(ZoekVeld.ALLE);
    this.zoekResultaat = new ZoekResultaat();
    this.zoekParameters = new ZoekParameters();
    this.hasSearched = false;
    this.hasTaken = false;
    this.hasZaken = false;
    this.hasDocument = false;
  }

  zoekVeldChanged() {
    delete this.zoekParameters.zoeken[this.huidigZoekVeld];
    if (!this.zoekveldControl.value) {
      return;
    }

    this.huidigZoekVeld = this.zoekveldControl.value;
    if (this.trefwoordenControl.value) {
      this.zoek.emit();
    }
  }

  betrokkeneActief(): boolean {
    return !!(
      this.zoekParameters.zoeken.ZAAK_BETROKKENEN ||
      this.zoekParameters.zoeken.ZAAK_INITIATOR ||
      this.zoekParameters.zoeken.ZAAK_BETROKKENE_BELANGHEBBENDE ||
      this.zoekParameters.zoeken.ZAAK_BETROKKENE_ADVISEUR ||
      this.zoekParameters.zoeken.ZAAK_BETROKKENE_BESLISSER ||
      this.zoekParameters.zoeken.ZAAK_BETROKKENE_KLANTCONTACTER ||
      this.zoekParameters.zoeken.ZAAK_BETROKKENE_ZAAKCOORDINATOR ||
      this.zoekParameters.zoeken.ZAAK_BETROKKENE_MEDE_INITIATOR
    );
  }
}
