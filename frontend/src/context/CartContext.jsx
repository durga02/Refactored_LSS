import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import * as cartApi from "../api/cart";

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [miniCartOpen, setMiniCartOpen] = useState(false);

  /*
   * Backend session cart is the single source of truth.
   */
  const applyCart = (data) => {
    setItems(Array.isArray(data?.items) ? data.items : []);
  };

  /*
   * Load the cart from the backend session when the app starts.
   */
  const refreshCart = async () => {
    try {
      setLoading(true);

      const data = await cartApi.getCart();

      applyCart(data);
    } catch (error) {
      console.error("Failed to load cart", error);
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshCart();
  }, []);

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

  const addItem = async (productId, priceId, quantity = 1) => {
    try {
      setLoading(true);

      const data = await cartApi.addToCart(
        productId,
        priceId,
        quantity
      );

      /*
       * Backend response contains the complete session cart.
       * Always use it as the source of truth.
       */
      applyCart(data);

      setMiniCartOpen(true);
    } catch (error) {
      console.error("Failed to add item to cart", error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const updateItem = async (productId, priceId, quantity) => {
    try {
      setLoading(true);

      const newQuantity = Number(quantity);

      const data = await cartApi.updateCartItem(
        productId,
        priceId,
        newQuantity
      );

      applyCart(data);
    } catch (error) {
      console.error("Failed to update cart item", error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const removeItem = async (productId, priceId) => {
    try {
      setLoading(true);

      const data = await cartApi.removeCartItem(
        productId,
        priceId
      );

      applyCart(data);
    } catch (error) {
      console.error("Failed to remove cart item", error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const clear = async () => {
    try {
      setLoading(true);

      const data = await cartApi.clearCart();

      applyCart(data);
    } catch (error) {
      console.error("Failed to clear cart", error);
      throw error;
    } finally {
      setLoading(false);
    }
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
