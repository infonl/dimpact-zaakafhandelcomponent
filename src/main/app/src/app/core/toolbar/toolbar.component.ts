/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import moment from "moment";
import { Observable, Subject, takeUntil } from "rxjs";
import { IdentityService } from "../../identity/identity.service";
import { User } from "../../identity/model/user";
import { OverigeRechten } from "../../policy/model/overige-rechten";
import { WerklijstRechten } from "../../policy/model/werklijst-rechten";
import { PolicyService } from "../../policy/policy.service";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { SignaleringenService } from "../../signaleringen.service";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { UtilService } from "../service/util.service";
import { ObjectType } from "../websocket/model/object-type";
import { Opcode } from "../websocket/model/opcode";
import { WebsocketListener } from "../websocket/model/websocket-listener";
import { WebsocketService } from "../websocket/websocket.service";

@Component({
  selector: "zac-toolbar",
  templateUrl: "./toolbar.component.html",
  styleUrls: ["./toolbar.component.less"],
})
export class ToolbarComponent implements OnInit, OnDestroy {
  @Input() zoekenSideNav: MatSidenav;
  zoekenFormControl = new FormControl<string>("");
  hasSearched = false;

  headerTitle$: Observable<string>;
  hasNewSignaleringen: boolean;
  ingelogdeMedewerker: User;
  overigeRechten = new OverigeRechten();
  werklijstRechten = new WerklijstRechten();
  medewerkerNaamToolbar = "";

  destroy$ = new Subject<void>();

  private signaleringListener: WebsocketListener;

  constructor(
    public utilService: UtilService,
    public navigation: NavigationService,
    private identityService: IdentityService,
    private zoekenService: ZoekenService,
    private signaleringenService: SignaleringenService,
    private websocketService: WebsocketService,
    private policyService: PolicyService,
  ) {}

  ngOnInit(): void {
    this.zoekenService.trefwoorden$
      .pipe(takeUntil(this.destroy$))
      .subscribe((trefwoorden) => {
        if (this.zoekenFormControl.value !== trefwoorden) {
          this.zoekenFormControl.setValue(trefwoorden);
        }
      });
    this.zoekenService.hasSearched$
      .pipe(takeUntil(this.destroy$))
      .subscribe((hasSearched) => {
        this.hasSearched = hasSearched;
      });
    this.zoekenFormControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((trefwoorden) => {
        this.zoekenService.trefwoorden$.next(trefwoorden);
      });
    this.headerTitle$ = this.utilService.headerTitle$;
    this.identityService
      .readLoggedInUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe((medewerker) => {
        this.ingelogdeMedewerker = medewerker;
        this.medewerkerNaamToolbar = medewerker.naam
          .split(" ")
          .map((n) => n[0])
          .join("");

        this.signaleringListener = this.websocketService.addListener(
          Opcode.UPDATED,
          ObjectType.SIGNALERINGEN,
          medewerker.id,
          () => this.signaleringenService.updateSignaleringen(),
        );
      });
    this.policyService
      .readOverigeRechten()
      .pipe(takeUntil(this.destroy$))
      .subscribe((rechten) => (this.overigeRechten = rechten));
    this.policyService
      .readWerklijstRechten()
      .pipe(takeUntil(this.destroy$))
      .subscribe((rechten) => (this.werklijstRechten = rechten));
    this.setSignaleringen();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.websocketService.removeListener(this.signaleringListener);
  }

  setSignaleringen(): void {
    this.signaleringenService.latestSignalering$
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        // TODO instead of session storage use userpreferences in a db
        const dashboardLastOpenendStorage: string =
          SessionStorageUtil.getItem("dashboardOpened");
        if (!dashboardLastOpenendStorage) {
          this.hasNewSignaleringen = !!value;
        } else {
          const dashboardLastOpenendMoment: moment.Moment = moment(
            dashboardLastOpenendStorage,
            moment.ISO_8601,
          );

          const newestSignalering: moment.Moment = moment(
            value,
            moment.ISO_8601,
          );
          this.hasNewSignaleringen = newestSignalering.isAfter(
            dashboardLastOpenendMoment,
          );
        }
      });
  }

  resetSearch(): void {
    this.zoekenService.reset$.next();
  }
}
