/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass } from "@angular/common";
import {
  Component,
  computed,
  effect,
  inject,
  input,
  OnDestroy,
} from "@angular/core";
import { takeUntilDestroyed, toSignal } from "@angular/core/rxjs-interop";
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
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
} from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import {
  injectIsMutating,
  injectQuery,
} from "@tanstack/angular-query-experimental";
import moment from "moment";
import { filter, map } from "rxjs";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";
import { IdentityService } from "../../identity/identity.service";
import { PolicyService } from "../../policy/policy.service";
import {
  BackButtonDirective,
  injectCanGoBack,
} from "../../shared/navigation/back-button.directive";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { VersionComponent } from "../../shared/version/version.component";
import { SignaleringenService } from "../../signaleringen.service";
import { ZakenService } from "../../zaken/zaken.service";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { WebsocketListener } from "../websocket/model/websocket-listener";
import { WebsocketService } from "../websocket/websocket.service";

@Component({
  selector: "zac-toolbar",
  templateUrl: "./toolbar.component.html",
  styleUrls: ["./toolbar.component.less"],
  standalone: true,
  imports: [
    NgClass,
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
    TranslatePipe,
    BackButtonDirective,
    VersionComponent,
  ],
})
export class ToolbarComponent implements OnDestroy {
  private readonly identityService = inject(IdentityService);
  protected readonly zoekenService = inject(ZoekenService);
  private readonly signaleringenService = inject(SignaleringenService);
  private readonly websocketService = inject(WebsocketService);
  private readonly policyService = inject(PolicyService);
  private readonly router = inject(Router);
  private readonly zakenService = inject(ZakenService);
  private readonly foutAfhandelingService = inject(FoutAfhandelingService);

  protected readonly creatingZaakCount = injectIsMutating({
    mutationKey: this.zakenService.createZaak().mutationKey,
  });

  readonly zoekenSideNav = input.required<MatSidenav>();
  protected zoekenFormControl = new FormControl<string>("");

  private signaleringListener?: WebsocketListener;

  protected readonly canGoBack = injectCanGoBack();
  protected readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );
  protected readonly overigeRechtenQuery = injectQuery(() =>
    this.policyService.readOverigeRechten(),
  );
  protected readonly werklijstRechtenQuery = injectQuery(() =>
    this.policyService.readWerklijstRechten(),
  );
  private readonly latestSignaleringQuery = injectQuery(() =>
    this.signaleringenService.readLatestSignalering(),
  );
  protected readonly hasNewSignaleringen = computed(() => {
    this.latestSignaleringQuery.dataUpdatedAt();
    const latestSignalering = this.latestSignaleringQuery.data();
    if (!latestSignalering) return false;

    // TODO instead of session storage use userpreferences in a db
    const dashboardLastOpened: string | null =
      SessionStorageUtil.getItem("dashboardOpened");
    if (!dashboardLastOpened) return true;

    return moment(latestSignalering, moment.ISO_8601).isAfter(
      moment(dashboardLastOpened, moment.ISO_8601),
    );
  });
  protected readonly medewerkerNaamToolbar = computed(() =>
    this.loggedInUserQuery
      .data()
      ?.naam?.split(" ")
      .filter(Boolean)
      .map(([firstLetter]) => firstLetter)
      .join(""),
  );

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url),
    ),
    { initialValue: this.router.url },
  );
  protected readonly isCaseRouteActive = computed(
    () =>
      this.url().startsWith("/zaken/") &&
      !this.url().startsWith("/zaken/create"),
  );
  protected readonly isTaskRouteActive = computed(() =>
    this.url().startsWith("/taken/"),
  );

  constructor() {
    effect(() => {
      const loggedInUser = this.loggedInUserQuery.data();
      if (!loggedInUser) return;

      this.signaleringListener = this.websocketService.addListener(
        "UPDATED",
        "SIGNALERINGEN",
        loggedInUser.username,
        () => void this.signaleringenService.invalidateLatestSignalering(),
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

  ngOnDestroy() {
    if (this.signaleringListener) {
      this.websocketService.removeListener(this.signaleringListener);
    }
  }

  protected resetSearch() {
    this.zoekenService.reset$.next();
  }

  protected logOut() {
    this.foutAfhandelingService.ignoreLoggedOutErrors();
  }
}
