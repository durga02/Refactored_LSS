import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext.jsx";

const NAV_ITEMS = [
  { to: "/admin/dashboard", label: "📊 Dashboard" },
  { to: "/admin/orders", label: "📦 Orders" },
  { to: "/admin/products", label: "🍬 Products" },
  { to: "/admin/customers", label: "👤 Customers" },
  { to: "/admin/festival-banner", label: "🎉 Festival Banner" },
];

export default function AdminLayout() {
  const { logout, username } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/admin/login", { replace: true });
  };

  return (
    <div className="flex min-h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="w-56 bg-slate-800 text-white flex flex-col shrink-0">
        <div className="px-5 py-6 font-bold text-base border-b border-slate-700">
          🍬 Lalitha Surya Sweets
        </div>

        <nav className="flex-1 py-4">
          {NAV_ITEMS.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `block px-5 py-3 text-sm font-medium transition-colors no-underline ${
                  isActive
                    ? "bg-slate-700 text-white border-l-4 border-amber-400"
                    : "text-slate-300 hover:bg-slate-700 hover:text-white"
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        <button
          type="button"
          onClick={handleLogout}
          className="mx-4 mb-5 py-2.5 px-4 text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-700 rounded-lg transition-colors text-left"
        >
          🚪 Logout
        </button>
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Topbar */}
        <div className="bg-white shadow-sm px-6 py-4 text-sm font-semibold text-gray-700 border-b border-gray-100">
          Admin Panel{username ? ` — ${username}` : ""}
        </div>

        <section className="flex-1 p-6 overflow-auto">
          <Outlet />
        </section>
      </div>
    </div>
  );
}
