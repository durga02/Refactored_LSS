import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext.jsx";
import { getAllProducts } from "../api/products.js";

const NAV_LINKS = [
  { label: "Home", to: "/" },
  { label: "Sweets", to: "/category/Sweet" },
  { label: "Hot", to: "/category/Hot" },
  { label: "Specials", to: "/category/Special" },
  { label: "Pickels", to: "/category/Pickel" },
];

export default function Header() {
  const { count, openMiniCart } = useCart();
  const navigate = useNavigate();

  const [navOpen, setNavOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [allProducts, setAllProducts] = useState([]);
  const [results, setResults] = useState([]);
  const [showResults, setShowResults] = useState(false);
  const searchRef = useRef(null);

  useEffect(() => {
    const controller = new AbortController();
    getAllProducts({ signal: controller.signal })
      .then(setAllProducts)
      .catch(() => setAllProducts([]));
    return () => controller.abort();
  }, []);

  useEffect(() => {
    function handleClickOutside(event) {
      if (searchRef.current && !searchRef.current.contains(event.target)) {
        setShowResults(false);
      }
    }
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, []);

  const handleSearch = (value) => {
    setQuery(value);
    if (!value.trim()) {
      setResults([]);
      setShowResults(false);
      return;
    }
    const lower = value.toLowerCase();
    setResults(allProducts.filter((p) => p.name.toLowerCase().includes(lower)));
    setShowResults(true);
  };
  
   const goToProduct = (product) => {
      setShowResults(false);
      setQuery("");

      navigate(`/category/${encodeURIComponent(product.category)}`, {
      state: { targetProductId: product.id },
      });
    };

  return (
    <header className="sticky top-0 z-50 bg-[#f4b400] shadow-md">
      {/* Announcement marquee */}
      <div className="bg-brand-red text-white text-xs py-1.5 overflow-hidden">
        <div className="flex animate-marquee whitespace-nowrap gap-12">
          {[
            
            "🍬 100% Pure Ghee — No artificial essence",
            "✨ Fresh Handcrafted Daily",
            "📦 Pan-India Shipping Available",
            
            "🍬 100% Pure Ghee — No artificial essence",
            "✨ Fresh Handcrafted Daily",
            "📦 Pan-India Shipping Available",
          ].map((msg, i) => (
            <span key={i} className="mx-6">
              {msg}
            </span>
          ))}
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4">
        <nav className="flex items-center justify-between py-4 flex-wrap gap-3">
          {/* Brand */}
          <Link to="/" className="font-bold text-xl text-black no-underline shrink-0">
            Lalitha Surya Sweets
          </Link>

          {/* Hamburger (mobile) */}
          <button
            type="button"
            aria-label="Toggle navigation"
            aria-expanded={navOpen}
            onClick={() => setNavOpen((o) => !o)}
            className="lg:hidden p-2 rounded text-black hover:bg-amber-500 transition-colors"
          >
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              {navOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>

          {/* Nav links */}
          <ul
            className={`${navOpen ? "flex" : "hidden"} lg:flex flex-col lg:flex-row gap-2 lg:gap-6 w-full lg:w-auto order-3 lg:order-none`}
          >
            {NAV_LINKS.map((link) => (
              <li key={link.to} className="list-none">
                <Link
                  to={link.to}
                  onClick={() => setNavOpen(false)}
                  className="text-white font-semibold text-[15px] hover:text-black transition-colors no-underline block"
                >
                  {link.label}
                </Link>
              </li>
            ))}
            <li className="list-none">
              <a
                href="#contact"
                onClick={() => setNavOpen(false)}
                className="text-white font-semibold text-[15px] hover:text-black transition-colors no-underline block"
              >
                Contact
              </a>
            </li>
          </ul>

          {/* Search + Cart */}
          <div className="flex items-center gap-3 shrink-0">
            {/* Search */}
            <div className="relative w-[220px]" ref={searchRef}>
              <input
                type="text"
                placeholder="Search sweets..."
                autoComplete="off"
                aria-label="Search products"
                value={query}
                onChange={(e) => handleSearch(e.target.value)}
                className="w-full px-3 py-2 rounded-full border border-gray-300 text-sm focus:outline-none focus:border-red-600 focus:ring-2 focus:ring-red-200 transition"
              />

              {showResults && (
                <div className="absolute top-10 left-0 w-full bg-white rounded-xl shadow-xl z-50 max-h-72 overflow-y-auto border border-gray-100">
                  {results.length === 0 ? (
                    <p className="p-3 text-gray-500 text-center text-sm">No sweets found</p>
                  ) : (
                    results.map((product) => (
                      <div
                        key={product.id}
                        role="option"
                        tabIndex={0}
                        aria-selected="false"
                        className="flex items-center gap-3 p-2 cursor-pointer hover:bg-gray-50 transition-colors"
                        onClick={() => goToProduct(product)}
                        onKeyDown={(e) => e.key === "Enter" && goToProduct(product)}
                      >
                        <img
                          src={product.imageUrl}
                          alt={product.name}
                          className="w-10 h-10 object-cover rounded-md shrink-0"
                        />
                        <span className="text-sm font-medium">{product.name}</span>
                      </div>
                    ))
                  )}
                </div>
              )}
            </div>

            {/* Cart button */}
            <button
              type="button"
              aria-label={`Open cart, ${count} items`}
              onClick={openMiniCart}
              className="relative border border-white text-white px-4 py-2 rounded-lg font-medium hover:bg-black hover:border-black transition-colors"
            >
              🛒 Cart
              {count > 0 && (
                <span className="absolute -top-2 -right-2 min-w-[20px] h-5 px-1 bg-red-600 text-white text-xs font-bold rounded-full flex items-center justify-center">
                  {count}
                </span>
              )}
            </button>
          </div>
        </nav>
      </div>
    </header>
  );
}
