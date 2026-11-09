export interface CropGeometry {
  cropLeft: number;
  cropTop: number;
  cropSize: number;
  sourceLeft: number;
  sourceTop: number;
  sourceSize: number;
  offsetX: number;
  offsetY: number;
  renderedWidth: number;
  renderedHeight: number;
  minOffsetX: number;
  maxOffsetX: number;
  minOffsetY: number;
  maxOffsetY: number;
  scale: number;
}

export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

export function getCropGeometry(
  sourceWidth: number,
  sourceHeight: number,
  frameWidth: number,
  frameHeight: number,
  zoom: number,
  panX: number,
  panY: number,
): CropGeometry {
  if (
    ![sourceWidth, sourceHeight, frameWidth, frameHeight, zoom, panX, panY].every(
      Number.isFinite,
    ) ||
    sourceWidth <= 0 ||
    sourceHeight <= 0 ||
    frameWidth <= 0 ||
    frameHeight <= 0 ||
    zoom < 100 ||
    zoom > 200
  ) {
    throw new RangeError("Crop dimensions, zoom, and position must be valid.");
  }

  const cropSize = Math.min(frameWidth, frameHeight);
  const scale = Math.max(cropSize / sourceWidth, cropSize / sourceHeight) * (zoom / 100);
  const renderedWidth = sourceWidth * scale;
  const renderedHeight = sourceHeight * scale;
  const cropLeft = (frameWidth - cropSize) / 2;
  const cropTop = (frameHeight - cropSize) / 2;
  const minOffsetX = cropLeft + cropSize - renderedWidth;
  const maxOffsetX = cropLeft;
  const minOffsetY = cropTop + cropSize - renderedHeight;
  const maxOffsetY = cropTop;
  const offsetX = minOffsetX + clamp(panX, 0, 1) * (maxOffsetX - minOffsetX);
  const offsetY = minOffsetY + clamp(panY, 0, 1) * (maxOffsetY - minOffsetY);

  return {
    cropLeft,
    cropTop,
    cropSize,
    sourceLeft: clamp((cropLeft - offsetX) / scale, 0, sourceWidth - cropSize / scale),
    sourceTop: clamp((cropTop - offsetY) / scale, 0, sourceHeight - cropSize / scale),
    sourceSize: cropSize / scale,
    offsetX,
    offsetY,
    renderedWidth,
    renderedHeight,
    minOffsetX,
    maxOffsetX,
    minOffsetY,
    maxOffsetY,
    scale,
  };
}

export function validateImageFile(file: File): string | null {
  if (file.type !== "image/png" && file.type !== "image/jpeg") {
    return "Choose a PNG or JPEG image.";
  }
  if (file.size > 10 * 1024 * 1024) {
    return "Choose an image smaller than 10 MB.";
  }
  return null;
}
