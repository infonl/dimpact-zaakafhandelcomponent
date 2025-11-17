import { Component, computed, inject } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { IdentityService } from "./identity.service";

@Component({
  templateUrl: "./identity.component.html",
})
export class IdentityComponent {
  private readonly identityService = inject(IdentityService);

  protected readonly userAutorizationQuery = injectQuery(() =>
    this.identityService.readLoggedInUserAuthorization(),
  );

  protected readonly groups = computed(
    () => this.userAutorizationQuery.data()?.groupIds || [],
  );
  protected readonly roles = computed(
    () => this.userAutorizationQuery.data()?.functionalRoles || [],
  );
  protected readonly rolesPerZaakType = computed(() => {
    return Object.entries(
      this.userAutorizationQuery.data()?.applicationRoles || {},
    ).map(([zaakType, roles]) => ({
      zaakType,
      roles: Array.from(Object.values(roles)),
    }));
  });
}
