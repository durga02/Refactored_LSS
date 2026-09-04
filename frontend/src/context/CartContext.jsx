import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import * as cartApi from "../api/cart";

const CartContext = createContext(null);

const CART_STORAGE_KEY = "lalitha_surya_cart";

function loadStoredCart() {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY);

    if (!raw) {
      return [];
    }

    const parsed = JSON.parse(raw);

    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error("Failed to read cart from localStorage", error);
    return [];
  }
}

export function CartProvider({ children }) {
  const [items, setItems] = useState(() => loadStoredCart());
  const [loading, setLoading] = useState(false);
  const [miniCartOpen, setMiniCartOpen] = useState(false);

  /*
   * localStorage is now the source of truth for the guest cart.
   */
  useEffect(() => {
    try {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
    } catch (error) {
      console.error("Failed to save cart", error);
    }
  }, [items]);

  const total = useMemo(() => {
    return items.reduce(
      (sum, item) =>
        sum + Number(item.price || 0) * Number(item.quantity || 0),
      0
    );
  }, [items]);

  const count = useMemo(() => {
    return items.reduce(
      (sum, item) => sum + Number(item.quantity || 0),
      0
    );
  }, [items]);

  /*
   * Kept for compatibility with components that already call refreshCart().
   * It now reloads from localStorage instead of depending on HttpSession.
   */
  const refreshCart = () => {
    setItems(loadStoredCart());
  };

  const addItem = async (productId, priceId, quantity = 1) => {
    try {
      /*
       * We still call the backend so it can validate the product/price
       * and return the complete cart item information.
       *
       * IMPORTANT:
       * We DO NOT replace our local cart with data.items anymore.
       */
      const data = await cartApi.addToCart(
        productId,
        priceId,
        quantity
      );

      const returnedItems = data?.items || [];

      const addedItem =
        returnedItems.find(
          (item) =>
            Number(item.productId) === Number(productId) &&
            Number(item.priceId) === Number(priceId)
        ) || returnedItems[returnedItems.length - 1];

      if (!addedItem) {
        throw new Error("Backend did not return the added cart item");
      }

      setItems((currentItems) => {
        const existingIndex = currentItems.findIndex(
          (item) =>
            Number(item.productId) === Number(productId) &&
            Number(item.priceId) === Number(priceId)
        );

        /*
         * Same product + same selected price:
         * increase quantity instead of creating duplicate line.
         */
        if (existingIndex !== -1) {
          return currentItems.map((item, index) => {
            if (index !== existingIndex) {
              return item;
            }

            const newQuantity =
              Number(item.quantity || 0) + Number(quantity);

            return {
              ...item,
              quantity: newQuantity,
              subtotal:
                Number(item.price || addedItem.price || 0) *
                newQuantity,
            };
          });
        }

        /*
         * New product/price combination.
         */
        const newItem = {
          ...addedItem,
          productId: Number(productId),
          priceId: Number(priceId),
          quantity: Number(quantity),
          subtotal:
            Number(addedItem.price || 0) *
            Number(quantity),
        };

        return [...currentItems, newItem];
      });

      setMiniCartOpen(true);
    } catch (error) {
      console.error("Failed to add item to cart", error);
      throw error;
    }
  };

  const updateItem = (productId, priceId, quantity) => {
    const newQuantity = Number(quantity);

    /*
     * Quantity zero means remove the item.
     */
    if (newQuantity <= 0) {
      removeItem(productId, priceId);
      return;
    }

    setItems((currentItems) =>
      currentItems.map((item) => {
        const matches =
          Number(item.productId) === Number(productId) &&
          Number(item.priceId) === Number(priceId);

        if (!matches) {
          return item;
        }

        return {
          ...item,
          quantity: newQuantity,
          subtotal: Number(item.price || 0) * newQuantity,
        };
      })
    );
  };

  const removeItem = (productId, priceId) => {
    setItems((currentItems) =>
      currentItems.filter(
        (item) =>
          !(
            Number(item.productId) === Number(productId) &&
            Number(item.priceId) === Number(priceId)
          )
      )
    );
  };

  const clear = () => {
    setItems([]);
    localStorage.removeItem(CART_STORAGE_KEY);
  };

  return (
    <CartContext.Provider
      value={{
        items,
        total,
        count,
        loading,
        miniCartOpen,

        openMiniCart: () => setMiniCartOpen(true),
        closeMiniCart: () => setMiniCartOpen(false),

        refreshCart,
        addItem,
        updateItem,
        removeItem,
        clear,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);

  if (!ctx) {
    throw new Error("useCart must be used within a CartProvider");
  }

  return ctx;
}
