"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import SignaturePad, { SignaturePadHandle } from "./SignaturePad";
import PhotoPicker from "./PhotoPicker";
import {
  WORK_ORDER_TYPE_LABELS,
  DIFFICULTY_LABELS,
  INSTALLER_NAME_LABELS,
  OPTION_TYPE_LABELS,
} from "@/lib/constants";

type Client = { id: string; name: string };

const INSTALLER_KEYS = Object.keys(INSTALLER_NAME_LABELS);
const OPTION_KEYS = Object.keys(OPTION_TYPE_LABELS);

export default function WorkOrderForm() {
  const router = useRouter();
  const sigRef = useRef<SignaturePadHandle>(null);

  const [clients, setClients] = useState<Client[]>([]);
  const [type, setType] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [clientId, setClientId] = useState("");

  const [installerChecked, setInstallerChecked] = useState<Record<string, boolean>>({});
  const [installerOtherText, setInstallerOtherText] = useState("");

  const [vehiclePlate, setVehiclePlate] = useState("");
  const [vehicleBrand, setVehicleBrand] = useState("");
  const [vehicleModel, setVehicleModel] = useState("");
  const [vehicleYear, setVehicleYear] = useState("");
  const [imei, setImei] = useState("");
  const [imeiPrev, setImeiPrev] = useState("");

  const [optionChecked, setOptionChecked] = useState<Record<string, boolean>>({});
  const [optionComment, setOptionComment] = useState<Record<string, string>>({});

  const [comment, setComment] = useState("");
  const [photos, setPhotos] = useState<File[]>([]);

  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [savedIdent, setSavedIdent] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/clients")
      .then((r) => r.json())
      .then(setClients)
      .catch(() => setError("Napaka pri nalaganju seznama strank."));
  }, []);

  function toggleInstaller(key: string) {
    setInstallerChecked((prev) => ({ ...prev, [key]: !prev[key] }));
  }

  function toggleOption(key: string) {
    setOptionChecked((prev) => ({ ...prev, [key]: !prev[key] }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!type) return setError("Izberite tip naloga.");
    if (!difficulty) return setError("Izberite zahtevnost.");
    if (!clientId) return setError("Izberite stranko.");

    const installers = INSTALLER_KEYS.filter((k) => installerChecked[k]).map((k) => ({
      name: k,
      otherText: k === "OSTALO" ? installerOtherText.trim() : undefined,
    }));
    if (installers.length === 0) return setError("Izberite vsaj enega monterja.");
    if (installers.some((i) => i.name === "OSTALO" && !i.otherText)) {
      return setError("Vpišite ime monterja pri izbiri 'Ostalo'.");
    }

    if (!/^\d{10}$/.test(imei)) return setError("IMEI mora vsebovati natanko 10 številk.");
    if (type === "INTERVENCIJA" && !/^\d{10}$/.test(imeiPrev)) {
      return setError("IMEI prej mora vsebovati natanko 10 številk (obvezno pri intervenciji).");
    }

    if (!sigRef.current || sigRef.current.isEmpty()) {
      return setError("Stranka mora podpisati nalog.");
    }

    const signatureFile = await sigRef.current.getFile();
    if (!signatureFile) return setError("Podpisa ni bilo mogoče shraniti, poskusite znova.");

    const options = OPTION_KEYS.filter((k) => optionChecked[k]).map((k) => ({
      optionType: k,
      comment: optionComment[k]?.trim() || undefined,
    }));

    const formData = new FormData();
    formData.set("type", type);
    formData.set("difficulty", difficulty);
    formData.set("clientId", clientId);
    formData.set("installers", JSON.stringify(installers));
    formData.set("vehiclePlate", vehiclePlate);
    formData.set("vehicleBrand", vehicleBrand);
    formData.set("vehicleModel", vehicleModel);
    formData.set("vehicleYear", vehicleYear);
    formData.set("imei", imei);
    if (type === "INTERVENCIJA") formData.set("imeiPrev", imeiPrev);
    formData.set("options", JSON.stringify(options));
    if (comment.trim()) formData.set("comment", comment.trim());
    formData.set("signature", signatureFile, "podpis.png");
    photos.forEach((p) => formData.append("photos", p));

    setSaving(true);
    try {
      const res = await fetch("/api/work-orders", { method: "POST", body: formData });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        setError(data.error || "Shranjevanje ni uspelo.");
        return;
      }
      const data = await res.json();
      setSavedIdent(data.ident);
    } finally {
      setSaving(false);
    }
  }

  if (savedIdent) {
    return (
      <div className="max-w-lg mx-auto p-6 text-center space-y-4">
        <div className="text-5xl">✓</div>
        <h2 className="text-xl font-semibold">Nalog shranjen</h2>
        <p className="text-gray-600">Št. naloga: <span className="font-mono font-semibold">{savedIdent}</span></p>
        <div className="flex gap-3 justify-center">
          <button
            className="bg-gray-900 text-white rounded-lg px-4 py-2"
            onClick={() => router.push("/nalogi")}
          >
            Nazaj na seznam
          </button>
          <button className="border border-gray-300 rounded-lg px-4 py-2" onClick={() => window.location.reload()}>
            Nov nalog
          </button>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-lg mx-auto p-4 space-y-6 pb-24">
      <h1 className="text-lg font-semibold">Nov delovni nalog</h1>

      {error && <p className="text-sm text-red-600 bg-red-50 rounded p-2">{error}</p>}

      <section className="space-y-2">
        <label className="block text-sm font-medium">Tip naloga</label>
        <div className="grid grid-cols-2 gap-2">
          {Object.entries(WORK_ORDER_TYPE_LABELS).map(([key, label]) => (
            <button
              type="button"
              key={key}
              onClick={() => setType(key)}
              className={`rounded-lg border px-3 py-2 text-sm ${
                type === key ? "bg-gray-900 text-white border-gray-900" : "border-gray-300"
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </section>

      <section className="space-y-2">
        <label className="block text-sm font-medium">Zahtevnost</label>
        <div className="grid grid-cols-2 gap-2">
          {Object.entries(DIFFICULTY_LABELS).map(([key, label]) => (
            <button
              type="button"
              key={key}
              onClick={() => setDifficulty(key)}
              className={`rounded-lg border px-3 py-2 text-sm ${
                difficulty === key ? "bg-gray-900 text-white border-gray-900" : "border-gray-300"
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </section>

      <section>
        <label className="block text-sm font-medium mb-1">Stranka</label>
        <select
          className="w-full rounded-lg border border-gray-300 px-3 py-2"
          value={clientId}
          onChange={(e) => setClientId(e.target.value)}
        >
          <option value="">— izberi stranko —</option>
          {clients.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </section>

      <section>
        <label className="block text-sm font-medium mb-1">Monter (lahko več)</label>
        <div className="grid grid-cols-2 gap-2">
          {INSTALLER_KEYS.map((key) => (
            <label key={key} className="flex items-center gap-2 text-sm border border-gray-300 rounded-lg px-3 py-2">
              <input type="checkbox" checked={!!installerChecked[key]} onChange={() => toggleInstaller(key)} />
              {INSTALLER_NAME_LABELS[key]}
            </label>
          ))}
        </div>
        {installerChecked["OSTALO"] && (
          <input
            className="w-full rounded-lg border border-gray-300 px-3 py-2 mt-2"
            placeholder="Vpiši ime monterja"
            value={installerOtherText}
            onChange={(e) => setInstallerOtherText(e.target.value)}
          />
        )}
      </section>

      <section className="space-y-3">
        <label className="block text-sm font-medium mb-1">Registrska št. vozila</label>
        <input
          className="w-full rounded-lg border border-gray-300 px-3 py-2"
          value={vehiclePlate}
          onChange={(e) => setVehiclePlate(e.target.value)}
          required
        />

        <label className="block text-sm font-medium mb-1">Vozilo</label>
        <div className="grid grid-cols-3 gap-2">
          <input
            className="rounded-lg border border-gray-300 px-3 py-2"
            placeholder="Znamka"
            value={vehicleBrand}
            onChange={(e) => setVehicleBrand(e.target.value)}
            required
          />
          <input
            className="rounded-lg border border-gray-300 px-3 py-2"
            placeholder="Model"
            value={vehicleModel}
            onChange={(e) => setVehicleModel(e.target.value)}
            required
          />
          <input
            className="rounded-lg border border-gray-300 px-3 py-2"
            placeholder="Letnik"
            inputMode="numeric"
            value={vehicleYear}
            onChange={(e) => setVehicleYear(e.target.value)}
            required
          />
        </div>
      </section>

      <section className="space-y-2">
        <label className="block text-sm font-medium mb-1">Št. IMEI (zadnjih 10 mest)</label>
        <input
          className="w-full rounded-lg border border-gray-300 px-3 py-2 font-mono"
          inputMode="numeric"
          maxLength={10}
          value={imei}
          onChange={(e) => setImei(e.target.value.replace(/\D/g, ""))}
          required
        />

        {type === "INTERVENCIJA" && (
          <>
            <label className="block text-sm font-medium mb-1">Št. IMEI prej (zadnjih 10 mest)</label>
            <input
              className="w-full rounded-lg border border-gray-300 px-3 py-2 font-mono"
              inputMode="numeric"
              maxLength={10}
              value={imeiPrev}
              onChange={(e) => setImeiPrev(e.target.value.replace(/\D/g, ""))}
              required
            />
          </>
        )}
      </section>

      <section>
        <label className="block text-sm font-medium mb-2">Izbire (DIN/ANI/CAN)</label>
        <div className="space-y-2">
          {OPTION_KEYS.map((key) => (
            <div key={key} className="border border-gray-300 rounded-lg p-2">
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={!!optionChecked[key]} onChange={() => toggleOption(key)} />
                {OPTION_TYPE_LABELS[key]}
              </label>
              {optionChecked[key] && (
                <input
                  className="w-full rounded-lg border border-gray-200 px-2 py-1 mt-2 text-sm"
                  placeholder="Komentar (neobvezno)"
                  value={optionComment[key] || ""}
                  onChange={(e) => setOptionComment((prev) => ({ ...prev, [key]: e.target.value }))}
                />
              )}
            </div>
          ))}
        </div>
      </section>

      <section>
        <label className="block text-sm font-medium mb-1">Slike</label>
        <PhotoPicker photos={photos} onChange={setPhotos} />
      </section>

      <section>
        <label className="block text-sm font-medium mb-1">Komentar</label>
        <textarea
          className="w-full rounded-lg border border-gray-300 px-3 py-2"
          rows={3}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
        />
      </section>

      <section>
        <label className="block text-sm font-medium mb-1">Podpis stranke</label>
        <SignaturePad ref={sigRef} />
        <button
          type="button"
          className="text-sm text-gray-500 mt-1"
          onClick={() => sigRef.current?.clear()}
        >
          Počisti podpis
        </button>
      </section>

      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4">
        <button
          type="submit"
          disabled={saving}
          className="max-w-lg mx-auto block w-full bg-gray-900 text-white rounded-lg py-3 font-medium disabled:opacity-50"
        >
          {saving ? "Shranjujem ..." : "Shrani"}
        </button>
      </div>
    </form>
  );
}
