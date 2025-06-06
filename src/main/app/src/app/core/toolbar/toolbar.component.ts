/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { Router } from "@angular/router";
import moment from "moment";
import { Observable, Subscription } from "rxjs";
import { IdentityService } from "../../identity/identity.service";
import { PolicyService } from "../../policy/policy.service";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
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
  ingelogdeMedewerker?: GeneratedType<"RestUser">;
  overigeRechten?: GeneratedType<"RestOverigeRechten">;
  werklijstRechten?: GeneratedType<"RestWerklijstRechten">;
  medewerkerNaamToolbar = "";

  private subscription$: Subscription;
  private signaleringListener: WebsocketListener;

  constructor(
    public utilService: UtilService,
    public navigation: NavigationService,
    private identityService: IdentityService,
    private zoekenService: ZoekenService,
    private signaleringenService: SignaleringenService,
    private websocketService: WebsocketService,
    private policyService: PolicyService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.zoekenService.trefwoorden$.subscribe((trefwoorden) => {
      if (this.zoekenFormControl.value !== trefwoorden) {
        this.zoekenFormControl.setValue(trefwoorden);
      }
    });
    this.zoekenService.hasSearched$.subscribe((hasSearched) => {
      this.hasSearched = hasSearched;
    });
    this.zoekenFormControl.valueChanges.subscribe((trefwoorden) => {
      this.zoekenService.trefwoorden$.next(trefwoorden);
    });
    this.headerTitle$ = this.utilService.headerTitle$;
    this.identityService.readLoggedInUser().subscribe((medewerker) => {
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
      .subscribe((rechten) => (this.overigeRechten = rechten));
    this.policyService
      .readWerklijstRechten()
      .subscribe((rechten) => (this.werklijstRechten = rechten));
    this.setSignaleringen();
  }

  ngOnDestroy(): void {
    this.subscription$.unsubscribe();
    this.websocketService.removeListener(this.signaleringListener);
  }

  setSignaleringen(): void {
    this.subscription$ = this.signaleringenService.latestSignalering$.subscribe(
      (value) => {
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
      },
    );
  }

  resetSearch(): void {
    this.zoekenService.reset$.next();
  }

  isCaseRouteActive() {
    return (
      this.router.url.startsWith("/zaken/") &&
      !this.router.url.startsWith("/zaken/create")
    );
  }

  isTaskRouteActive() {
    return this.router.url.startsWith("/taken/");
  }
}
