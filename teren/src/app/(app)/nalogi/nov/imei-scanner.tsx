"use client";

import { useEffect, useRef, useState } from "react";
import { BrowserMultiFormatReader, BrowserCodeReader } from "@zxing/browser";
import type { IScannerControls } from "@zxing/browser";
import { createWorker, type Worker as TesseractWorker } from "tesseract.js";

const OCR_INTERVAL_MS = 1800;
// Ujame natanko 10 zaporednih številk, ki niso del daljšega niza (npr. serijske št.).
const ISOLATED_10_DIGITS = /(?<!\d)\d{10}(?!\d)/;

// Brskalniki po eksplicitni zavrnitvi namenoma ne vprašajo za dovoljenje znova
// (zaščita pred spamanjem s pozivi) — edina pot naprej je ročna sprememba
// nastavitve, zato uporabniku pokažemo natančna navodila namesto splošne napake.
const PERMISSION_HELP =
  'Dostop do kamere je blokiran za to stran. Če je aplikacija odprta v brskalniku: tapni ikono ključavnice ali "i" poleg naslova strani → Dovoljenja → Kamera → Dovoli. Če je nameščena na zaslonu telefona: Nastavitve telefona → Aplikacije → Teren → Dovoljenja → Kamera → Dovoli. Nato pritisni "Poskusi znova".';

function messageForError(err: unknown): string {
  const name = err instanceof DOMException ? err.name : "";
  if (name === "NotAllowedError" || name === "PermissionDeniedError") return PERMISSION_HELP;
  if (name === "NotFoundError" || name === "DevicesNotFoundError") return "Na napravi ni zaznane kamere.";
  if (name === "NotReadableError" || name === "TrackStartError")
    return "Kamera je zasedena — morda jo uporablja druga aplikacija. Zapri jo in poskusi znova.";
  return "Dostop do kamere ni uspel. Preveri dovoljenja za kamero ali vnesi IMEI ročno.";
}

