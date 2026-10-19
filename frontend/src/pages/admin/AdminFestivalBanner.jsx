
import { useEffect, useState } from "react";
import api from "../../api/client.js";

export default function AdminFestivalBanner() {
  const [message, setMessage] = useState("");
  const [enabled, setEnabled] = useState(false);
  const [backgroundColor, setBackgroundColor] = useState("#7f1d1d");
  const [scrolling, setScrolling] = useState(true);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/home/festival-banner")
      .then(({ data }) => {
        setMessage(data.message || "");
        setEnabled(Boolean(data.enabled));
        setBackgroundColor(data.backgroundColor || "#7f1d1d");
        setScrolling(Boolean(data.scrolling));
      })
      .catch(() => {
        setError("Unable to load banner settings. Please refresh the page.");
      })
      .finally(() => setLoading(false));
  }, []);

  async function handleSave(event) {
    event.preventDefault();
    setSaving(true);
    setNotice("");
    setError("");

    try {
      const { data } = await api.put("/admin/festival-banner", {
        message,
        enabled,
        backgroundColor,
        scrolling,
      });

      setMessage(data.message || "");
      setEnabled(Boolean(data.enabled));
      setBackgroundColor(data.backgroundColor || "#7f1d1d");
      setScrolling(Boolean(data.scrolling));
      setNotice("Festival banner settings saved successfully.");
    } catch {
      setError(
        "Unable to save settings. The admin save API may not be configured yet."
      );
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="p-6">Loading festival banner settings...</div>;
  }

  return (
    <div className="max-w-3xl">
      <h1 className="text-2xl font-bold text-gray-800">
        Festival Banner
      </h1>
      <p className="mt-2 text-gray-600">
        Manage the festival announcement displayed on your homepage.
      </p>

      <form
        onSubmit={handleSave}
        className="mt-6 space-y-5 rounded-lg border bg-white p-6 shadow-sm"
      >
        <div>
          <label className="mb-2 block font-medium text-gray-700">
            Banner Message
          </label>
          <textarea
            value={message}
            onChange={(event) => setMessage(event.target.value)}
            maxLength={500}
            rows={3}
            placeholder="Example: Happy Diwali! Order our special sweets today."
            className="w-full rounded-md border border-gray-300 p-3"
          />
          <p className="mt-1 text-sm text-gray-500">
            Maximum 500 characters.
          </p>
        </div>

        <label className="flex items-center gap-3">
          <input
            type="checkbox"
            checked={enabled}
            onChange={(event) => setEnabled(event.target.checked)}
          />
          <span className="font-medium text-gray-700">
            Enable festival banner
          </span>
        </label>

        <div>
          <label className="mb-2 block font-medium text-gray-700">
            Background Color
          </label>
          <div className="flex items-center gap-3">
            <input
              type="color"
              value={backgroundColor}
              onChange={(event) => setBackgroundColor(event.target.value)}
              className="h-11 w-16 cursor-pointer"
            />
            <span className="text-gray-600">{backgroundColor}</span>
          </div>
        </div>

        <label className="flex items-center gap-3">
          <input
            type="checkbox"
            checked={scrolling}
            onChange={(event) => setScrolling(event.target.checked)}
          />
          <span className="font-medium text-gray-700">
            Scroll banner message
          </span>
        </label>

        {notice && (
          <p className="rounded-md bg-green-50 p-3 text-green-700">
            {notice}
          </p>
        )}

        {error && (
          <p className="rounded-md bg-red-50 p-3 text-red-700">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={saving}
          className="rounded-md bg-red-900 px-5 py-3 font-semibold text-white disabled:opacity-60"
        >
          {saving ? "Saving..." : "Save Settings"}
        </button>
      </form>
    </div>
  );
}
