import { memo, useState } from "react";
import { useCart } from "../context/CartContext.jsx";
import { useToast } from "../context/ToastContext.jsx";
import ProductModal from "./ProductModal.jsx";

function ProductCard({ product }) {
  const { addItem } = useCart();
  const { showToast } = useToast();
  const [selectedPriceId, setSelectedPriceId] = useState(product.prices?.[0]?.id ?? null);
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [imgLoaded, setImgLoaded] = useState(false);
  const [imgError, setImgError] = useState(false);

  const handleAdd = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (!selectedPriceId) {
      showToast("Please select a weight", "warning");
      return;
    }

    setAdding(true);
    try {
      await addItem(product.id, selectedPriceId, 1);
      setAdded(true);
      setTimeout(() => setAdded(false), 1200);
    } catch {
      showToast("Failed to add item to cart", "error");
    } finally {
      setAdding(false);
    }
  };

  const hasPrices = product.prices?.length > 0;

  return (
    <>
      <div
        id={`product-${product.id}`}
        className="bg-white rounded-2xl shadow-md hover:-translate-y-1 transition-transform duration-300 flex flex-col overflow-hidden cursor-pointer group scroll-mt-32"
        onClick={() => setModalOpen(true)}
      >
        {/* ── Image zone — fixed height so card never jumps ── */}
        <div className="relative h-48 bg-gray-100 overflow-hidden">

          {/* Skeleton: visible while loading OR when image failed */}
          {(!imgLoaded || imgError) && (
            <div className="absolute inset-0 bg-gray-200 animate-skeleton flex items-center justify-center">
              {imgError && (
                <span className="text-4xl select-none" aria-hidden="true">🍬</span>
              )}
            </div>
          )}

          {/* Real image — fades in once loaded; removed from DOM on error */}
          {!imgError && (
            <img
              src={product.imageUrl}
              alt={product.name}
              className={`absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ${
                imgLoaded ? "opacity-100" : "opacity-0"
              }`}
              onLoad={() => setImgLoaded(true)}
              onError={() => setImgError(true)}
            />
          )}

          {/* Featured badge */}
          {product.featured && (
            <span className="absolute top-3 left-3 z-10 bg-amber-400 text-white text-xs font-semibold px-2 py-0.5 rounded-full shadow">
              Featured
            </span>
          )}

          {/* Bottom gradient — only meaningful when image is visible */}
          {imgLoaded && !imgError && (
            <div className="absolute bottom-0 left-0 right-0 h-16 bg-gradient-to-t from-black/40 to-transparent" />
          )}
        </div>

        {/* ── Content ── */}
        <div className="p-4 flex flex-col flex-1">
          <h3 className="text-center font-semibold text-[1.05rem] mt-1 mb-2">{product.name}</h3>

          {/* Price buttons or skeleton pills */}
          <div
            className="flex flex-wrap gap-2 justify-center my-2"
            onClick={(e) => e.stopPropagation()}
          >
            {hasPrices ? (
              product.prices.map((pp) => (
                <button
                  type="button"
                  key={pp.id}
                  onClick={() => setSelectedPriceId(pp.id)}
                  className={`px-3 py-1 rounded-full text-sm border transition-colors cursor-pointer ${
                    selectedPriceId === pp.id
                      ? "bg-green-500 text-white border-green-500"
                      : "bg-white border-orange-400 hover:bg-orange-50"
                  }`}
                >
                  <span>{pp.label}</span> — ₹<span>{pp.price}</span>
                </button>
              ))
            ) : (
              <>
                <span className="h-7 w-20 rounded-full bg-gray-200 animate-skeleton inline-block" />
                <span className="h-7 w-20 rounded-full bg-gray-200 animate-skeleton inline-block" />
              </>
            )}
          </div>

          <form
            className="mt-auto pt-2"
            onSubmit={handleAdd}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="submit"
              disabled={adding || !hasPrices}
              className={`w-full py-2.5 rounded-full font-semibold text-white transition-all duration-200 ${
                added
                  ? "bg-green-500"
                  : "bg-gradient-to-r from-orange-400 to-orange-600 hover:-translate-y-0.5 hover:shadow-[0_6px_14px_rgba(255,120,0,0.35)]"
              } disabled:opacity-60 disabled:cursor-not-allowed`}
            >
              {added ? "✔ Added" : adding ? "Adding..." : hasPrices ? "Add to Cart" : "Unavailable"}
            </button>
          </form>
        </div>
      </div>

      {modalOpen && (
        <ProductModal product={product} onClose={() => setModalOpen(false)} />
      )}
    </>
  );
}

export default memo(ProductCard);
