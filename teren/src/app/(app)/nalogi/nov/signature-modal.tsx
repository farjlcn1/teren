"use client";

import { useEffect, useRef } from "react";
import SignatureCanvas from "react-signature-canvas";

export function SignatureModal({ onSave, onClose }: { onSave: (dataUrl: string) => void; onClose: () => void }) {
  const sigRef = useRef<SignatureCanvas>(null);

  useEffect(() => {
    history.pushState({ signatureModal: true }, "");
    const handlePopState = () => onClose();
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [onClose]);

  function close() {
    history.back();
  }

  function handleSave() {
    if (sigRef.current && !sigRef.current.isEmpty()) {
      onSave(sigRef.current.getTrimmedCanvas().toDataURL("image/png"));
    }
    close();
  }

  return (
    <div className="fixed inset-0 z-50 flex flex-col bg-black">
      <div className="flex items-center justify-between gap-2 bg-black/80 p-4">
        <p className="text-base text-white">Podpis stranke</p>
        <button
          type="button"
          onClick={close}
          className="rounded-md bg-white/10 px-4 py-2 text-base text-white hover:bg-white/20"
        >
          Prekliči
        </button>
      </div>
      <div
        className="flex-1 bg-white"
        onPointerDown={() => {
          if (document.activeElement instanceof HTMLElement) document.activeElement.blur();
        }}
      >
        <SignatureCanvas
          ref={sigRef}
          penColor="black"
          clearOnResize={false}
          canvasProps={{ className: "h-full w-full touch-none" }}
        />
      </div>
      <div className="flex gap-2 bg-black/80 p-4">
        <button
          type="button"
          onClick={() => sigRef.current?.clear()}
          className="flex-1 rounded-md bg-white/10 px-4 py-2 text-base text-white hover:bg-white/20"
        >
          Počisti
        </button>
        <button
          type="button"
          onClick={handleSave}
          className="flex-1 rounded-md bg-blue-600 px-4 py-2 text-base font-medium text-white hover:bg-blue-500"
        >
          Shrani
        </button>
      </div>
    </div>
  );
}
