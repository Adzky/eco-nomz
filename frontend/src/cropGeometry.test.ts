import { describe, expect, it } from "vitest";
import { getCropGeometry, validateImageFile } from "./cropGeometry";

describe("getCropGeometry", () => {
  it("starts with a centered square crop for a landscape image", () => {
    const crop = getCropGeometry(1200, 750, 610, 421, 100, 0.5, 0.5);

    expect(crop.sourceSize).toBeCloseTo(750);
    expect(crop.sourceLeft).toBeCloseTo(225);
    expect(crop.sourceTop).toBeCloseTo(0);
    expect(crop.sourceTop + crop.sourceSize).toBeLessThanOrEqual(750);
    expect(crop.offsetX + crop.renderedWidth).toBeGreaterThanOrEqual(
      crop.cropLeft + crop.cropSize,
    );
    expect(crop.offsetY + crop.renderedHeight).toBeGreaterThanOrEqual(
      crop.cropTop + crop.cropSize,
    );
  });

  it("keeps a panned portrait crop inside the image bounds", () => {
    const crop = getCropGeometry(800, 1200, 610, 421, 150, 0, 1);

    expect(crop.sourceLeft).toBeGreaterThanOrEqual(0);
    expect(crop.sourceTop).toBeGreaterThanOrEqual(0);
    expect(crop.sourceLeft + crop.sourceSize).toBeLessThanOrEqual(800);
    expect(crop.sourceTop + crop.sourceSize).toBeLessThanOrEqual(1200);
  });

  it("keeps landscape crop boundaries inside the source at both pan extremes", () => {
    for (const panX of [0, 1]) {
      const crop = getCropGeometry(1200, 750, 610, 421, 125, panX, 0.5);

      expect(crop.sourceLeft).toBeGreaterThanOrEqual(0);
      expect(crop.sourceLeft + crop.sourceSize).toBeLessThanOrEqual(1200);
      expect(crop.sourceTop).toBeGreaterThanOrEqual(0);
      expect(crop.sourceTop + crop.sourceSize).toBeLessThanOrEqual(750);
    }
  });

  it("rejects invalid dimensions and out-of-range zoom", () => {
    expect(() => getCropGeometry(0, 750, 610, 421, 100, 0.5, 0.5)).toThrow(
      RangeError,
    );
    expect(() => getCropGeometry(1200, 750, 610, 421, 201, 0.5, 0.5)).toThrow(
      RangeError,
    );
  });
});

describe("validateImageFile", () => {
  it("accepts PNG and JPEG files at the 10 MB limit", () => {
    const exactlyLimit = new File([new Uint8Array(10 * 1024 * 1024)], "logo.png", {
      type: "image/png",
    });
    expect(validateImageFile(new File(["x"], "logo.png", { type: "image/png" }))).toBeNull();
    expect(validateImageFile(new File(["x"], "logo.jpg", { type: "image/jpeg" }))).toBeNull();
    expect(validateImageFile(exactlyLimit)).toBeNull();
  });

  it("rejects unsupported formats and oversized files", () => {
    expect(validateImageFile(new File(["x"], "logo.svg", { type: "image/svg+xml" }))).toMatch(
      /PNG or JPEG/,
    );
    const oversized = new File([new Uint8Array(10 * 1024 * 1024 + 1)], "logo.png", {
      type: "image/png",
    });
    expect(validateImageFile(oversized)).toMatch(/10 MB/);
  });
});
