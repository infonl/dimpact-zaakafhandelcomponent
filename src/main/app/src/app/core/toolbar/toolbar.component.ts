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
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
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

  protected readonly queryClient = inject(QueryClient);
  protected readonly createZaakMutationKey =
    this.zakenService.createZaak().mutationKey;

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

  constructor() {
    effect(() => {
      const loggedInUser = this.loggedInUserQuery.data();
      if (!loggedInUser) return;

      this.signaleringListener = this.websocketService.addListener(
        Opcode.UPDATED,
        ObjectType.SIGNALERINGEN,
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
