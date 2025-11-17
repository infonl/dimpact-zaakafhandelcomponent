import { Component, computed, inject } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { IdentityService } from "./identity.service";

@Component({
  templateUrl: "./identity.component.html",
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
