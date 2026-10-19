
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import ProductCard from "../components/ProductCard.jsx";
import HeroCarousel from "../components/HeroCarousel.jsx";
import { getFeatured, getHomeSweets, getHomeHot } from "../api/products.js";
import api from "../api/client.js";

// Festival banner displayed below the hero carousel

function FestivalBannerDisplay() {
  const [banner, setBanner] = useState(null);

  useEffect(() => {
    api.get("/home/festival-banner")
      .then(({ data }) => setBanner(data))
      .catch(() => setBanner(null));
  }, []);

  if (!banner?.enabled || !banner?.message?.trim()) {
    return null;
  }

  return (
    <section
      className="relative overflow-hidden border-y border-amber-400/60 text-amber-50 shadow-md"
      style={{
        background: `linear-gradient(110deg, ${banner.backgroundColor || "#7f1d1d"} 0%, #641515 48%, ${banner.backgroundColor || "#7f1d1d"} 100%)`,
      }}
      aria-label="Festival announcement"
      role="status"
    >
      {/* Decorative gold accents */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 opacity-20"
        style={{
          backgroundImage:
            "radial-gradient(circle at 15% 50%, #fbbf24 0, transparent 2px), radial-gradient(circle at 85% 50%, #fbbf24 0, transparent 2px)",
          backgroundSize: "24px 24px",
        }}
      />

      <div className="relative mx-auto flex min-h-[62px] max-w-7xl items-center gap-3 px-4 py-3 sm:gap-5 sm:px-6">
        <span
          aria-hidden="true"
          className="hidden shrink-0 text-xl text-amber-300 sm:block"
        >
          ✦
        </span>

        {banner.scrolling ? (
          <div className="min-w-0 flex-1 overflow-hidden">
            <div className="festival-banner-track flex w-max items-center">
              {[0, 1].map((item) => (
                <span
                  key={item}
                  aria-hidden={item === 1}
                  className="shrink-0 whitespace-nowrap px-8 text-sm font-semibold tracking-wide sm:px-12 sm:text-base"
                >
                  {banner.message}
                  <span className="mx-8 text-amber-300" aria-hidden="true">
                    ✧
                  </span>
                </span>
              ))}
            </div>
          </div>
        ) : (
          <p className="min-w-0 flex-1 text-center text-sm font-semibold leading-relaxed tracking-wide sm:text-base">
            <span className="mr-2 text-amber-300" aria-hidden="true">
              ✦
            </span>
            {banner.message}
            <span className="ml-2 text-amber-300" aria-hidden="true">
              ✦
            </span>
          </p>
        )}

        <span
          aria-hidden="true"
          className="hidden shrink-0 text-xl text-amber-300 sm:block"
        >
          ✦
        </span>
      </div>

      {banner.scrolling && (
        <style>{`
          .festival-banner-track {
            animation: lss-festival-scroll 24s linear infinite;
            will-change: transform;
          }

          .festival-banner-track:hover {
            animation-play-state: paused;
          }

          @keyframes lss-festival-scroll {
            from { transform: translateX(0); }
            to { transform: translateX(-50%); }
          }

          @media (prefers-reduced-motion: reduce) {
            .festival-banner-track {
              animation: none;
              width: 100%;
              justify-content: center;
            }

            .festival-banner-track span:nth-child(2) {
              display: none;
            }
          }
        `}</style>
      )}
    </section>
  );
}

// Inline skeleton card
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

const BENEFITS = [
  { icon: "🧈", title: "Pure Desi Ghee", desc: "Every sweet made with authentic ghee" },
  { icon: "🌿", title: "No Artificial Essence", desc: "Natural flavours, always" },
  { icon: "🤲", title: "Handcrafted Daily", desc: "Made fresh in small batches" },
  { icon: "🚚", title: "Pan-India Delivery", desc: "Delivered to your doorstep" },
];

const TESTIMONIALS = [
  {
    name: "Priya R.",
    stars: 5,
    text: "The Kaju Katli melts in your mouth — pure ghee all the way!",
    location: "Hyderabad",
  },
  {
    name: "Ravi K.",
    stars: 5,
    text: "Ordered for our wedding. Everyone loved it. Will order again!",
    location: "Vijayawada",
  },
  {
    name: "Sunita M.",
    stars: 5,
    text: "Best online sweets shop. Shelf life was accurate, arrived fresh.",
    location: "Bengaluru",
  },
];

function SectionHeading({ children }) {
  return (
    <div className="text-center mb-8">
      <h2 className="text-2xl font-bold">{children}</h2>
      <span className="block w-12 h-1 bg-orange-500 rounded-full mx-auto mt-2" />
    </div>
  );
}

export default function Home() {
  const [featured, setFeatured] = useState([]);
  const [sweet, setSweet] = useState([]);
  const [hot, setHot] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    Promise.all([
      getFeatured({ signal: controller.signal }),
      getHomeSweets({ signal: controller.signal }),
      getHomeHot({ signal: controller.signal }),
    ])
      .then(([f, s, h]) => {
        setFeatured(f);
        setSweet(s.slice(0, 15));
        setHot(h.slice(0, 15));
      })
      .catch((err) => {
        if (err?.code !== "ERR_CANCELED") {
          setError("Could not load products. Is the backend running?");
        }
      })
      .finally(() => setLoading(false));

    return () => controller.abort();
  }, []);

  if (error) {
    return (
      <p className="max-w-xl mx-auto my-20 text-center text-red-600 font-medium">
        {error}
      </p>
    );
  }

  return (
    <>
      <HeroCarousel />

      {/* Festival banner managed from Admin */}
      <FestivalBannerDisplay />

      {/* Benefits strip */}
      <section className="bg-white/80 py-8 shadow-sm">
        <div className="max-w-7xl mx-auto px-4">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {BENEFITS.map((b) => (
              <div
                key={b.title}
                className="bg-white rounded-2xl shadow-sm p-5 flex flex-col items-center text-center gap-2 hover:bg-amber-400/10 transition-colors"
              >
                <span className="text-3xl">{b.icon}</span>
                <p className="font-semibold text-sm text-gray-800">{b.title}</p>
                <p className="text-xs text-gray-500">{b.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Brand message */}
      <section className="bg-[#f9e0c8] py-12">
        <div className="max-w-7xl mx-auto px-4 text-center">
          <div className="bg-white rounded-2xl shadow-xl p-10 max-w-3xl mx-auto">
            <h2 className="font-bold text-2xl text-[#f57c00] mb-3">
              "Where Sweetness meets perfection"
            </h2>
            <p className="mb-3 text-gray-700">
              At <strong>Lalitha Surya Sweets</strong>, every delicacy tells a story — No
              artificial essence added to any of our sweets.
            </p>
            <h5 className="font-semibold text-gray-800">
              You Are What You Eat, So Eat Something Sweet
            </h5>
          </div>
        </div>
      </section>

      {/* Featured */}
      <section id="featured" className="max-w-7xl mx-auto px-4 mb-16 mt-10">
        <div className="flex flex-col items-center mb-6">
          <SectionHeading>Featured Products</SectionHeading>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {loading
            ? Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)
            : featured.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      </section>

      {/* Traditional Sweets */}
      <section id="sweet" className="max-w-7xl mx-auto px-4 mb-14">
        <div className="flex flex-col items-center mb-2">
          <SectionHeading>Traditional Sweets</SectionHeading>
        </div>
        <div className="flex justify-end mb-4">
          <Link to="/category/Sweet" className="text-orange-500 font-semibold hover:underline text-sm">
            View All →
          </Link>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {loading
            ? Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)
            : sweet.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      </section>

      {/* Making section */}
      <section id="making" className="bg-[#fff7ec] py-16 text-center px-4">
        <SectionHeading>How Our Specials Are Made</SectionHeading>
        <p className="text-gray-500 mb-8">Watch our chefs craft magic daily 🍯🔥</p>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-10 max-w-6xl mx-auto">
          {[
            "https://www.youtube.com/embed/T2rvjUjR7hY?si=G1LNMx7mDj2cFHit",
            "https://www.youtube.com/embed/_cphhE0Xwjk?si=ifhbPaa2FEstVyKO",
            "https://www.youtube.com/embed/RC_N7rgm1TU?si=vYNNa8QqRbkm7OVe",
          ].map((src) => (
            <iframe
              key={src}
              src={src}
              title="YouTube video player"
              className="w-full h-72 md:h-80 rounded-xl"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerPolicy="strict-origin-when-cross-origin"
              allowFullScreen
            />
          ))}
        </div>
      </section>

      {/* Hot Snacks */}
      <section id="hot" className="max-w-7xl mx-auto px-4 mb-16 mt-10">
        <div className="flex flex-col items-center mb-2">
          <SectionHeading>Hot Snacks</SectionHeading>
        </div>
        <div className="flex justify-end mb-4">
          <Link to="/category/Hot" className="text-orange-500 font-semibold hover:underline text-sm">
            View All →
          </Link>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {loading
            ? Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)
            : hot.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      </section>

      {/* Testimonials */}
      <section className="bg-[#f9e0c8] py-16 px-4">
        <div className="max-w-7xl mx-auto">
          <SectionHeading>What Our Customers Say</SectionHeading>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-8">
            {TESTIMONIALS.map((t) => (
              <div
                key={t.name}
                className="bg-white rounded-2xl p-6 shadow-sm border-l-4 border-amber-400"
              >
                <div className="text-amber-400 text-lg mb-2">
                  {"★".repeat(t.stars)}
                </div>
                <p className="text-gray-700 text-sm leading-relaxed mb-4">"{t.text}"</p>
                <div>
                  <p className="font-semibold text-gray-900 text-sm">{t.name}</p>
                  <p className="text-gray-400 text-xs">{t.location}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
