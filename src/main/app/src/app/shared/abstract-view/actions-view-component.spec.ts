/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, viewChild } from "@angular/core";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { render, screen, waitFor } from "@testing-library/angular";
import { ActionsViewComponent } from "./actions-view-component";

@Component({
  template: `
    <mat-sidenav-container #sideNavContainer>
      <mat-sidenav
        #menuSidenav
        position="start"
        opened="true"
        disableClose="true"
        mode="side"
      >
        menu
      </mat-sidenav>
      <mat-sidenav #actionsSidenav position="end" mode="over">
        actions
      </mat-sidenav>
      <mat-sidenav-content role="region" aria-label="inhoud">
        inhoud
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  imports: [MatSidenavModule],
})
class TestActionsViewComponent extends ActionsViewComponent {
  readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");
  readonly menuSidenav = viewChild.required<MatSidenav>("menuSidenav");
  readonly actionsSidenav = viewChild.required<MatSidenav>("actionsSidenav");
}

describe(ActionsViewComponent.name, () => {
  async function setup() {
    const { fixture } = await render(TestActionsViewComponent);
    return fixture.componentInstance.actionsSidenav();
  }

  it("makes the content unusable while the actions panel is open", async () => {
    const actionsSidenav = await setup();

    await actionsSidenav.open();

    await waitFor(() =>
      expect(screen.getByRole("region", { name: "inhoud" })).toHaveAttribute(
        "inert",
      ),
    );
  });

  it("makes the content usable again once the actions panel is closed", async () => {
    const actionsSidenav = await setup();
    await actionsSidenav.open();
    await waitFor(() =>
      expect(screen.getByRole("region", { name: "inhoud" })).toHaveAttribute(
        "inert",
      ),
    );

    await actionsSidenav.close();

    await waitFor(() =>
      expect(
        screen.getByRole("region", { name: "inhoud" }),
      ).not.toHaveAttribute("inert"),
    );
  });
});
