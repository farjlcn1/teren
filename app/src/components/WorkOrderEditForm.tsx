"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  WORK_ORDER_TYPE_LABELS,
  DIFFICULTY_LABELS,
  INSTALLER_NAME_LABELS,
  OPTION_TYPE_LABELS,
} from "@/lib/constants";

type Client = { id: string; name: string };
type Installer = { name: string; otherText: string | null };
type OptionItem = { optionType: string; comment: string | null };

type OrderData = {
  id: string;
  type: string;
  difficulty: string;
  clientId: string;
  vehiclePlate: string;
  vehicleBrand: string;
  vehicleModel: string;
  vehicleYear: string;
  imei: string;
  imeiPrev: string | null;
  comment: string | null;
  installers: Installer[];
  options: OptionItem[];
};

const INSTALLER_KEYS = Object.keys(INSTALLER_NAME_LABELS);
const OPTION_KEYS = Object.keys(OPTION_TYPE_LABELS);

export default function WorkOrderEditForm({ order, clients }: { order: OrderData; clients: Client[] }) {
  const router = useRouter();

  const [type, setType] = useState(order.type);
  const [difficulty, setDifficulty] = useState(order.difficulty);
  const [clientId, setClientId] = useState(order.clientId);

  const [installerChecked, setInstallerChecked] = useState<Record<string, boolean>>(
    Object.fromEntries(order.installers.map((i) => [i.name, true]))
  );
  const [installerOtherText, setInstallerOtherText] = useState(
    order.installers.find((i) => i.name === "OSTALO")?.otherText || ""
  );

  const [vehiclePlate, setVehiclePlate] = useState(order.vehiclePlate);
  const [vehicleBrand, setVehicleBrand] = useState(order.vehicleBrand);
  const [vehicleModel, setVehicleModel] = useState(order.vehicleModel);
  const [vehicleYear, setVehicleYear] = useState(order.vehicleYear);
  const [imei, setImei] = useState(order.imei);
  const [imeiPrev, setImeiPrev] = useState(order.imeiPrev || "");

  const [optionChecked, setOptionChecked] = useState<Record<string, boolean>>(
    Object.fromEntries(order.options.map((o) => [o.optionType, true]))
  );
  const [optionComment, setOptionComment] = useState<Record<string, string>>(
    Object.fromEntries(order.options.map((o) => [o.optionType, o.comment || ""]))
  );

  const [comment, setComment] = useState(order.comment || "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function toggleInstaller(key: string) {
    setInstallerChecked((prev) => ({ ...prev, [key]: !prev[key] }));
  }

  function toggleOption(key: string) {
    setOptionChecked((prev) => ({ ...prev, [key]: !prev[key] }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const installers = INSTALLER_KEYS.filter((k) => installerChecked[k]).map((k) => ({
      name: k,
      otherText: k === "OSTALO" ? installerOtherText.trim() : undefined,
    }));
    if (installers.length === 0) return setError("Izberite vsaj enega monterja.");

    if (!/^\d{10}$/.test(imei)) return setError("IMEI mora vsebovati natanko 10 številk.");
    if (type === "INTERVENCIJA" && !/^\d{10}$/.test(imeiPrev)) {
      return setError("IMEI prej mora vsebovati natanko 10 številk.");
    }

    const options = OPTION_KEYS.filter((k) => optionChecked[k]).map((k) => ({
      optionType: k,
      comment: optionComment[k]?.trim() || undefined,
    }));

    setSaving(true);
    try {
      const res = await fetch(`/api/work-orders/${order.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          type,
          difficulty,
          clientId,
          installers,
          vehiclePlate,
          vehicleBrand,
          vehicleModel,
          vehicleYear,
          imei,
          imeiPrev: type === "INTERVENCIJA" ? imeiPrev : undefined,
          options,
          comment: comment.trim() || undefined,
        }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error || "Shranjevanje ni uspelo.");
        return;
      }
      router.push(`/nalogi/${order.id}`);
      router.refresh();
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-lg mx-auto p-4 space-y-6 pb-24">
      <h1 className="text-lg font-semibold">Urejanje naloga</h1>

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
        <label className="block text-sm font-medium mb-1">Komentar</label>
        <textarea
          className="w-full rounded-lg border border-gray-300 px-3 py-2"
          rows={3}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
        />
      </section>

      <p className="text-xs text-gray-500">
        Opomba: slik in podpisa stranke ni mogoče spremeniti po shranitvi naloga.
      </p>

      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4">
        <button
          type="submit"
          disabled={saving}
          className="max-w-lg mx-auto block w-full bg-gray-900 text-white rounded-lg py-3 font-medium disabled:opacity-50"
        >
          {saving ? "Shranjujem ..." : "Shrani spremembe"}
        </button>
      </div>
    </form>
  );
}
