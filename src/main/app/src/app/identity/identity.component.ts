/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, inject } from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatListModule } from "@angular/material/list";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { IdentityService } from "./identity.service";

@Component({
  templateUrl: "./identity.component.html",
  standalone: true,
  imports: [MatCardModule, MatListModule],
})
export class IdentityComponent {
  private readonly identityService = inject(IdentityService);

  protected readonly loggedInUserQuery = injectQuery(() =>
    this.identityService.readLoggedInUser(),
  );

  protected readonly groups = computed(
    () => this.loggedInUserQuery.data()?.groupIds || [],
  );
  protected readonly roles = computed(
    () => this.loggedInUserQuery.data()?.functionalRoles || [],
  );
  protected readonly rolesPerZaakType = computed(() => {
    return Object.entries(
      this.loggedInUserQuery.data()?.applicationRoles || {},
    ).map(([zaakType, roles]) => ({
      zaakType,
      roles: Array.from(Object.values(roles)),
    }));
  });
}
