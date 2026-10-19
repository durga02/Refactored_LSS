import { useEffect, useRef, useState } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { trackOrder, cancelOrder } from "../api/orders.js";
import { useToast } from "../context/ToastContext.jsx";

const STEPS = [
  "Placed", "Confirmed", "Preparing", "Packed", "Shipped", "Out for Delivery", "Delivered",
];

const CANCEL_REASONS = [
  "Ordered by mistake", "Wrong address", "Delay in delivery", "I Changed My Mind", "Others",
];

const STATUS_COLORS = {
  DELIVERED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-700",
  SHIPPED: "bg-blue-100 text-blue-700",
  OUT_FOR_DELIVERY: "bg-purple-100 text-purple-700",
  PLACED: "bg-amber-100 text-amber-700",
  CONFIRMED: "bg-amber-100 text-amber-700",
  PREPARING: "bg-amber-100 text-amber-700",
  PACKED: "bg-blue-100 text-blue-700",
};

export default function TrackOrder() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const { showToast } = useToast();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [reason, setReason] = useState("");
  const [cancelling, setCancelling] = useState(false);
  const intervalRef = useRef(null);

  const load = (signal) => {
    trackOrder(id, signal ? { signal } : undefined)
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    intervalRef.current = setInterval(() => load(), 10000);
    return () => {
      controller.abort();
      clearInterval(intervalRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleCancel = async (e) => {
    e.preventDefault();
    if (!reason) return;
    setCancelling(true);
    try {
      await cancelOrder(id, reason);
      showToast("Order cancelled successfully", "success");
      load();
    } catch {
      showToast("Failed to cancel order", "error");
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return <p className="max-w-xl mx-auto my-20 text-center text-gray-500">Loading order status...</p>;
  }

  if (!data) {
    return <p className="max-w-xl mx-auto my-20 text-center text-red-600">Order not found.</p>;
  }

  const { order, step, estimatedDelivery, history } = data;
  const canCancel = ["PLACED", "CONFIRMED", "PREPARING"].includes(order.status);
  const statusClass = STATUS_COLORS[order.status] || "bg-gray-100 text-gray-700";

  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <h2 className="text-2xl font-bold mb-4">📦 Track Your Order</h2>

      {searchParams.get("cancelled") && (
        <div className="bg-red-600 text-white text-center py-3 px-4 rounded-xl mb-5 font-medium">
          ❌ Order Cancelled Successfully
        </div>
      )}

      {/* Status + info */}
      <div className="bg-white rounded-2xl shadow-sm p-5 mb-6">
        <div className="flex items-center justify-between flex-wrap gap-3 mb-4">
          <p className="font-semibold">
            Current Status:{" "}
            <span className={`inline-block px-3 py-1 rounded-full text-sm font-bold ${statusClass}`}>
              {order.status}
            </span>
          </p>
          {estimatedDelivery && (
            <p className="text-sm text-gray-500">
              Estimated Delivery:{" "}
              <strong>{new Date(estimatedDelivery).toLocaleDateString()}</strong>
            </p>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
          {[
            ["Order ID", order.id],
            ["Name", order.customerNameSnapshot],
            ["Order Date", order.orderDate ? new Date(order.orderDate).toLocaleString() : "-"],
            [
              "Delivery Charges", 
              "To be Confirmed",
            ],
            ["Total Amount", `₹ ${order.totalAmount}`],
          ].map(([label, value]) => (
            <div key={label}>
              <p className="text-gray-500 text-xs">{label}</p>
              <p className="font-semibold">{value}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Stepper */}
      {order.status !== "CANCELLED" && (
        <div className="flex items-start justify-between overflow-x-auto pb-2 mb-6 gap-1">
          {STEPS.map((label, index) => {
            const active = step >= index + 1;
            return (
              <div key={label} className="flex flex-col items-center flex-1 min-w-[60px]">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold border-2 transition-colors ${
                    active
                      ? "bg-green-500 border-green-500 text-white"
                      : "bg-white border-gray-300 text-gray-400"
                  }`}
                >
                  {index + 1}
                </div>
                <p className="text-[10px] text-center mt-1 leading-tight text-gray-600">{label}</p>
              </div>
            );
          })}
        </div>
      )}

      {order.status === "DELIVERED" && (
        <div className="bg-green-50 border border-green-200 text-green-700 text-center font-semibold py-3 rounded-xl mb-6">
          🎉 Your Order Has Been Delivered Successfully!
        </div>
      )}

      {/* Status History */}
      <div className="bg-white rounded-2xl shadow-sm p-5 mb-6">
        <h3 className="font-bold text-base mb-4">Status History</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100">
                <th className="text-left py-2 pr-4 text-gray-500 font-medium">Status</th>
                <th className="text-left py-2 text-gray-500 font-medium">Time</th>
              </tr>
            </thead>
            <tbody>
              {history.map((h, i) => (
                <tr key={i} className="border-b border-gray-50 last:border-0">
                  <td className="py-2 pr-4 font-medium">{h.status}</td>
                  <td className="py-2 text-gray-500">
                    {h.updatedTime ? new Date(h.updatedTime).toLocaleString() : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Cancel form */}
      {canCancel && (
        <div className="bg-white rounded-2xl shadow-sm p-5">
          <h3 className="font-bold text-base mb-4">Cancel Order</h3>
          <form onSubmit={handleCancel} className="flex flex-col sm:flex-row gap-3">
            <select
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required
              className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-red-400 transition"
            >
              <option value="">Select reason</option>
              {CANCEL_REASONS.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
            <button
              type="submit"
              disabled={cancelling || !reason}
              className="bg-red-600 hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-semibold px-6 py-2 rounded-lg transition-colors"
            >
              {cancelling ? "Cancelling..." : "Cancel Order"}
            </button>
          </form>
        </div>
      )}

      {order.status === "CANCELLED" && (
        <div className="bg-red-50 border border-red-200 rounded-2xl p-5 text-center">
          <p className="text-red-600 font-bold text-lg mb-2">🚫 This Order Was Cancelled</p>
          {order.cancelReason && (
            <p className="text-red-500 text-sm">
              <strong>Reason:</strong> {order.cancelReason}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
