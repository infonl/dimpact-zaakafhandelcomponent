import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class FormCommunicatieService {
  private formSelectedSource = new BehaviorSubject<{
    seelcted: boolean;
    formId: string | null;
  }>({ seelcted: false, formId: null });
  formSubmitted$ = this.formSelectedSource.asObservable();

  notifySelected(formId: string) {
    this.formSelectedSource.next({ seelcted: true, formId });
  }
}
