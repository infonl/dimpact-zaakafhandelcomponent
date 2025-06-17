/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  ViewChild,
} from "@angular/core";
import { MatPaginator } from "@angular/material/paginator";
import { MatTableDataSource } from "@angular/material/table";
import { map, startWith, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { PutBody } from "../../shared/http/zac-http-client";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ContactmomentenService } from "../contactmomenten.service";

@Component({
  selector: "zac-klant-contactmomenten-tabel",
  templateUrl: "./klant-contactmomenten-tabel.component.html",
  styleUrls: ["./klant-contactmomenten-tabel.component.less"],
})
export class KlantContactmomentenTabelComponent
  implements OnInit, AfterViewInit, OnChanges
{
  @Input() bsn?: string;
  @Input() vestigingsnummer?: string;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  dataSource = new MatTableDataSource<GeneratedType<"RestContactmoment">>();
  columns: string[] = [
    "registratiedatum",
    "kanaal",
    "initiatiefnemer",
    "medewerker",
    "tekst",
  ];
  listParameters: PutBody<"/rest/klanten/contactmomenten"> = {};
  init = false;
  isLoadingResults = true;

  constructor(
    private readonly contactmomentenService: ContactmomentenService,
    private readonly utilService: UtilService,
  ) {}

  ngOnInit(): void {
    this.listParameters.bsn = this.bsn;
    this.listParameters.vestigingsnummer = this.vestigingsnummer;
  }

  ngAfterViewInit(): void {
    this.init = true;
    this.paginator.page
      .pipe(
        startWith({}),
        switchMap(() => {
          this.isLoadingResults = true;
          this.utilService.setLoading(true);
          return this.loadContactmomenten();
        }),
        map((resultaat) => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
          return resultaat;
        }),
      )
      .subscribe((resultaat) => {
        this.paginator.length = resultaat.totaal ?? 0;
        this.dataSource.data = resultaat.resultaten ?? [];
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.bsn = changes.bsn?.currentValue;
    this.vestigingsnummer = changes.vestigingsnummer?.currentValue;
    if (this.init) {
      this.paginator.pageIndex = 0;
      this.paginator.page.emit();
    }
  }

  private loadContactmomenten() {
    this.listParameters.page = this.paginator.pageIndex;
    return this.contactmomentenService.listContactmomenten(this.listParameters);
  }
}
