"use client";

import { useEffect, useState, useTransition, useActionState } from "react";
import Link from "next/link";
import { ClientCombobox } from "@/components/client-combobox";
import { PlateCombobox } from "@/components/plate-combobox";
import { DateTimeInput } from "@/components/date-input";
import { updatePlanGroup, deletePlanGroup, addPlannedTask, deletePlannedTask } from "./actions";
import { isoToLocalDateTimeStr, localDateTimeToIso, type PlanGroupItem } from "./plan-calendar";

const INSTALLERS = ["SIMON", "VITO", "SERGEJ", "GREGOR", "KLEMEN", "OSTALO"];

function installerLabel(name: string) {
  return name === "OSTALO" ? "Ostalo" : name.charAt(0) + name.slice(1).toLowerCase();
}

function fieldClass() {
  return "mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 dark:border-gray-600 dark:bg-gray-800 dark:text-gray-100";
}

function fmtDateTime(iso: string) {
  return new Date(iso).toLocaleString("sl-SI", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function GroupPopup({
  group,
  clients,
  vehiclePlates,
  canManagePlan,
  onClose,
}: {
  group: PlanGroupItem;
  clients: { id: string; name: string }[];
  vehiclePlates: string[];
  canManagePlan: boolean;
  onClose: () => void;
}) {
  const [editMode, setEditMode] = useState(false);
  const [editClientId, setEditClientId] = useState(group.clientId);
  const [editStartAtIso, setEditStartAtIso] = useState(group.startAt);
  const [editEndAtIso, setEditEndAtIso] = useState(group.endAt);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deletingTaskId, setDeletingTaskId] = useState<string | null>(null);
  const [, startTransition] = useTransition();

  const [newInstaller, setNewInstaller] = useState("");
  const [taskFormKey, setTaskFormKey] = useState(0);

  const boundUpdate = updatePlanGroup.bind(null, group.id);
  const [updateState, updateFormAction, updatePending] = useActionState(boundUpdate, undefined);

  const boundAddTask = addPlannedTask.bind(null, group.id);
  const [addState, addFormAction, addPending] = useActionState(boundAddTask, undefined);

  const hasCompletedTask = group.tasks.some((t) => t.workOrderId);

  useEffect(() => {
    if (updateState?.success) setEditMode(false);
  }, [updateState]);

  // Ob uspešnem dodajanju naloga izprazni obrazec za naslednji vnos -- PlateCombobox/textarea/otherText
  // so nekontrolirani/lokalni, zato jih prisilimo v remount s spremembo key-ja namesto ročnega resetiranja.
  useEffect(() => {
    if (addState?.success) {
      setNewInstaller("");
      setTaskFormKey((k) => k + 1);
    }
  }, [addState]);

  function openEditMode() {
    setEditClientId(group.clientId);
    setEditStartAtIso(group.startAt);
    setEditEndAtIso(group.endAt);
    setEditMode(true);
  }

  function handleDeleteGroup() {
    if (!window.confirm("Izbriši ta dogodek?")) return;
    startTransition(async () => {
      const result = await deletePlanGroup(group.id);
      if (result?.error) {
        setDeleteError(result.error);
        return;
      }
      onClose();
    });
  }

  function handleDeleteTask(taskId: string) {
    if (!window.confirm("Izbriši ta nalog?")) return;
    setDeletingTaskId(taskId);
    startTransition(async () => {
      const result = await deletePlannedTask(taskId);
      if (result?.error) setDeleteError(result.error);
      setDeletingTaskId(null);
    });
  }

  return (
    <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/30" onClick={onClose}>
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-lg space-y-4 rounded-md border border-gray-200 bg-white p-4 shadow-lg dark:border-gray-700 dark:bg-gray-900"
      >
        {editMode ? (
          <form action={updateFormAction} className="space-y-3">
            <h3 className="text-sm font-medium text-gray-900 dark:text-gray-100">Uredi dogodek</h3>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">Stranka</label>
              {hasCompletedTask ? (
                <div className="mt-1 rounded-md border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-700 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300">
                  {group.clientName} (ni mogoče spremeniti, ker je nalog že opravljen)
                </div>
              ) : (
                <ClientCombobox clients={clients} value={editClientId} onChange={setEditClientId} required />
              )}
              <input type="hidden" name="clientId" value={editClientId} />
            </div>
            <input type="hidden" name="startAt" value={editStartAtIso} />
            <input type="hidden" name="endAt" value={editEndAtIso} />
            <div className="grid grid-cols-2 gap-3">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Od
                <DateTimeInput
                  withTime
                  required
                  defaultValue={isoToLocalDateTimeStr(group.startAt)}
                  onValueChange={(v) => setEditStartAtIso(localDateTimeToIso(v))}
                />
              </label>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                Do
                <DateTimeInput
                  withTime
                  required
                  defaultValue={isoToLocalDateTimeStr(group.endAt)}
                  onValueChange={(v) => setEditEndAtIso(localDateTimeToIso(v))}
                />
              </label>
            </div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
              Opomba (neobvezno)
              <textarea name="note" rows={2} defaultValue={group.note ?? ""} className={fieldClass()} />
            </label>
            {updateState?.error && <p className="text-sm text-red-600 dark:text-red-400">{updateState.error}</p>}
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setEditMode(false)}
                className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 dark:border-gray-600 dark:text-gray-300"
              >
                Prekliči
              </button>
              <button
                type="submit"
                disabled={updatePending}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
              >
                {updatePending ? "Shranjujem …" : "Shrani"}
              </button>
            </div>
          </form>
        ) : (
          <>
            <div className="flex items-start justify-between gap-2">
              <div>
                <h3 className="text-sm font-medium text-gray-900 dark:text-gray-100">{group.clientName}</h3>
                <div className="mt-1 space-y-0.5 text-xs text-gray-600 dark:text-gray-400">
                  <div>Od: {fmtDateTime(group.startAt)}</div>
                  <div>Do: {fmtDateTime(group.endAt)}</div>
                  {group.note && <div>Opomba: {group.note}</div>}
                </div>
              </div>
              {canManagePlan && (
                <div className="flex shrink-0 gap-2">
                  <button
                    type="button"
                    onClick={openEditMode}
                    className="rounded-md border border-gray-300 px-2.5 py-1 text-xs text-gray-700 dark:border-gray-600 dark:text-gray-300"
                  >
                    Uredi
                  </button>
                  {!hasCompletedTask && (
                    <button
                      type="button"
                      onClick={handleDeleteGroup}
                      className="rounded-md border border-red-300 px-2.5 py-1 text-xs text-red-600 dark:border-red-800 dark:text-red-400"
                    >
                      Izbriši
                    </button>
                  )}
                </div>
              )}
            </div>

            {deleteError && <p className="text-sm text-red-600 dark:text-red-400">{deleteError}</p>}

            <div className="space-y-2 border-t border-gray-200 pt-3 dark:border-gray-700">
              <h4 className="text-xs font-semibold uppercase text-gray-500 dark:text-gray-400">Nalogi</h4>
              {group.tasks.length === 0 && (
                <p className="text-sm text-gray-500 dark:text-gray-400">Ta dogodek še nima nalogov.</p>
              )}
              <ul className="space-y-1.5">
                {group.tasks.map((t) => (
                  <li
                    key={t.id}
                    className="flex items-center justify-between gap-2 rounded-md border border-gray-200 px-2.5 py-2 text-sm dark:border-gray-700"
                  >
                    <div className="min-w-0">
                      <div className="truncate font-medium text-gray-900 dark:text-gray-100">{t.vehiclePlate}</div>
                      {t.note && <div className="truncate text-xs text-gray-500 dark:text-gray-400">{t.note}</div>}
                      {t.expectedInstaller && (
                        <div className="truncate text-xs text-gray-500 dark:text-gray-400">
                          Pričakovan monter:{" "}
                          {t.expectedInstaller === "OSTALO"
                            ? t.expectedInstallerOtherText || "Ostalo"
                            : installerLabel(t.expectedInstaller)}
                        </div>
                      )}
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      {t.workOrderId ? (
                        <Link
                          href={`/nalogi/${t.workOrderId}`}
                          className="rounded-md bg-green-100 px-2.5 py-1 text-xs font-medium text-green-800 dark:bg-green-900/40 dark:text-green-300"
                        >
                          ✓ Opravljeno
                        </Link>
                      ) : (
                        <Link
                          href={`/nalogi/nov?planTaskId=${t.id}`}
                          className="rounded-md bg-blue-600 px-2.5 py-1 text-xs font-medium text-white"
                        >
                          Izpolni
                        </Link>
                      )}
                      {canManagePlan && !t.workOrderId && (
                        <button
                          type="button"
                          onClick={() => handleDeleteTask(t.id)}
                          disabled={deletingTaskId === t.id}
                          className="rounded-md border border-red-300 px-2 py-1 text-xs text-red-600 disabled:opacity-50 dark:border-red-800 dark:text-red-400"
                        >
                          ×
                        </button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </div>

            {canManagePlan && (
              <form action={addFormAction} className="space-y-2 border-t border-gray-200 pt-3 dark:border-gray-700">
                <h4 className="text-xs font-semibold uppercase text-gray-500 dark:text-gray-400">Dodaj nalog</h4>
                <div key={taskFormKey} className="space-y-2">
                  <PlateComboboxField vehiclePlates={vehiclePlates} />
                  <textarea name="note" rows={1} placeholder="Opomba (neobvezno)" className={fieldClass()} />
                  <div className="flex flex-wrap items-center gap-2">
                    <select
                      name="expectedInstaller"
                      value={newInstaller}
                      onChange={(e) => setNewInstaller(e.target.value)}
                      className={`${fieldClass()} mt-0 w-auto`}
                    >
                      <option value="">— brez pričakovanega monterja —</option>
                      {INSTALLERS.map((name) => (
                        <option key={name} value={name}>
                          {installerLabel(name)}
                        </option>
                      ))}
                    </select>
                    {newInstaller === "OSTALO" && (
                      <input
                        name="expectedInstallerOtherText"
                        placeholder="Ime monterja"
                        className={`${fieldClass()} mt-0 flex-1`}
                      />
                    )}
                  </div>
                </div>
                {addState?.error && <p className="text-sm text-red-600 dark:text-red-400">{addState.error}</p>}
                <div className="flex justify-end">
                  <button
                    type="submit"
                    disabled={addPending}
                    className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-50"
                  >
                    {addPending ? "Dodajam …" : "Dodaj nalog"}
                  </button>
                </div>
              </form>
            )}

            <div className="flex justify-end border-t border-gray-200 pt-3 dark:border-gray-700">
              <button
                type="button"
                onClick={onClose}
                className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white"
              >
                Zapri
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// Ločena majhna komponenta samo zato, ker PlateCombobox nima lastnega name="" atributa (kontrolirana
// komponenta) -- lokalni useState + skriti input priklopita njeno vrednost na FormData obrazca nad njo.
function PlateComboboxField({ vehiclePlates }: { vehiclePlates: string[] }) {
  const [value, setValue] = useState("");
  return (
    <div>
      <PlateCombobox plates={vehiclePlates} value={value} onChange={setValue} className={fieldClass()} />
      <input type="hidden" name="vehiclePlate" value={value} />
    </div>
  );
}
