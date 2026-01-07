/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  effect,
  inject,
  Input,
  OnDestroy,
  OnInit,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormControl } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { Router } from "@angular/router";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { Observable, Subscription } from "rxjs";
import { IdentityService } from "../../identity/identity.service";
import { PolicyService } from "../../policy/policy.service";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenService } from "../../signaleringen.service";
import { ZakenService } from "../../zaken/zaken.service";
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
    standalone: false
})
export class ToolbarComponent implements OnInit, OnDestroy {
  protected readonly queryClient = inject(QueryClient);
  protected readonly createZaakMutationKey =
    this.zakenService.createZaak().mutationKey;

  @Input({ required: true }) zoekenSideNav!: MatSidenav;
  zoekenFormControl = new FormControl<string>("");

  headerTitle$?: Observable<string>;
  hasNewSignaleringen = false;
  overigeRechten?: GeneratedType<"RestOverigeRechten">;
  werklijstRechten?: GeneratedType<"RestWerklijstRechten">;

  private subscription$?: Subscription;
  private signaleringListener?: WebsocketListener;

  protected readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );
  protected readonly medewerkerNaamToolbar = computed(() =>
    this.loggedInUserQuery
      .data()
      ?.naam?.split(" ")
      .filter(Boolean)
      .map(([firstLetter]) => firstLetter)
      .join(""),
  );

  constructor(
    public utilService: UtilService,
    public navigation: NavigationService,
    private identityService: IdentityService,
    protected readonly zoekenService: ZoekenService,
    private signaleringenService: SignaleringenService,
    private websocketService: WebsocketService,
    private policyService: PolicyService,
    private router: Router,
    private readonly zakenService: ZakenService,
  ) {
    effect(() => {
      const loggedInUser = this.loggedInUserQuery.data();
      if (!loggedInUser) return;

      this.signaleringListener = this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.SIGNALERINGEN,
        loggedInUser.id,
        () => this.signaleringenService.updateSignaleringen(),
      );
    });

    effect(() => {
      const trefwoorden = this.zoekenService.trefwoorden();
      if (this.zoekenFormControl.value === trefwoorden) return;
      this.zoekenFormControl.setValue(trefwoorden);
    });

    this.zoekenFormControl.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((trefwoorden) => {
        this.zoekenService.trefwoorden.set(trefwoorden);
      });

    this.zoekenService.reset$.pipe(takeUntilDestroyed()).subscribe(() => {
      this.zoekenFormControl.setValue(null);
    });
  }

  ngOnInit() {
    this.headerTitle$ = this.utilService.headerTitle$;

    this.policyService
      .readOverigeRechten()
      .subscribe((rechten) => (this.overigeRechten = rechten));
    this.policyService
      .readWerklijstRechten()
      .subscribe((rechten) => (this.werklijstRechten = rechten));
    this.setSignaleringen();
  }

  ngOnDestroy() {
    this.subscription$?.unsubscribe();

    if (this.signaleringListener) {
      this.websocketService.removeListener(this.signaleringListener);
    }
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
