/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class FormCommunicatieService {
  private itemSelectedSource = new BehaviorSubject<{
    selected: boolean;
    uuid: string | null;
  }>({ selected: false, uuid: null });
  itemSelected$ = this.itemSelectedSource.asObservable();

  notifyItemSelected(uuid: string) {
    this.itemSelectedSource.next({ selected: true, uuid });
  }
}
