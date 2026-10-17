import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getAdminOrder, updateOrderStatus } from "../../api/admin.js";
import { useToast } from "../../context/ToastContext.jsx";

const STATUS_COLORS = {
  DELIVERED: "bg-green-100 text-green-700",
  CANCELLED: "bg-red-100 text-red-700",
  SHIPPED: "bg-blue-100 text-blue-700",
  OUT_FOR_DELIVERY: "bg-purple-100 text-purple-700",
  PLACED: "bg-amber-100 text-amber-700",
  CONFIRMED: "bg-amber-100 text-amber-700",
  PREPARING: "bg-orange-100 text-orange-700",
  PACKED: "bg-blue-100 text-blue-700",
};

const thClass = "text-left px-5 py-3 font-semibold text-gray-600 text-sm";
const tdClass = "px-5 py-3 text-sm";

export default function AdminOrderDetails() {
  const { id } = useParams();
  const { showToast } = useToast();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [nextStatus, setNextStatus] = useState("");
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    getAdminOrder(id, { signal: controller.signal })
      .then(setOrder)
      .catch((err) => { if (err?.code !== "ERR_CANCELED") setError("Failed to load order."); })
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, [id]);

  const handleUpdate = async () => {
    if (!nextStatus) return;
    setUpdating(true);
    try {
      const updated = await updateOrderStatus(id, nextStatus);
      setOrder(updated);
      setNextStatus("");
      showToast("Status updated successfully", "success");
    } catch (err) {
      showToast(err.response?.data?.message || "Failed to update status.", "error");
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return <p className="text-center text-gray-500 py-10">Loading order...</p>;
  if (error) return <p className="text-center text-red-600 py-10">{error}</p>;
  if (!order) return null;

  const statusClass = STATUS_COLORS[order.status] || "bg-gray-100 text-gray-700";

  return (
    <div>
      <Link to="/admin/orders" className="text-blue-600 hover:underline text-sm mb-4 inline-block">
        ← Back to orders
      </Link>

      <div className="flex items-center gap-3 mb-6">
        <h2 className="text-xl font-bold">Order #{order.id}</h2>
        <span className={`px-3 py-1 rounded-full text-xs font-bold ${statusClass}`}>
          {order.status}
        </span>
        <span className="text-gray-400 text-sm">
          {order.orderDate ? new Date(order.orderDate).toLocaleString() : ""}
        </span>
      </div>

      {/* Info cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
        {[
          {
            title: "Customer",
            content: [order.customerNameSnapshot, order.customerPhoneSnapshot, order.customer?.email],
          },
          {
            title: "Delivery Address",
            content: [order.address, order.state],
          },
          {
            title: "Payment",
            content: [`Status: ${order.paymentStatus || "-"}`, `Payment ID: ${order.paymentId || "-"}`],
          },
        ].map(({ title, content }) => (
          <div key={title} className="bg-white rounded-2xl shadow-sm p-5">
            <h3 className="font-bold text-sm text-gray-500 mb-3">{title}</h3>
            <div className="space-y-1">
              {content.filter(Boolean).map((line, i) => (
                <p key={i} className="text-sm text-gray-700">{line}</p>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Items table */}
      <h3 className="font-bold text-base mb-3">Items</h3>
      <div className="bg-white rounded-2xl shadow-sm overflow-x-auto mb-5">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-100">
            <tr>
              <th className={thClass}>Product</th>
              <th className={thClass}>Size / Label</th>
              <th className={thClass}>Qty</th>
              <th className={thClass}>Price</th>
              <th className={thClass}>Subtotal</th>
            </tr>
          </thead>
          <tbody>
            {order.items?.map((item) => (
              <tr key={item.id} className="border-b border-gray-50 hover:bg-gray-50">
                <td className={tdClass}>{item.productName}</td>
                <td className={tdClass}>{item.priceLabel}</td>
                <td className={tdClass}>{item.quantity}</td>
                <td className={tdClass}>₹{item.price}</td>
                <td className={tdClass}>₹{item.subtotal}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Totals */}
      <div className="bg-white rounded-2xl shadow-sm p-5 inline-block mb-6">
        <div className="space-y-1 text-sm">
          <div className="flex justify-between gap-8"><span className="text-gray-500">Subtotal</span><span>₹{order.subtotal}</span></div>
          <div className="flex justify-between gap-8"><span className="text-gray-500">Delivery</span><span>₹{order.deliveryCharges}</span></div>
          <div className="flex justify-between gap-8"><span className="text-gray-500">Handling</span><span>₹{order.handlingCharges}</span></div>
          <div className="flex justify-between gap-8 font-bold pt-2 border-t border-gray-100 mt-1"><span>Total</span><span>₹{order.totalAmount}</span></div>
        </div>
      </div>

      <button
        type="button"
        onClick={handleDownloadInvoice}
        disabled={invoiceLoading}
        className="rounded-lg bg-red-800 px-4 py-2 text-sm font-medium text-white hover:bg-red-900 disabled:cursor-not-allowed disabled:opacity-60"
        >
        {invoiceLoading ? "Downloading..." : "Download Invoice"}
      </button>
      
      {order.cancelReason && (
        <p className="text-sm text-red-600 mb-6">
          <strong>Cancel Reason:</strong> {order.cancelReason}
        </p>
      )}

      {/* Status update */}
      <h3 className="font-bold text-base mb-3">Update Status</h3>
      {order.nextAllowedStatuses?.length > 0 ? (
        <div className="flex gap-3">
          <select
            value={nextStatus}
            onChange={(e) => setNextStatus(e.target.value)}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400 transition"
          >
            <option value="">Change to...</option>
            {order.nextAllowedStatuses.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          <button
            type="button"
            disabled={!nextStatus || updating}
            onClick={handleUpdate}
            className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-semibold px-5 py-2 rounded-lg text-sm transition-colors"
          >
            {updating ? "Updating..." : "Update Status"}
          </button>
        </div>
      ) : (
        <p className="text-gray-400 text-sm italic">This order is in a final state and cannot be updated.</p>
      )}
    </div>
  );
}
