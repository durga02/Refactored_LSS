import { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { getCheckoutSummary, lookupPincode, placeOrder, verifyPayment } from "../api/checkout.js";
import { useCart } from "../context/CartContext.jsx";
import { useToast } from "../context/ToastContext.jsx";
import { loadRazorpay } from "../lib/razorpay.js";

const INDIAN_STATES = [
  "Andhra Pradesh", "Telangana", "Tamil Nadu", "Karnataka", "Kerala",
  "Maharashtra", "Gujarat", "Rajasthan", "Madhya Pradesh", "Goa",
  "Chhattisgarh", "Delhi", "Other",
];

const inputClass =
  "w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-200 transition";

const labelClass = "block text-sm font-medium text-gray-700 mb-1";

export default function Checkout() {
  const navigate = useNavigate();
  const { clear } = useCart();
  const { showToast } = useToast();

  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [agreedToShipping, setAgreedToShipping] = useState(false);

  const [form, setForm] = useState({
    name: "", phone: "", email: "", address: "", pincode: "", city: "", state: "",
  });

  useEffect(() => {
    const controller = new AbortController();
    getCheckoutSummary({ signal: controller.signal })
      .then(setSummary)
      .catch((err) => { if (err?.code !== "ERR_CANCELED") setSummary(null); })
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (form.pincode.length !== 6) return;
    const timer = setTimeout(() => {
      lookupPincode(form.pincode).then((data) => {
        if (data.city || data.state) {
          setForm((f) => ({ ...f, city: data.city || f.city, state: data.state || f.state }));
        }
      }).catch(() => {});
    }, 300);
    return () => clearTimeout(timer);
  }, [form.pincode]);

  const handleChange = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);

    try {
      await loadRazorpay();
    } catch (err) {
      showToast(err.message, "error");
      setSubmitting(false);
      return;
    }

    try {
      const orderResponse = await placeOrder(form);

      if (!orderResponse.success) {
        showToast(orderResponse.message || "Could not place order", "error");
        setSubmitting(false);
        return;
      }

      const options = {
        key: orderResponse.key,
        amount: orderResponse.amount,
        currency: "INR",
        name: "Lalitha Surya Sweets",
        description: "Order Payment",
        order_id: orderResponse.razorpayOrderId,
        prefill: { name: form.name, email: form.email, contact: form.phone },
        theme: { color: "#d32f2f" },
        handler: async (response) => {
          try {
            await verifyPayment({
              paymentId: response.razorpay_payment_id,
              razorpayOrderId: response.razorpay_order_id,
              signature: response.razorpay_signature,
              orderId: orderResponse.orderId,
            });
            clear();
            navigate(`/order-success/${orderResponse.orderId}`);
          } catch {
            navigate("/payment-failed");
          }
        },
        modal: { ondismiss: () => setSubmitting(false) },
      };

      const rzp = new window.Razorpay(options);
      rzp.on("payment.failed", (response) => {
        showToast("Payment Failed: " + response.error.description, "error");
        setSubmitting(false);
      });
      rzp.open();
    } catch (err) {
      showToast(err.response?.data?.message || "Could not place order", "error");
      setSubmitting(false);
    }
  };

  if (loading) {
    return <p className="max-w-xl mx-auto my-20 text-center text-gray-500">Loading checkout...</p>;
  }

  if (!summary || summary.items.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-24 px-4 text-center">
        <h2 className="text-2xl font-bold mb-4">Your cart is empty</h2>
        <Link
          to="/"
          className="bg-gradient-to-r from-orange-400 to-orange-600 text-white font-semibold px-8 py-3 rounded-full hover:shadow-lg transition-all no-underline"
        >
          Start Shopping
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8">
      <h2 className="text-2xl font-bold mb-8">Checkout</h2>

      <form onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Delivery Details */}
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <h3 className="text-lg font-bold mb-5">Delivery Details</h3>

            <div className="space-y-4">
              <div>
                <label className={labelClass} htmlFor="name">Full Name</label>
                <input id="name" type="text" required className={inputClass} value={form.name} onChange={handleChange("name")} />
              </div>
              <div>
                <label className={labelClass} htmlFor="phone">Mobile Number</label>
                <input id="phone" type="text" required className={inputClass} value={form.phone} onChange={handleChange("phone")} />
              </div>
              <div>
                <label className={labelClass} htmlFor="email">Email</label>
                <input id="email" type="email" required className={inputClass} value={form.email} onChange={handleChange("email")} />
              </div>
              <div>
                <label className={labelClass} htmlFor="address">House No, Building, Road, Area</label>
                <textarea id="address" required rows={3} className={inputClass} value={form.address} onChange={handleChange("address")} />
              </div>
              <div>
                <label className={labelClass} htmlFor="pincode">Pincode</label>
                <input id="pincode" type="text" maxLength={6} required className={inputClass} value={form.pincode} onChange={handleChange("pincode")} />
              </div>
              <div className="flex gap-4">
                <div className="flex-1">
                  <label className={labelClass} htmlFor="city">City</label>
                  <input id="city" type="text" className={inputClass} value={form.city} onChange={handleChange("city")} />
                </div>
                <div className="flex-1">
                  <label className={labelClass} htmlFor="state">State</label>
                  <select id="state" required className={inputClass} value={form.state} onChange={handleChange("state")}>
                    <option value="">Select state</option>
                    {INDIAN_STATES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
          </div>

          {/* Order Summary */}
          <div className="bg-white rounded-2xl shadow-sm p-6 self-start sticky top-24">
            <h3 className="text-lg font-bold mb-5">Order Summary</h3>

            <div className="space-y-2 mb-4">
              {summary.items.map((item) => (
                <div key={`${item.productId}_${item.priceId}`} className="flex justify-between text-sm">
                  <span className="text-gray-700">{item.productName} × {item.quantity}</span>
                  <span className="font-medium">₹{item.subtotal}</span>
                </div>
              ))}
            </div>

            <hr className="my-4 border-gray-100" />

            <div className="space-y-2 text-sm">
              <div className="flex justify-between"><span className="text-gray-600">Subtotal</span><span>₹{summary.subtotal}</span></div>
              <div className="flex justify-between"><span className="text-gray-600">Handling</span><span>₹{summary.handling}</span></div>
              <div className="flex justify-between"><span className="text-gray-600">Delivery</span><span className="font-medium text-amber-700">To be Confirmed</span></div>
              <div className="flex justify-between font-bold text-base pt-2 border-t border-gray-100 mt-2">
                <span>Total</span><span>₹{summary.total}</span>
              </div>
              <p className="mt-2 text-xs text-gray-500 text-right">
                Final courier charges will be confirmed before dispatch.
              </p>
            </div>
          </div>
        </div>

        {/* Shipping Policy */}
        <div className="mt-6 p-5 bg-amber-50 border border-amber-200 rounded-xl">
          <h4 className="font-bold mb-3">📦 Shipping Policy</h4>
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={agreedToShipping}
              onChange={(e) => setAgreedToShipping(e.target.checked)}
              className="w-4 h-4"
            />
            <span className="text-sm">I have read and agree to the shipping policy</span>
          </label>

          {agreedToShipping && (
            <div className="mt-4 text-sm text-gray-700">
              <p className="font-semibold mb-2">Shipping Charges:</p>
              <ul className="list-none space-y-1 ml-2">
                <li>✅ An estimated shipping cost will be shown at checkout</li>
                <li>✅ Final courier charges will be confirmed before dispatch</li>
              </ul>
              <p className="mt-2">📞 For exact courier charges, contact us on WhatsApp after placing your order.</p>
            </div>
          )}
        </div>

        <button
          type="submit"
          disabled={!agreedToShipping || submitting}
          className="mt-6 w-full bg-red-600 hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed text-white font-bold py-4 rounded-xl text-lg transition-colors"
        >
          {submitting ? "Processing..." : "Place Order"}
        </button>
      </form>
    </div>
  );
}
