"use client";

import { useState, useTransition } from "react";
import { updateWorkOrder } from "./actions";
import { ClientCombobox } from "@/components/client-combobox";
import { PlateCombobox } from "@/components/plate-combobox";

type Client = { id: string; name: string };

const TYPES = [
  { value: "MONTAZA", label: "Montaža" },
  { value: "DEMONTAZA", label: "Demontaža" },
  { value: "INTERVENCIJA", label: "Intervencija" },
  { value: "PREMONTAZA", label: "Premontaža" },
  { value: "OSTALO", label: "Ostalo" },
];

const DIFFICULTIES = [
  { value: "OSNOVNA", label: "Osnovna" },
  { value: "ZAHTEVNA", label: "Zahtevna" },
];

const INSTALLERS = ["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"];

const OPTION_TYPES = [
  "DIN1",
  "DIN2",
  "DIN3",
  "DIN4",
  "DIN5",
  "ANI1",
  "ANI2",
  "ANI3",
  "ALL_CAN",
  "FMSCAN",
  "TACHO",
];

const OPTION_LABELS: Record<string, string> = { ALL_CAN: "ALL CAN" };

function fieldClass() {
  return "mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100";
}

export function EditWorkOrderForm({
  id,
  clients,
  vehiclePlates,
  initial,
}: {
  id: string;
  clients: Client[];
  vehiclePlates: string[];
  initial: {
    type: string;
    difficulty: string;
    clientId: string;
    vehiclePlate: string;
    vehicleBrand: string;
    vehicleModel: string;
    vehicleYear: string;
    imei: string;
    imeiPrev: string;
    comment: string;
    installers: { name: string; otherText: string | null }[];
    options: { optionType: string; comment: string | null }[];
  };
}) {
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  const [type, setType] = useState(initial.type);
  const [difficulty, setDifficulty] = useState(initial.difficulty);
  const [clientId, setClientId] = useState(initial.clientId);
  const [vehiclePlate, setVehiclePlate] = useState(initial.vehiclePlate);
  const [vehicleBrand, setVehicleBrand] = useState(initial.vehicleBrand);
  const [vehicleModel, setVehicleModel] = useState(initial.vehicleModel);
  const [vehicleYear, setVehicleYear] = useState(initial.vehicleYear);
  const [imei, setImei] = useState(initial.imei);
  const [imeiPrev, setImeiPrev] = useState(initial.imeiPrev);
  const [comment, setComment] = useState(initial.comment);

  const [installers, setInstallers] = useState<Record<string, boolean>>(
    Object.fromEntries(initial.installers.map((i) => [i.name, true]))
  );
  const [installerOtherText, setInstallerOtherText] = useState(
    initial.installers.find((i) => i.name === "OSTALO")?.otherText ?? ""
  );

  const [optionState, setOptionState] = useState<Record<string, { checked: boolean; comment: string }>>(
    Object.fromEntries(initial.options.map((o) => [o.optionType, { checked: true, comment: o.comment ?? "" }]))
  );

  function toggleInstaller(name: string) {
    setInstallers((prev) => ({ ...prev, [name]: !prev[name] }));
  }

  function toggleOption(opt: string) {
    setOptionState((prev) => ({
      ...prev,
      [opt]: { checked: !prev[opt]?.checked, comment: prev[opt]?.comment ?? "" },
    }));
  }

  function setOptionComment(opt: string, value: string) {
    setOptionState((prev) => ({ ...prev, [opt]: { checked: prev[opt]?.checked ?? true, comment: value } }));
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const selectedInstallers = Object.entries(installers)
      .filter(([, checked]) => checked)
      .map(([name]) => ({ name, otherText: name === "OSTALO" ? installerOtherText : undefined }));

    if (selectedInstallers.length === 0) return setError("Izberi vsaj enega monterja.");

    const selectedOptions = Object.entries(optionState)
      .filter(([, v]) => v.checked)
      .map(([optionType, v]) => ({ optionType, comment: v.comment || undefined }));

    const fd = new FormData();
    fd.set("type", type);
    fd.set("difficulty", difficulty);
    fd.set("clientId", clientId);
    fd.set("vehiclePlate", vehiclePlate);
    fd.set("vehicleBrand", vehicleBrand);
    fd.set("vehicleModel", vehicleModel);
    fd.set("vehicleYear", vehicleYear);
    fd.set("imei", imei);
    fd.set("imeiPrev", type === "INTERVENCIJA" ? imeiPrev : "");
    fd.set("comment", comment);
    fd.set("installers", JSON.stringify(selectedInstallers));
    fd.set("options", JSON.stringify(selectedOptions));

    startTransition(async () => {
      const result = await updateWorkOrder(id, undefined, fd);
      if (result?.error) setError(result.error);
    });
  }

  return (
    <form onSubmit={handleSubmit} className="max-w-xl space-y-5">
      <section className="grid grid-cols-2 gap-3">
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Tip naloga
          <select value={type} onChange={(e) => setType(e.target.value)} className={fieldClass()}>
            {TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </label>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Zahtevnost
          <select value={difficulty} onChange={(e) => setDifficulty(e.target.value)} className={fieldClass()}>
            {DIFFICULTIES.map((d) => (
              <option key={d.value} value={d.value}>
                {d.label}
              </option>
            ))}
          </select>
        </label>
      </section>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Stranka</label>
        <ClientCombobox clients={clients} value={clientId} onChange={setClientId} required />
      </div>

      <fieldset>
        <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">Kdo je montiral</legend>
        <div className="mt-2 grid grid-cols-3 gap-2">
          {INSTALLERS.map((name) => (
            <label
              key={name}
              className="flex items-center gap-2 rounded-md border border-gray-200 px-3 py-2 text-sm text-gray-900 dark:border-gray-700 dark:text-gray-100"
            >
              <input type="checkbox" checked={!!installers[name]} onChange={() => toggleInstaller(name)} />
              {name === "OSTALO" ? "Ostalo" : name.charAt(0) + name.slice(1).toLowerCase()}
            </label>
          ))}
        </div>
        {installers.OSTALO && (
          <input
            value={installerOtherText}
            onChange={(e) => setInstallerOtherText(e.target.value)}
            placeholder="Ime monterja"
            className={fieldClass()}
          />
        )}
      </fieldset>

      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Registrska št. vozila</label>
        <PlateCombobox plates={vehiclePlates} value={vehiclePlate} onChange={setVehiclePlate} className={`${fieldClass()} mt-0`} />
      </div>

      <section className="grid grid-cols-3 gap-3">
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Znamka
          <input value={vehicleBrand} onChange={(e) => setVehicleBrand(e.target.value)} className={fieldClass()} />
        </label>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Model
          <input value={vehicleModel} onChange={(e) => setVehicleModel(e.target.value)} className={fieldClass()} />
        </label>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Letnik
          <input value={vehicleYear} onChange={(e) => setVehicleYear(e.target.value)} className={fieldClass()} />
        </label>
      </section>

      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
        Št. IMEI (zadnjih 10 mest)
        <input
          value={imei}
          onChange={(e) => setImei(e.target.value.replace(/\D/g, "").slice(0, 10))}
          className={fieldClass()}
        />
      </label>

      {type === "INTERVENCIJA" && (
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          Št. IMEI prej (zadnjih 10 mest)
          <input
            value={imeiPrev}
            onChange={(e) => setImeiPrev(e.target.value.replace(/\D/g, "").slice(0, 10))}
            className={fieldClass()}
          />
        </label>
      )}

      <fieldset>
        <legend className="text-sm font-medium text-gray-700 dark:text-gray-300">DIN / ANI / CAN</legend>
        <div className="mt-2 space-y-2">
          {OPTION_TYPES.map((opt) => (
            <div key={opt} className="rounded-md border border-gray-200 p-2 dark:border-gray-700">
              <label className="flex items-center gap-2 text-sm text-gray-900 dark:text-gray-100">
                <input type="checkbox" checked={!!optionState[opt]?.checked} onChange={() => toggleOption(opt)} />
                {OPTION_LABELS[opt] ?? opt}
              </label>
              {optionState[opt]?.checked && (
                <input
                  value={optionState[opt]?.comment ?? ""}
                  onChange={(e) => setOptionComment(opt, e.target.value)}
                  placeholder="Komentar (neobvezno)"
                  className="mt-2 w-full rounded-md border border-gray-300 bg-white px-2 py-1 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100"
                />
              )}
            </div>
          ))}
        </div>
      </fieldset>

      <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
        Komentar
        <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={3} className={fieldClass()} />
      </label>

      {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}

      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {pending ? "Shranjujem …" : "Shrani spremembe"}
      </button>
    </form>
  );
}