export function ImeiScanner({ onScan, onClose }: { onScan: (digits: string) => void; onClose: () => void }) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const controlsRef = useRef<IScannerControls | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const trackRef = useRef<MediaStreamTrack | null>(null);
  const scannedRef = useRef(false);
  const [error, setError] = useState<string | null>(null);
  const [torchOn, setTorchOn] = useState(false);
  const [attempt, setAttempt] = useState(0);

  // Če brskalnik podpira Permissions API, med odprtim skenerjem opazuj stanje
  // dovoljenja za kamero — če ga uporabnik spremeni (npr. v nastavitvah strani),
  // samodejno ponovi poskus brez ročnega pritiska na "Poskusi znova".
  useEffect(() => {
    let status: PermissionStatus | null = null;
    let cancelled = false;
    navigator.permissions
      ?.query({ name: "camera" as PermissionName })
      .then((result) => {
        if (cancelled) return;
        status = result;
        status.onchange = () => {
          if (status?.state === "granted") setAttempt((n) => n + 1);
        };
      })
      .catch(() => {
        // Permissions API za kamero ni podprt (npr. Safari) — ročni gumb ostane edina pot.
      });
    return () => {
      cancelled = true;
      if (status) status.onchange = null;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    let ocrTimer: ReturnType<typeof setInterval> | null = null;
    let ocrBusy = false;
    let worker: TesseractWorker | null = null;
    const reader = new BrowserMultiFormatReader();

    function reportScan(digits: string) {
      if (scannedRef.current) return;
      scannedRef.current = true;
      controlsRef.current?.stop();
      onScan(digits);
    }

    async function start() {
      setError(null);
      let stream: MediaStream;
      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: { ideal: "environment" },
            width: { ideal: 1920 },
            height: { ideal: 1080 },
          },
        });
      } catch (err) {
        if (!cancelled) setError(messageForError(err));
        return;
      }
      if (cancelled) {
        stream.getTracks().forEach((t) => t.stop());
        return;
      }
      streamRef.current = stream;

      const track = stream.getVideoTracks()[0];
      if (track) {
        trackRef.current = track;
        // Poskusi vklopiti ne glede na to, kaj javi torch-compatible preverba —
        // ta je na nekaterih napravah nezanesljiva, sam poskus pa je neškodljiv.
        BrowserCodeReader.mediaStreamSetTorch(track, true)
          .then(() => setTorchOn(true))
          .catch(() => {});
      }

      try {
        const controls = await reader.decodeFromStream(stream, videoRef.current ?? undefined, (result) => {
          if (result) {
            const digits = result.getText().replace(/\D/g, "").slice(-10);
            if (digits.length === 10) reportScan(digits);
          }
        });
        if (cancelled) {
          controls.stop();
          return;
        }
        controlsRef.current = controls;
      } catch {
        // Dekodiranje črtne kode se ni uspelo zagnati — OCR spodaj deluje neodvisno naprej.
      }

      try {
        worker = await createWorker("eng");
        await worker.setParameters({ tessedit_char_whitelist: "0123456789" });
      } catch {
        worker = null;
      }

      if (worker && !cancelled) {
        const canvas = document.createElement("canvas");
        ocrTimer = setInterval(() => {
          if (ocrBusy || cancelled || !worker || !videoRef.current) return;
          const video = videoRef.current;
          if (video.readyState < 2 || video.videoWidth === 0) return;

          ocrBusy = true;
          (async () => {
            try {
              canvas.width = video.videoWidth;
              canvas.height = video.videoHeight;
              const ctx = canvas.getContext("2d");
              if (ctx && worker) {
                ctx.drawImage(video, 0, 0);
                const {
                  data: { text },
                } = await worker.recognize(canvas);
                const match = text.match(ISOLATED_10_DIGITS);
                if (match) reportScan(match[0]);
              }
            } catch {
              // spodletel poskus OCR, poskusimo znova ob naslednjem intervalu
            } finally {
              ocrBusy = false;
            }
          })();
        }, OCR_INTERVAL_MS);
      }
    }

    start();

    return () => {
      cancelled = true;
      if (ocrTimer) clearInterval(ocrTimer);
      controlsRef.current?.stop();
      streamRef.current?.getTracks().forEach((t) => t.stop());
      worker?.terminate().catch(() => {});
    };
  }, [onScan, attempt]);

  function handleToggleTorch() {
    const track = trackRef.current;
    if (!track) return;
    const next = !torchOn;
    BrowserCodeReader.mediaStreamSetTorch(track, next)
      .then(() => setTorchOn(next))
      .catch(() => {});
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-black">
      <div className="flex items-center justify-between gap-2 bg-black/80 p-4">
        <p className="text-base text-white">Usmeri kamero na črtno kodo ali št. IMEI</p>
        <div className="flex shrink-0 gap-2">
          <button
            type="button"
            onClick={handleToggleTorch}
            className="rounded-md bg-white/10 px-3 py-2 text-base text-white hover:bg-white/20"
          >
            {torchOn ? "💡 Ugasni" : "💡 Prižgi"}
          </button>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md bg-white/10 px-4 py-2 text-base text-white hover:bg-white/20"
          >
            Prekliči
          </button>
        </div>
      </div>
      <div className="relative flex-1">
        {/* eslint-disable-next-line jsx-a11y/media-has-caption */}
        <video ref={videoRef} muted playsInline className="h-full w-full object-cover" />
        <div className="pointer-events-none absolute inset-x-8 top-1/2 h-24 -translate-y-1/2 rounded-lg border-2 border-white/80" />
      </div>
      {error && (
        <div className="space-y-3 bg-black/80 p-4">
          <p className="text-base text-red-400">{error}</p>
          <button
            type="button"
            onClick={() => setAttempt((n) => n + 1)}
            className="rounded-md bg-white/10 px-4 py-2 text-base text-white hover:bg-white/20"
          >
            Poskusi znova
          </button>
        </div>
      )}
    </div>
  );
}
