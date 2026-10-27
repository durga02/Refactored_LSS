import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { useLocation, useParams } from "react-router-dom";
import ProductCard from "../components/ProductCard.jsx";
import { getProductsByCategory } from "../api/products.js";

const CATEGORY_EMOJI = {
  Sweet: "🍬",
  Hot: "🌶️",
  Special: "⭐",
  Pickel: "🥒",
};

function SkeletonCard() {
  return (
    <div className="bg-white/60 rounded-2xl overflow-hidden animate-skeleton">
      <div className="w-full h-48 bg-gray-200" />
      <div className="p-4 flex flex-col gap-3">
        <div className="h-4 bg-gray-200 rounded-full w-3/4 mx-auto" />
        <div className="h-3 bg-gray-200 rounded-full w-1/2 mx-auto" />
        <div className="h-9 bg-gray-200 rounded-full mt-2" />
      </div>
    </div>
  );
}

export default function Category() {
  const { category } = useParams();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const location = useLocation();
  
  useEffect(() => {
    setLoading(true);
    const controller = new AbortController();

    getProductsByCategory(category, { signal: controller.signal })
      .then(setProducts)
      .catch((err) => {
        if (err?.code !== "ERR_CANCELED") setProducts([]);
      })
      .finally(() => setLoading(false));

    return () => controller.abort();
  }, [category]);

  useEffect(() => {
    if (loading || !location.state?.targetProductId) return;

    const targetId = `product-${location.state.targetProductId}`;

    requestAnimationFrame(() => {
      document.getElementById(targetId)?.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
    });
    }, [loading, products, location.state]);

  const emoji = CATEGORY_EMOJI[category] ?? "";

  return (
    <section className="max-w-7xl mx-auto px-4 mt-10 mb-16">
      <h2 className="text-2xl font-bold mb-2">
        {emoji && <span className="mr-2">{emoji}</span>}
        {category} Products
      </h2>

      {!loading && (
        <p className="text-gray-500 text-sm mb-6">{products.length} products found</p>
      )}

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {Array.from({ length: 8 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : products.length === 0 ? (
        <p className="text-center text-gray-500 py-20">No products found in this category yet.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </section>
  );
}
