/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { Component, computed, input } from "@angular/core";
import { MatIcon } from "@angular/material/icon";
import { TranslatePipe } from "@ngx-translate/core";
import { isLoggedOut } from "../../fout-afhandeling/fout-afhandeling.service";

/** The part of an `injectQuery` result that tells whether a refetch failed. */
export type RefetchableQuery = {
  isRefetchError: () => boolean;
  error: () => unknown;
};

function hasFailedToRefresh(query: RefetchableQuery) {
  if (!query.isRefetchError()) return false;

  // The error handler reloads the page to send the user to the login page.
  const error = query.error();
  return !(error instanceof HttpErrorResponse && isLoggedOut(error));
}

@Component({
  selector: "zac-refetch-error-hint",
  templateUrl: "./refetch-error-hint.component.html",
  styleUrl: "./refetch-error-hint.component.less",
  host: { role: "status" },
  imports: [MatIcon, TranslatePipe],
})
export class RefetchErrorHintComponent {
  readonly query = input.required<
    RefetchableQuery | readonly RefetchableQuery[]
  >();

  protected readonly visible = computed(() =>
    [this.query()].flat().some(hasFailedToRefresh),
  );
}
