"use client";

import { useEffect, useState } from "react";

export function ImageLightbox({
  src,
  alt,
  children,
}: {
  src: string;
  alt: string;
  children: React.ReactNode;
}) {
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (!isOpen) return;

    history.pushState({ lightbox: true }, "");
    const handlePopState = () => setIsOpen(false);
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [isOpen]);

  function close() {
    // Pojdi nazaj po zgodovini — popstate poslušalec zapre lightbox.
    // Tako Android gumb "nazaj" in gumb za zapiranje delujeta enako in ne zapustita strani.
    history.back();
  }

  return (
    <>
      <button type="button" onClick={() => setIsOpen(true)} className="block w-full border-0 bg-transparent p-0">
        {children}
      </button>
      {isOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4"
          onClick={close}
        >
          <button
            type="button"
            onClick={close}
            aria-label="Zapri"
            className="absolute right-4 top-4 flex h-11 w-11 items-center justify-center rounded-full bg-white/10 text-2xl text-white hover:bg-white/20"
          >
            ×
          </button>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={src}
            alt={alt}
            className="max-h-full max-w-full object-contain"
            onClick={(e) => e.stopPropagation()}
          />
        </div>
      )}
    </>
  );
}
