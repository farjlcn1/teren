"use client";

import { useEffect, useState } from "react";
import { PERMISSION_DEFINITIONS, PermissionKey } from "@/lib/constants";

type UserRow = {
  id: string;
  email: string;
  fullName: string;
  isActive: boolean;
} & Record<PermissionKey, boolean>;

const emptyPermissions = Object.fromEntries(PERMISSION_DEFINITIONS.map((p) => [p.key, false])) as Record<
  PermissionKey,
  boolean
>;

export default function UsersAdmin() {
  const [users, setUsers] = useState<UserRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [showNew, setShowNew] = useState(false);
  const [newEmail, setNewEmail] = useState("");
  const [newFullName, setNewFullName] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [newPermissions, setNewPermissions] = useState<Record<PermissionKey, boolean>>(emptyPermissions);
  const [creating, setCreating] = useState(false);

  function loadUsers() {
    setLoading(true);
    fetch("/api/users")
      .then((r) => r.json())
      .then(setUsers)
      .catch(() => setError("Napaka pri nalaganju uporabnikov."))
      .finally(() => setLoading(false));
  }

  useEffect(loadUsers, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setCreating(true);
    try {
      const res = await fetch("/api/users", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: newEmail,
          password: newPassword,
          fullName: newFullName,
          ...newPermissions,
        }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error || "Ustvarjanje uporabnika ni uspelo.");
        return;
      }
      setNewEmail("");
      setNewFullName("");
      setNewPassword("");
      setNewPermissions(emptyPermissions);
      setShowNew(false);
      loadUsers();
    } finally {
      setCreating(false);
    }
  }

  async function updateUser(id: string, patch: Record<string, unknown>) {
    setError(null);
    const res = await fetch(`/api/users/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(patch),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      setError(data.error || "Posodobitev ni uspela.");
      return;
    }
    loadUsers();
  }

  return (
    <div className="space-y-4">
      {error && <p className="text-sm text-red-600 bg-red-50 rounded p-2">{error}</p>}

      <div className="flex justify-between items-center">
        <h1 className="text-lg font-semibold">Uporabniki</h1>
        <button onClick={() => setShowNew((s) => !s)} className="bg-gray-900 text-white rounded-lg px-3 py-1.5 text-sm">
          {showNew ? "Prekliči" : "+ Nov uporabnik"}
        </button>
      </div>

      {showNew && (
        <form onSubmit={handleCreate} className="bg-white border border-gray-200 rounded-lg p-4 space-y-3">
          <div className="grid grid-cols-3 gap-3">
            <input
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
              placeholder="Ime in priimek"
              value={newFullName}
              onChange={(e) => setNewFullName(e.target.value)}
              required
            />
            <input
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
              placeholder="E-mail"
              type="email"
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              required
            />
            <input
              className="border border-gray-300 rounded-lg px-3 py-2 text-sm"
              placeholder="Geslo (8+, velika, št., poseben znak)"
              type="text"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
            {PERMISSION_DEFINITIONS.map((p) => (
              <label key={p.key} className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={newPermissions[p.key]}
                  onChange={(e) => setNewPermissions((prev) => ({ ...prev, [p.key]: e.target.checked }))}
                />
                {p.label}
              </label>
            ))}
          </div>

          <button
            type="submit"
            disabled={creating}
            className="bg-gray-900 text-white rounded-lg px-4 py-1.5 text-sm disabled:opacity-50"
          >
            {creating ? "Ustvarjam ..." : "Ustvari uporabnika"}
          </button>
        </form>
      )}

      {!loading && (
        <div className="bg-white border border-gray-200 rounded-lg overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 text-left">
                <th className="px-3 py-2">Ime</th>
                <th className="px-3 py-2">E-mail</th>
                {PERMISSION_DEFINITIONS.map((p) => (
                  <th key={p.key} className="px-3 py-2 text-center">
                    {p.label}
                  </th>
                ))}
                <th className="px-3 py-2 text-center">Aktiven</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-b border-gray-100">
                  <td className="px-3 py-2 whitespace-nowrap">{u.fullName}</td>
                  <td className="px-3 py-2 whitespace-nowrap">{u.email}</td>
                  {PERMISSION_DEFINITIONS.map((p) => (
                    <td key={p.key} className="px-3 py-2 text-center">
                      <input
                        type="checkbox"
                        checked={u[p.key]}
                        onChange={(e) => updateUser(u.id, { [p.key]: e.target.checked })}
                      />
                    </td>
                  ))}
                  <td className="px-3 py-2 text-center">
                    <input
                      type="checkbox"
                      checked={u.isActive}
                      onChange={(e) => updateUser(u.id, { isActive: e.target.checked })}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
