"use client";

import { useState, useEffect } from "react";

type Props = {
  photos: File[];
  onChange: (photos: File[]) => void;
};

export default function PhotoPicker({ photos, onChange }: Props) {
  const [previews, setPreviews] = useState<string[]>([]);

  useEffect(() => {
    const urls = photos.map((f) => URL.createObjectURL(f));
    setPreviews(urls);
    return () => urls.forEach((u) => URL.revokeObjectURL(u));
  }, [photos]);

  function handleFiles(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length > 0) {
      onChange([...photos, ...files]);
    }
    e.target.value = "";
  }

  function removeAt(index: number) {
    onChange(photos.filter((_, i) => i !== index));
  }

  return (
    <div>
      <label className="inline-block bg-gray-100 border border-gray-300 rounded-lg px-4 py-2 text-sm font-medium cursor-pointer">
        Dodaj sliko
        <input type="file" accept="image/*" capture="environment" multiple className="hidden" onChange={handleFiles} />
      </label>

      {previews.length > 0 && (
        <div className="grid grid-cols-3 gap-2 mt-3">
          {previews.map((src, i) => (
            <div key={i} className="relative">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={src} alt={`Slika ${i + 1}`} className="w-full h-24 object-cover rounded-lg border border-gray-200" />
              <button
                type="button"
                onClick={() => removeAt(i)}
                className="absolute -top-2 -right-2 bg-red-600 text-white rounded-full w-6 h-6 text-xs leading-none"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
