import { Directive, ElementRef, Renderer2, OnInit } from "@angular/core";

@Directive({
  selector: "[zacNarrowMatCheckbox]",
})
export class ZacNarrowMatCheckboxDirective implements OnInit {
  constructor(
    private el: ElementRef,
    private renderer: Renderer2,
  ) {}

  ngOnInit(): void {
    this.renderer.addClass(this.el.nativeElement, "mat-narrow-checkbox");
  }
}
