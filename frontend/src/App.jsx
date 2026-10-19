import { lazy } from "react";
import { Route, Routes } from "react-router-dom";

import StorefrontLayout from "./components/StorefrontLayout.jsx";
import CheckoutLayout from "./components/CheckoutLayout.jsx";
import MinimalLayout from "./components/MinimalLayout.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";

// Storefront pages — eager loaded (public, fast first paint)
import Home from "./pages/Home.jsx";
import Category from "./pages/Category.jsx";
import Cart from "./pages/Cart.jsx";
import Checkout from "./pages/Checkout.jsx";
import OrderSuccess from "./pages/OrderSuccess.jsx";
import PaymentFailed from "./pages/PaymentFailed.jsx";
import TrackOrder from "./pages/TrackOrder.jsx";
import Policies from "./pages/Policies.jsx";
import NotFound from "./pages/NotFound.jsx";

// Admin pages — lazy loaded (only admin users ever need this bundle)
const AdminLogin = lazy(() => import("./pages/admin/AdminLogin.jsx"));
const AdminLayout = lazy(() => import("./pages/admin/AdminLayout.jsx"));
const AdminDashboard = lazy(() => import("./pages/admin/AdminDashboard.jsx"));
const AdminOrders = lazy(() => import("./pages/admin/AdminOrders.jsx"));
const AdminOrderDetails = lazy(() => import("./pages/admin/AdminOrderDetails.jsx"));
const AdminProducts = lazy(() => import("./pages/admin/AdminProducts.jsx"));
const AdminProductForm = lazy(() => import("./pages/admin/AdminProductForm.jsx"));
const AdminCustomers = lazy(() => import("./pages/admin/AdminCustomers.jsx"));
const AdminFestivalBanner = lazy(
  () => import("./pages/admin/AdminFestivalBanner.jsx")
);

export default function App() {
  return (
    <Routes>
      {/* Storefront */}
      <Route element={<StorefrontLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/category/:category" element={<Category />} />
        <Route path="/order-success/:id" element={<OrderSuccess />} />
        <Route path="/payment-failed" element={<PaymentFailed />} />
        <Route path="/policies" element={<Policies />} />
      </Route>

      {/* Cart/Checkout: minimal header, no full nav, to reduce drop-off */}
      <Route element={<CheckoutLayout />}>
        <Route path="/cart" element={<Cart />} />
        <Route path="/checkout" element={<Checkout />} />
      </Route>

      {/* Track Order: reached via a direct link, not by browsing the shop */}
      <Route element={<MinimalLayout />}>
        <Route path="/track/:id" element={<TrackOrder />} />
      </Route>

      {/* Admin */}
      <Route path="/admin/login" element={<AdminLogin />} />
      <Route
        path="/admin"
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route path="dashboard" element={<AdminDashboard />} />
        <Route path="orders" element={<AdminOrders />} />
        <Route path="orders/:id" element={<AdminOrderDetails />} />
        <Route path="products" element={<AdminProducts />} />
        <Route path="products/add" element={<AdminProductForm />} />
        <Route path="products/edit/:id" element={<AdminProductForm />} />
        <Route path="customers" element={<AdminCustomers />} />
        <Route path="festival-banner" element={<AdminFestivalBanner />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
