import { AfterViewInit, Directive, ElementRef } from "@angular/core";

@Directive({
  selector: "[appEnhanceMatError]",
})
export class EnhanceMatErrorDirective implements AfterViewInit {
  constructor(private elRef: ElementRef) {}

  ngAfterViewInit() {
    const css = `
        text-overflow: ellipsis;
        overflow: hidden;
        white-space: nowrap;
      `;

    const matErrorElement = this.elRef.nativeElement;

    matErrorElement.title = matErrorElement.innerHTML;
    matErrorElement.setAttribute("style", css);
  }
}
