import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class FormCommunicatieService {
  private formSelectedSource = new BehaviorSubject<{
    selected: boolean;
    uuid: string | null;
  }>({ selected: false, uuid: null });
  formSubmitted$ = this.formSelectedSource.asObservable();

  notifySelected(uuid: string) {
    this.formSelectedSource.next({ selected: true, uuid });
  }
}
