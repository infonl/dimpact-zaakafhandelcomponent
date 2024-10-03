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

  notifySelected(uuid: string) {
    this.itemSelectedSource.next({ selected: true, uuid });
  }
}
