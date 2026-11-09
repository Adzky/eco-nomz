import { useCallback, useEffect, useRef, useState } from "react";
import type { ChangeEvent, KeyboardEvent, PointerEvent } from "react";
import brandLogoUrl from "./assets/brand-logo.png";
import sampleLogoUrl from "../../assets/plates/crop-source-photo.png?url";
import { getCropGeometry, validateImageFile } from "./cropGeometry";
import type { CropGeometry } from "./cropGeometry";

type Language = "en" | "id";
type Copy = {
  overview: string;
  restaurants: string;
  offers: string;
  reservations: string;
  back: string;
  details: string;
  logo: string;
  pickup: string;
  publish: string;
  title: string;
  instruction: string;
  preview: string;
  squareCrop: string;
  imageRequirements: string;
  formats: string;
  sizeLimit: string;
  requiredLogo: string;
  selectImage: string;
  chooseFile: string;
  rotateLeft: string;
  rotateRight: string;
  reset: string;
  zoom: string;
  apply: string;
  continueSquare: string;
  choosePrompt: string;
  alreadySquare: string;
  outputReady: string;
  download: string;
  uploadDescription: string;
  saveLogo: string;
  savingLogo: string;
  logoSaved: string;
  uploadFailure: string;
  uploadRejected: string;
  uploadNotConfigured: string;
  backToRestaurant: string;
  saveDetailsFirst: string;
  dragHelp: string;
  invalidFile: string;
  croppedTooLarge: string;
  cropFailure: string;
  ownerNavigation: string;
  languageSelection: string;
  account: string;
};

const COPY: Record<Language, Copy> = {
  en: {
    overview: "Overview",
    restaurants: "Restaurants",
    offers: "Offers",
    reservations: "Reservations",
    back: "Restaurant setup",
    details: "Details",
    logo: "Logo",
    pickup: "Pickup",
    publish: "Publish",
    title: "Position your logo",
    instruction: "Drag the image to choose what customers see in the square.",
    preview: "Logo preview",
    squareCrop: "Square crop",
    imageRequirements: "Image requirements",
    formats: "PNG or JPEG",
    sizeLimit: "Maximum 10 MB",
    requiredLogo: "Your logo is required before this restaurant can be published.",
    selectImage: "Choose an image",
    chooseFile: "Select a different file",
    rotateLeft: "Rotate left",
    rotateRight: "Rotate right",
    reset: "Reset",
    zoom: "Zoom",
    apply: "Apply crop and continue",
    continueSquare: "Continue with square image",
    choosePrompt: "Choose a PNG or JPEG image up to 10 MB to preview your logo.",
    alreadySquare: "This image is already square, so cropping is skipped.",
    outputReady: "Square image prepared. Save it to your restaurant profile or download a copy.",
    download: "Download square logo",
    saveLogo: "Save logo to restaurant",
    savingLogo: "Saving logo…",
    logoSaved: "Logo saved to this restaurant profile.",
    uploadFailure: "The logo could not be saved. Check your connection and try again.",
    uploadRejected: "Choose a valid PNG or JPEG square image under 10 MB, then try again.",
    uploadNotConfigured: "Cloudinary is not configured yet. Ask the site administrator to finish image storage setup.",
    backToRestaurant: "Back to restaurant",
    saveDetailsFirst: "Save the restaurant details before uploading its logo.",
    uploadDescription: "Prepare a square logo, then save it to this restaurant profile.",
    dragHelp: "Drag to reposition the image, or focus the canvas and use the arrow keys.",
    invalidFile: "The image could not be opened. Try another PNG or JPEG file.",
    croppedTooLarge: "The square image exceeds 10 MB. Choose a smaller source image.",
    cropFailure: "The crop could not be prepared. Choose another image and try again.",
    ownerNavigation: "Owner navigation",
    languageSelection: "Choose language",
    account: "Account",
  },
  id: {
    overview: "Ringkasan",
    restaurants: "Restoran",
    offers: "Penawaran",
    reservations: "Reservasi",
    back: "Pengaturan restoran",
    details: "Detail",
    logo: "Logo",
    pickup: "Pengambilan",
    publish: "Publikasikan",
    title: "Atur posisi logo",
    instruction: "Geser gambar untuk memilih bagian yang tampil dalam area persegi.",
    preview: "Pratinjau logo",
    squareCrop: "Potongan persegi",
    imageRequirements: "Persyaratan gambar",
    formats: "PNG atau JPEG",
    sizeLimit: "Maksimal 10 MB",
    requiredLogo: "Logo wajib tersedia sebelum restoran ini dapat dipublikasikan.",
    selectImage: "Pilih gambar",
    chooseFile: "Pilih file lain",
    rotateLeft: "Putar ke kiri",
    rotateRight: "Putar ke kanan",
    reset: "Atur ulang",
    zoom: "Zoom",
    apply: "Terapkan potongan dan lanjutkan",
    continueSquare: "Lanjutkan dengan gambar persegi",
    choosePrompt: "Pilih gambar PNG atau JPEG hingga 10 MB untuk melihat pratinjau logo.",
    alreadySquare: "Gambar ini sudah persegi, jadi pemotongan dilewati.",
    outputReady: "Gambar persegi siap. Simpan ke profil restoran atau unduh salinannya.",
    download: "Unduh logo persegi",
    saveLogo: "Simpan logo ke restoran",
    savingLogo: "Menyimpan logo…",
    logoSaved: "Logo tersimpan di profil restoran ini.",
    uploadFailure: "Logo tidak dapat disimpan. Periksa koneksi Anda lalu coba lagi.",
    uploadRejected: "Pilih gambar persegi PNG atau JPEG yang valid dan berukuran di bawah 10 MB, lalu coba lagi.",
    uploadNotConfigured: "Cloudinary belum dikonfigurasi. Hubungi administrator situs untuk menyiapkan penyimpanan gambar.",
    backToRestaurant: "Kembali ke restoran",
    saveDetailsFirst: "Simpan detail restoran terlebih dahulu sebelum mengunggah logo.",
    uploadDescription: "Siapkan logo persegi, lalu simpan ke profil restoran ini.",
    dragHelp: "Geser untuk mengatur posisi gambar, atau fokuskan kanvas dan gunakan tombol panah.",
    invalidFile: "Gambar tidak dapat dibuka. Coba file PNG atau JPEG lain.",
    croppedTooLarge: "Gambar persegi melebihi 10 MB. Pilih gambar sumber yang lebih kecil.",
    cropFailure: "Potongan gambar tidak dapat disiapkan. Pilih gambar lain dan coba lagi.",
    ownerNavigation: "Navigasi pemilik",
    languageSelection: "Pilih bahasa",
    account: "Akun",
  },
};

