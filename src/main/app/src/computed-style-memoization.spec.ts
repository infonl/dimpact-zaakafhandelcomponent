/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

describe("memoized getComputedStyle", () => {
  it("sees a style attribute change", () => {
    const element = document.createElement("div");
    document.body.appendChild(element);
    expect(getComputedStyle(element).visibility).toBe("visible");
    element.style.visibility = "hidden";
    expect(getComputedStyle(element).visibility).toBe("hidden");
    element.style.display = "none";
    expect(getComputedStyle(element).display).toBe("none");
    element.remove();
  });

  it("sees a class change that an inserted stylesheet targets", () => {
    const style = document.createElement("style");
    style.textContent = ".gone { display: none; visibility: hidden; }";
    document.head.appendChild(style);
    const element = document.createElement("div");
    document.body.appendChild(element);
    expect(getComputedStyle(element).display).toBe("block");
    element.classList.add("gone");
    expect(getComputedStyle(element).display).toBe("none");
    expect(getComputedStyle(element).visibility).toBe("hidden");
    element.classList.remove("gone");
    expect(getComputedStyle(element).display).toBe("block");
    element.remove();
    style.remove();
  });

  it("sees a parent class change through inheritance", () => {
    const style = document.createElement("style");
    style.textContent = ".hidden-parent { visibility: hidden; }";
    document.head.appendChild(style);
    const parent = document.createElement("div");
    const child = document.createElement("span");
    parent.appendChild(child);
    document.body.appendChild(parent);
    expect(getComputedStyle(child).visibility).toBe("visible");
    parent.classList.add("hidden-parent");
    expect(getComputedStyle(child).visibility).toBe("hidden");
    parent.remove();
    style.remove();
  });

  it("sees a node moved under a hidden parent", () => {
    const style = document.createElement("style");
    style.textContent = ".hidden-parent { visibility: hidden; }";
    document.head.appendChild(style);
    const parent = document.createElement("div");
    parent.className = "hidden-parent";
    const child = document.createElement("span");
    document.body.append(parent, child);
    expect(getComputedStyle(child).visibility).toBe("visible");
    parent.appendChild(child);
    expect(getComputedStyle(child).visibility).toBe("hidden");
    parent.remove();
    style.remove();
  });
});
