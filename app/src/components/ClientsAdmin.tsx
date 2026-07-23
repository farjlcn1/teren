"use client";

import { useEffect, useState } from "react";

type ClientRow = { id: string; name: string; address: string | null; contact: string | null };

export default function ClientsAdmin() {
  const [clients, setClients] = useState<ClientRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [contact, setContact] = useState("");
  const [saving, setSaving] = useState(false);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState("");
  const [editAddress, setEditAddress] = useState("");
  const [editContact, setEditContact] = useState("");

  function loadClients() {
    setLoading(true);
    fetch("/api/clients")
      .then((r) => r.json())
      .then(setClients)
      .catch(() => setError("Napaka pri nalaganju strank."))
      .finally(() => setLoading(false));
  }

  useEffect(loadClients, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const res = await fetch("/api/clients", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, address: address || undefined, contact: contact || undefined }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setError(data.error || "Dodajanje stranke ni uspelo.");
        return;
      }
      setName("");
      setAddress("");
      setContact("");
      loadClients();
    } finally {
      setSaving(false);
    }
  }

  function startEdit(c: ClientRow) {
    setEditingId(c.id);
    setEditName(c.name);
    setEditAddress(c.address || "");
    setEditContact(c.contact || "");
  }

  async function saveEdit(id: string) {
    setError(null);
    const res = await fetch(`/api/clients/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: editName, address: editAddress || undefined, contact: editContact || undefined }),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      setError(data.error || "Posodobitev ni uspela.");
      return;
    }
    setEditingId(null);
    loadClients();
  }

  return (
    <div className="space-y-4 max-w-3xl">
      <h1 className="text-lg font-semibold">Stranke</h1>

      {error && <p className="text-sm text-red-600 bg-red-50 rounded p-2">{error}</p>}

      <form onSubmit={handleCreate} className="bg-white border border-gray-200 rounded-lg p-4 flex gap-3 items-end">
        <div className="flex-1">
          <label className="block text-xs text-gray-500 mb-1">Ime stranke</label>
          <input
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>
        <div className="flex-1">
          <label className="block text-xs text-gray-500 mb-1">Naslov</label>
          <input
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
          />
        </div>
        <div className="flex-1">
          <label className="block text-xs text-gray-500 mb-1">Kontakt</label>
          <input
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
            value={contact}
            onChange={(e) => setContact(e.target.value)}
          />
        </div>
        <button disabled={saving} className="bg-gray-900 text-white rounded-lg px-4 py-2 text-sm disabled:opacity-50">
          Dodaj
        </button>
      </form>

      {!loading && (
        <div className="bg-white border border-gray-200 rounded-lg overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 text-left">
                <th className="px-3 py-2">Ime</th>
                <th className="px-3 py-2">Naslov</th>
                <th className="px-3 py-2">Kontakt</th>
                <th className="px-3 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {clients.map((c) => (
                <tr key={c.id} className="border-b border-gray-100">
                  {editingId === c.id ? (
                    <>
                      <td className="px-3 py-2">
                        <input
                          className="border border-gray-300 rounded px-2 py-1 text-sm w-full"
                          value={editName}
                          onChange={(e) => setEditName(e.target.value)}
                        />
                      </td>
                      <td className="px-3 py-2">
                        <input
                          className="border border-gray-300 rounded px-2 py-1 text-sm w-full"
                          value={editAddress}
                          onChange={(e) => setEditAddress(e.target.value)}
                        />
                      </td>
                      <td className="px-3 py-2">
                        <input
                          className="border border-gray-300 rounded px-2 py-1 text-sm w-full"
                          value={editContact}
                          onChange={(e) => setEditContact(e.target.value)}
                        />
                      </td>
                      <td className="px-3 py-2 whitespace-nowrap">
                        <button onClick={() => saveEdit(c.id)} className="text-sm text-blue-600 mr-2">
                          Shrani
                        </button>
                        <button onClick={() => setEditingId(null)} className="text-sm text-gray-500">
                          Prekliči
                        </button>
                      </td>
                    </>
                  ) : (
                    <>
                      <td className="px-3 py-2">{c.name}</td>
                      <td className="px-3 py-2">{c.address}</td>
                      <td className="px-3 py-2">{c.contact}</td>
                      <td className="px-3 py-2">
                        <button onClick={() => startEdit(c)} className="text-sm text-blue-600">
                          Uredi
                        </button>
                      </td>
                    </>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