type DragOrigin = {
  pointerX: number;
  pointerY: number;
  panX: number;
  panY: number;
  rangeX: number;
  rangeY: number;
};

function createOrientedCanvas(image: HTMLImageElement, rotation: number): HTMLCanvasElement {
  const swap = rotation % 180 !== 0;
  const canvas = document.createElement("canvas");
  canvas.width = swap ? image.naturalHeight : image.naturalWidth;
  canvas.height = swap ? image.naturalWidth : image.naturalHeight;
  const context = canvas.getContext("2d");
  if (!context) {
    throw new Error("A 2D canvas context is unavailable.");
  }
  context.translate(canvas.width / 2, canvas.height / 2);
  context.rotate((rotation * Math.PI) / 180);
  context.drawImage(image, -image.naturalWidth / 2, -image.naturalHeight / 2);
  return canvas;
}

function getImageFileName(file: File): string {
  return file.name.replace(/\.[^.]+$/, "") || "restaurant-logo";
}

function drawCropPreview(
  source: HTMLCanvasElement,
  canvas: HTMLCanvasElement,
  geometry: CropGeometry,
): void {
  const outputSize = Math.max(1, Math.min(900, Math.round(geometry.sourceSize)));
  canvas.width = outputSize;
  canvas.height = outputSize;
  const context = canvas.getContext("2d");
  if (!context) {
    throw new Error("A 2D canvas context is unavailable.");
  }
  context.drawImage(
    source,
    geometry.sourceLeft,
    geometry.sourceTop,
    geometry.sourceSize,
    geometry.sourceSize,
    0,
    0,
    outputSize,
    outputSize,
  );
}

