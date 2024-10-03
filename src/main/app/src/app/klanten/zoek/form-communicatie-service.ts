import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class FormCommunicatieService {
  private formSubmittedSource = new BehaviorSubject<{
    submitted: boolean;
    formId: string | null;
  }>({ submitted: false, formId: null });
  formSubmitted$ = this.formSubmittedSource.asObservable();

  private formClearedSource = new BehaviorSubject<{
    cleared: boolean;
    formId: string | null;
  }>({ cleared: false, formId: null });
  formCleared$ = this.formClearedSource.asObservable();

  notifySelected(formId: string) {
    this.formSubmittedSource.next({ submitted: true, formId });
  }
}
