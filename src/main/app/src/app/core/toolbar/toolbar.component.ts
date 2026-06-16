/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgClass, NgIf } from "@angular/common";
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
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatBadge } from "@angular/material/badge";
import {
  MatButton,
  MatIconButton,
  MatMiniFabButton,
} from "@angular/material/button";
import { MatDivider } from "@angular/material/divider";
import {
  MatFormField,
  MatLabel,
  MatSuffix,
} from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatInput } from "@angular/material/input";
import { MatMenu, MatMenuItem, MatMenuTrigger } from "@angular/material/menu";
import { MatSidenav } from "@angular/material/sidenav";
import { MatToolbar } from "@angular/material/toolbar";
import { MatTooltip } from "@angular/material/tooltip";
import { Router, RouterLink, RouterLinkActive } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { Observable, Subscription } from "rxjs";
import { IdentityService } from "../../identity/identity.service";
import { PolicyService } from "../../policy/policy.service";
import { BackButtonDirective } from "../../shared/navigation/back-button.directive";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { VersionComponent } from "../../shared/version/version.component";
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
  standalone: true,
  imports: [
    NgIf,
    NgClass,
    AsyncPipe,
    ReactiveFormsModule,
    RouterLink,
    RouterLinkActive,
    MatToolbar,
    MatMiniFabButton,
    MatButton,
    MatIconButton,
    MatBadge,
    MatIcon,
    MatMenu,
    MatMenuItem,
    MatMenuTrigger,
    MatFormField,
    MatLabel,
    MatSuffix,
    MatInput,
    MatTooltip,
    MatDivider,
    TranslateModule,
    BackButtonDirective,
    VersionComponent,
  ],
})
export class ToolbarComponent implements OnInit, OnDestroy {
  protected readonly queryClient = inject(QueryClient);
  protected readonly createZaakMutationKey =
    this.zakenService.createZaak().mutationKey;

  @Input({ required: true }) zoekenSideNav!: MatSidenav;
  protected zoekenFormControl = new FormControl<string>("");

  protected headerTitle$?: Observable<string>;
  protected hasNewSignaleringen = false;
  protected overigeRechten?: GeneratedType<"RestOverigeRechten">;
  protected werklijstRechten?: GeneratedType<"RestWerklijstRechten">;

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
    private readonly utilService: UtilService,
    protected readonly navigation: NavigationService,
    private readonly identityService: IdentityService,
    protected readonly zoekenService: ZoekenService,
    private readonly signaleringenService: SignaleringenService,
    private readonly websocketService: WebsocketService,
    private readonly policyService: PolicyService,
    private readonly router: Router,
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

  protected setSignaleringen() {
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

  protected resetSearch() {
    this.zoekenService.reset$.next();
  }

  protected isCaseRouteActive() {
    return (
      this.router.url.startsWith("/zaken/") &&
      !this.router.url.startsWith("/zaken/create")
    );
  }

  protected isTaskRouteActive() {
    return this.router.url.startsWith("/taken/");
  }
}
