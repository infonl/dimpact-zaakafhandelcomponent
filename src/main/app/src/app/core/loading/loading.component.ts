/*
  ~ SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
  ~ SPDX-License-Identifier: EUPL-1.2+
  */

import { Component, computed, inject } from "@angular/core";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { Router } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import {
  QueryClient,
  injectIsFetching,
  injectIsMutating,
} from "@tanstack/angular-query-experimental";
import { UtilService } from "../service/util.service";

@Component({
  selector: "zac-loading",
  templateUrl: "./loading.component.html",
  standalone: true,
  imports: [MatProgressBarModule, TranslatePipe],
})
export class LoadingComponent {
  protected readonly utilService = inject(UtilService);
  protected readonly queryClient = inject(QueryClient);
  private readonly router = inject(Router);

  protected readonly progress = this.utilService.progress;

  protected readonly mutatingCount = injectIsMutating();
  protected readonly fetchingCount = injectIsFetching();

  protected readonly isMutating = computed(() => this.mutatingCount() > 0);
  protected readonly isFetching = computed(() => this.fetchingCount() > 0);
  protected readonly isLoading = computed(
    () =>
      this.utilService.loading() ||
      this.isFetching() ||
      this.router.currentNavigation() !== null,
  );
}