function getGeometry(
  source: HTMLCanvasElement,
  frameWidth: number,
  frameHeight: number,
  zoom: number,
  panX: number,
  panY: number,
): CropGeometry {
  return getCropGeometry(source.width, source.height, frameWidth, frameHeight, zoom, panX, panY);
}

function SquareCropper({
  language,
  restaurantId,
  initialLogoUrl,
  csrfToken,
  csrfHeaderName,
}: {
  language: Language;
  restaurantId: number | null;
  initialLogoUrl: string | null;
  csrfToken: string | null;
  csrfHeaderName: string | null;
}) {
  const copy = COPY[language];
  const fileInput = useRef<HTMLInputElement>(null);
  const cropCanvas = useRef<HTMLCanvasElement>(null);
  const previewCanvas = useRef<HTMLCanvasElement>(null);
  const dragOrigin = useRef<DragOrigin | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [image, setImage] = useState<HTMLImageElement | null>(null);
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [zoom, setZoom] = useState(100);
  const [rotation, setRotation] = useState(0);
  const [pan, setPan] = useState({ x: 0.5, y: 0.5 });
  const [error, setError] = useState<string | null>(null);
  const [preparedFile, setPreparedFile] = useState<File | null>(null);
  const [preparedUrl, setPreparedUrl] = useState<string | null>(null);
  const [savedLogoUrl, setSavedLogoUrl] = useState<string | null>(initialLogoUrl);
  const [logoSaved, setLogoSaved] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isSquare, setIsSquare] = useState(false);
  const [frameSize, setFrameSize] = useState({ width: 0, height: 0 });

  useEffect(() => {
    const editor = cropCanvas.current;
    if (!editor) return;
    const observer = new ResizeObserver(([entry]) => {
      if (entry) {
        setFrameSize({
          width: entry.contentRect.width,
          height: entry.contentRect.height,
        });
      }
    });
    observer.observe(editor);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!imageUrl) {
      setImage(null);
      return;
    }

    let active = true;
    const loadedImage = new Image();
    loadedImage.onload = () => {
      if (active) {
        setImage(loadedImage);
        setIsSquare(loadedImage.naturalWidth === loadedImage.naturalHeight);
      }
    };
    loadedImage.onerror = () => {
      if (active) {
        setError(copy.invalidFile);
        setImageUrl(null);
        setFile(null);
      }
    };
    loadedImage.src = imageUrl;

    return () => {
      active = false;
    };
  }, [imageUrl, copy.invalidFile]);

  useEffect(
    () => () => {
      if (imageUrl) URL.revokeObjectURL(imageUrl);
    },
    [imageUrl],
  );

  useEffect(
    () => () => {
      if (preparedUrl) URL.revokeObjectURL(preparedUrl);
    },
    [preparedUrl],
  );

  const chooseImage = (nextFile: File | null) => {
    setError(null);
    setLogoSaved(false);
    setPreparedFile(null);
    setPreparedUrl(null);
    setIsSquare(false);
    if (!nextFile) return;

    const validationError = validateImageFile(nextFile);
    if (validationError) {
      setError(validationError);
      if (fileInput.current) fileInput.current.value = "";
      return;
    }

    const nextUrl = URL.createObjectURL(nextFile);
    setImage(null);
    setFile(nextFile);
    setImageUrl(nextUrl);
    setZoom(100);
    setRotation(0);
    setPan({ x: 0.5, y: 0.5 });
  };

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    void chooseImage(event.currentTarget.files?.[0] ?? null);
  };

  const rotatedSource = useCallback(() => {
    if (!image) return null;
    return createOrientedCanvas(image, rotation);
  }, [image, rotation]);

  useEffect(() => {
    const editor = cropCanvas.current;
    const preview = previewCanvas.current;
    const source = rotatedSource();
    if (!editor || !preview) return;

    const frame = editor.getBoundingClientRect();
    const frameWidth = frameSize.width || frame.width;
    const frameHeight = frameSize.height || frame.height;
    if (frameWidth === 0 || frameHeight === 0) return;
    const ratio = window.devicePixelRatio || 1;
    editor.width = Math.round(frameWidth * ratio);
    editor.height = Math.round(frameHeight * ratio);
    const editorContext = editor.getContext("2d");
    if (!editorContext) throw new Error("A 2D canvas context is unavailable.");

    editorContext.setTransform(ratio, 0, 0, ratio, 0, 0);
    editorContext.clearRect(0, 0, frameWidth, frameHeight);
    if (!source) {
      preview.width = 1;
      preview.height = 1;
      const previewContext = preview.getContext("2d");
      if (!previewContext) throw new Error("A 2D canvas context is unavailable.");
      previewContext.clearRect(0, 0, 1, 1);
      return;
    }

    if (isSquare) {
      const side = Math.min(frameWidth, frameHeight);
      editorContext.drawImage(
        source,
        (frameWidth - side) / 2,
        (frameHeight - side) / 2,
        side,
        side,
      );
      preview.width = Math.min(900, source.width);
      preview.height = preview.width;
      const previewContext = preview.getContext("2d");
      if (!previewContext) throw new Error("A 2D canvas context is unavailable.");
      previewContext.drawImage(source, 0, 0, preview.width, preview.height);
    } else {
      const geometry = getGeometry(source, frameWidth, frameHeight, zoom, pan.x, pan.y);
      editorContext.drawImage(
        source,
        geometry.offsetX,
        geometry.offsetY,
        geometry.renderedWidth,
        geometry.renderedHeight,
      );
      drawCropPreview(source, preview, geometry);
    }
  }, [frameSize.height, frameSize.width, image, isSquare, pan.x, pan.y, rotatedSource, zoom]);

  useEffect(() => {
    if (!import.meta.env.DEV || !new URLSearchParams(window.location.search).has("sample")) return;
    let active = true;
    void fetch(sampleLogoUrl)
      .then((response) => {
        if (!response.ok) throw new Error("The demo image could not be loaded.");
        return response.blob();
      })
      .then((blob) => {
        if (active) {
          void chooseImage(new File([blob], "kedai-pagi-logo.png", { type: "image/png" }));
        }
      })
      .catch(() => {
        if (active) setError(copy.invalidFile);
      });
    return () => {
      active = false;
    };
  }, []);

  const rotate = (amount: number) => {
    setRotation((current) => (current + amount + 360) % 360);
    setPreparedFile(null);
    setPreparedUrl(null);
  };

  const reset = () => {
    setZoom(100);
    setRotation(0);
    setPan({ x: 0.5, y: 0.5 });
    setPreparedFile(null);
    setPreparedUrl(null);
  };

  const updatePanFromPointer = (event: PointerEvent<HTMLCanvasElement>) => {
    if (isSaving || !dragOrigin.current || !cropCanvas.current || !image) return;
    const source = rotatedSource();
    if (!source) return;
    const bounds = cropCanvas.current.getBoundingClientRect();
    const geometry = getGeometry(
      source,
      bounds.width,
      bounds.height,
      zoom,
      dragOrigin.current.panX,
      dragOrigin.current.panY,
    );
    const rangeX = geometry.maxOffsetX - geometry.minOffsetX;
    const rangeY = geometry.maxOffsetY - geometry.minOffsetY;
    setPan({
      x:
        rangeX === 0
          ? dragOrigin.current.panX
          : Math.min(
              1,
              Math.max(
                0,
                dragOrigin.current.panX +
                  (event.clientX - dragOrigin.current.pointerX) / rangeX,
              ),
            ),
      y:
        rangeY === 0
          ? dragOrigin.current.panY
          : Math.min(
              1,
              Math.max(
                0,
                dragOrigin.current.panY +
                  (event.clientY - dragOrigin.current.pointerY) / rangeY,
              ),
            ),
    });
    setPreparedFile(null);
  };

  const handlePointerDown = (event: PointerEvent<HTMLCanvasElement>) => {
    if (isSaving || isSquare || !cropCanvas.current || !image) return;
    const source = rotatedSource();
    if (!source) return;
    const bounds = cropCanvas.current.getBoundingClientRect();
    const geometry = getGeometry(source, bounds.width, bounds.height, zoom, pan.x, pan.y);
    dragOrigin.current = {
      pointerX: event.clientX,
      pointerY: event.clientY,
      panX: pan.x,
      panY: pan.y,
      rangeX: geometry.maxOffsetX - geometry.minOffsetX,
      rangeY: geometry.maxOffsetY - geometry.minOffsetY,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLCanvasElement>) => {
    if (isSaving || isSquare || !image) return;
    const step = event.shiftKey ? 0.1 : 0.025;
    const delta = {
      ArrowLeft: { x: -step, y: 0 },
      ArrowRight: { x: step, y: 0 },
      ArrowUp: { x: 0, y: -step },
      ArrowDown: { x: 0, y: step },
    }[event.key];
    if (!delta) return;
    event.preventDefault();
    setPan((current) => ({
      x: Math.min(1, Math.max(0, current.x + delta.x)),
      y: Math.min(1, Math.max(0, current.y + delta.y)),
    }));
    setPreparedFile(null);
  };

  const createPreparedFile = async () => {
    if (!file || !image) return;
    setError(null);
    const source = rotatedSource();
    const editor = cropCanvas.current;
    if (!source || !editor) return;
    let outputBlob: Blob;
    if (isSquare) {
      const output = document.createElement("canvas");
      output.width = Math.min(900, source.width);
      output.height = output.width;
      const context = output.getContext("2d");
      if (!context) throw new Error("A 2D canvas context is unavailable.");
      context.drawImage(source, 0, 0, output.width, output.height);
      outputBlob = await new Promise<Blob>((resolve, reject) => {
        output.toBlob(
          (blob) => (blob ? resolve(blob) : reject(new Error("The square image could not be encoded."))),
          file.type,
          file.type === "image/jpeg" ? 0.92 : undefined,
        );
      });
    } else {
      const bounds = editor.getBoundingClientRect();
      const geometry = getGeometry(source, bounds.width, bounds.height, zoom, pan.x, pan.y);
      const output = document.createElement("canvas");
      output.width = Math.max(1, Math.min(900, Math.round(geometry.sourceSize)));
      output.height = output.width;
      const context = output.getContext("2d");
      if (!context) throw new Error("A 2D canvas context is unavailable.");
      context.drawImage(
        source,
        geometry.sourceLeft,
        geometry.sourceTop,
        geometry.sourceSize,
        geometry.sourceSize,
        0,
        0,
        output.width,
        output.height,
      );
      outputBlob = await new Promise<Blob>((resolve, reject) => {
        output.toBlob(
          (blob) => (blob ? resolve(blob) : reject(new Error("The crop could not be encoded."))),
          file.type,
          file.type === "image/jpeg" ? 0.92 : undefined,
        );
      });
    }

    const extension = file.type === "image/jpeg" ? ".jpg" : ".png";
    const nextFile = new File(
      [outputBlob],
      `${getImageFileName(file)}-square${extension}`,
      { type: file.type },
    );
    if (nextFile.size > 10 * 1024 * 1024) {
      setError(copy.croppedTooLarge);
      return;
    }
    const nextUrl = URL.createObjectURL(nextFile);
    setPreparedUrl((currentUrl) => {
      if (currentUrl) URL.revokeObjectURL(currentUrl);
      return nextUrl;
    });
    setPreparedFile(nextFile);
  };

  const saveLogo = async () => {
    if (!preparedFile || !restaurantId) {
      setError(copy.saveDetailsFirst);
      return;
    }
    if (!csrfToken || !csrfHeaderName) {
      setError(copy.uploadFailure);
      return;
    }

    setIsSaving(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append("logo", preparedFile);
      const response = await fetch(`/owner/restaurant/${restaurantId}/logo`, {
        method: "PUT",
        credentials: "same-origin",
        headers: { [csrfHeaderName]: csrfToken },
        body: formData,
      });
      if (response.status === 503) {
        setError(copy.uploadNotConfigured);
        return;
      }
      if (!response.ok) {
        setError(response.status === 400 || response.status === 413 || response.status === 415
          ? copy.uploadRejected
          : copy.uploadFailure);
        return;
      }

      const result: unknown = await response.json();
      if (
        typeof result !== "object"
        || result === null
        || !("logoUrl" in result)
        || typeof result.logoUrl !== "string"
      ) {
        throw new Error("The logo upload response is invalid.");
      }
      setSavedLogoUrl(result.logoUrl);
      setLogoSaved(true);
    } catch (uploadError: unknown) {
      if (uploadError instanceof Error) {
        setError(copy.uploadFailure);
        console.error("Could not save the restaurant logo.", uploadError);
      } else {
        throw uploadError;
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <>
      <header className="topbar">
        <a className="brand" href="/" aria-label="Eco-Nomz">
          <img src={brandLogoUrl} alt="Eco-Nomz" />
        </a>
        <nav className="owner-nav" aria-label={copy.ownerNavigation}>
          <a href="/owner">{copy.overview}</a>
          <a className="active" href="/owner/restaurant">{copy.restaurants}</a>
          <a href="/owner/offers">{copy.offers}</a>
          <a href="/owner/orders">{copy.reservations}</a>
        </nav>
        <div className="header-tools">
          <div className="language-switch" role="group" aria-label={copy.languageSelection}>
            <a
              href={`/language?lang=en&returnTo=${encodeURIComponent(window.location.pathname)}`}
              aria-current={language === "en" ? "true" : undefined}
            >
              EN
            </a>
            <span aria-hidden="true">|</span>
            <a
              href={`/language?lang=id&returnTo=${encodeURIComponent(window.location.pathname)}`}
              aria-current={language === "id" ? "true" : undefined}
            >
              ID
            </a>
          </div>
          <a className="account-link" href="/owner" aria-label={copy.account}>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="8" r="3.25" />
              <path d="M5.5 20c.7-3.4 3-5.2 6.5-5.2s5.8 1.8 6.5 5.2" />
            </svg>
          </a>
        </div>
      </header>

      <main className="page-shell">
        <a className="breadcrumb" href={restaurantId ? `/owner/restaurant/edit?id=${restaurantId}` : "/owner/restaurant"}>
          <span aria-hidden="true">←</span> {copy.back}
        </a>

        <ol className="steps" aria-label="Restaurant setup progress">
          <li className="step complete"><span aria-hidden="true">✓</span>{copy.details}</li>
          <li className="step active" aria-current="step"><span>2</span>{copy.logo}</li>
          <li className="step"><span>3</span>{copy.pickup}</li>
          <li className="step"><span>4</span>{copy.publish}</li>
        </ol>

        <section className="page-heading" aria-labelledby="page-title">
          <h1 id="page-title">{copy.title}</h1>
          <p>{copy.instruction}</p>
        </section>

        <div className="workspace">
          <section className="editor-panel" aria-label={copy.title}>
            <div className={`crop-stage${image && !isSquare ? " is-draggable" : ""}`}>
              <canvas
                ref={cropCanvas}
                className="source-canvas"
                role="group"
                tabIndex={image && !isSquare ? 0 : -1}
                aria-label={copy.dragHelp}
                onPointerDown={handlePointerDown}
                onPointerMove={updatePanFromPointer}
                onPointerUp={() => { dragOrigin.current = null; }}
                onPointerCancel={() => { dragOrigin.current = null; }}
                onKeyDown={handleKeyDown}
              />
              {image && !isSquare && <CropOverlay />}
              {!image && (
                <button
                  className="empty-picker"
                  type="button"
                  onClick={() => {
                    if (fileInput.current) fileInput.current.value = "";
                    fileInput.current?.click();
                  }}
                >
                  <span className="upload-mark" aria-hidden="true">
                    <UploadIcon />
                  </span>
                  <strong>{copy.selectImage}</strong>
                  <span>{copy.choosePrompt}</span>
                </button>
              )}
              {image && isSquare && <p className="square-bypass">{copy.alreadySquare}</p>}
            </div>

            <div className="editor-controls">
              <button type="button" className="secondary-button" disabled={!image || isSquare || isSaving} onClick={() => rotate(-90)}>
                {copy.rotateLeft}
              </button>
              <button type="button" className="secondary-button" disabled={!image || isSquare || isSaving} onClick={() => rotate(90)}>
                {copy.rotateRight}
              </button>
              <button type="button" className="secondary-button" disabled={!image || isSquare || isSaving} onClick={reset}>
                {copy.reset}
              </button>
              <label className="zoom-control" htmlFor="zoom">
                <span>{copy.zoom}</span>
                <input
                  id="zoom"
                  type="range"
                  min="100"
                  max="200"
                  step="1"
                  value={zoom}
                  disabled={!image || isSquare || isSaving}
                  aria-valuetext={`${zoom}%`}
                  onChange={(event) => {
                    setZoom(Number(event.currentTarget.value));
                    setPreparedFile(null);
                  }}
                />
              </label>
            </div>
          </section>

          <aside className="preview-panel" aria-labelledby="preview-title">
            <h2 id="preview-title">{copy.preview}</h2>
            <p className="preview-caption">{copy.squareCrop}</p>
            <div className="preview-frame">
              {image ? (
                <canvas ref={previewCanvas} aria-label={`${copy.preview}: ${file?.name ?? ""}`} />
              ) : savedLogoUrl ? (
                <img src={savedLogoUrl} alt={copy.preview} />
              ) : (
                <div className="preview-empty" aria-hidden="true">
                  <UploadIcon />
                </div>
              )}
            </div>
            <div className="requirements">
              <strong>{copy.imageRequirements}</strong>
              <span>{copy.formats}</span>
              <span>{copy.sizeLimit}</span>
            </div>
            {!savedLogoUrl && <p className="required-notice">{copy.requiredLogo}</p>}
          </aside>
        </div>

        <div className="page-footer">
          <div className="file-actions">
            <button
              type="button"
              className="text-button"
              disabled={isSaving}
              onClick={() => {
                if (fileInput.current) fileInput.current.value = "";
                fileInput.current?.click();
              }}
            >
              {copy.chooseFile}
            </button>
            {preparedUrl && preparedFile && (
              <a className="download-link" href={preparedUrl} download={preparedFile.name}>
                {copy.download}
              </a>
            )}
          </div>
          <button
            type="button"
            className="primary-button"
            disabled={!image || isSaving || logoSaved || (Boolean(preparedFile) && !restaurantId)}
            aria-busy={isSaving}
            onClick={() => {
              if (preparedFile) {
                void saveLogo();
              } else {
                void createPreparedFile().catch((processingError: unknown) => {
                  if (processingError instanceof Error) {
                    setError(copy.cropFailure);
                    console.error("Could not prepare the square logo image.", processingError);
                  } else {
                    throw processingError;
                  }
                });
              }
            }}
          >
            {isSaving
              ? copy.savingLogo
              : logoSaved
                ? copy.logoSaved
                : preparedFile && restaurantId
                  ? copy.saveLogo
                  : isSquare
                    ? copy.continueSquare
                    : copy.apply}
          </button>
        </div>

        {error && <p className="error-message" role="alert">{error}</p>}
        {logoSaved && <p className="success-message" role="status">{copy.logoSaved}</p>}
        {preparedFile && !logoSaved && <p className="success-message" role="status">{copy.outputReady}</p>}
        {!logoSaved && (
          <p className="storage-disclosure">{restaurantId ? copy.uploadDescription : copy.saveDetailsFirst}</p>
        )}
        {logoSaved && restaurantId && (
          <a className="profile-link" href={`/owner/restaurant/edit?id=${restaurantId}`}>
            {copy.backToRestaurant}
          </a>
        )}
        <input
          ref={fileInput}
          className="visually-hidden"
          type="file"
          accept="image/png,image/jpeg"
          onChange={handleFileChange}
          aria-label={copy.selectImage}
        />
      </main>
    </>
  );
}

function CropOverlay() {
  return (
    <div className="crop-overlay" aria-hidden="true">
      <span className="crop-shade left" />
      <span className="crop-shade right" />
      <div className="crop-edge" />
      <span className="crop-handle top-left" />
      <span className="crop-handle bottom-right" />
    </div>
  );
}

function UploadIcon() {
  return (
    <svg viewBox="0 0 32 32" fill="none" aria-hidden="true">
      <path d="M16 21V5m0 0L9.5 11.5M16 5l6.5 6.5M6 20v6h20v-6" />
    </svg>
  );
}

export function LogoCropper({
  language,
  restaurantId,
  logoUrl,
  csrfToken,
  csrfHeaderName,
}: {
  language: Language;
  restaurantId: number | null;
  logoUrl: string | null;
  csrfToken: string | null;
  csrfHeaderName: string | null;
}) {
  return (
    <SquareCropper
      language={language}
      restaurantId={restaurantId}
      initialLogoUrl={logoUrl}
      csrfToken={csrfToken}
      csrfHeaderName={csrfHeaderName}
    />
  );
}
