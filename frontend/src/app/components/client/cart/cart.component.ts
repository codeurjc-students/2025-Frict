import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {FooterComponent} from '../../common/footer/footer.component';
import {NavbarComponent} from '../../common/navbar/navbar.component';

// Interfaces
export interface Product {
  id: string;
  name: string;
  specs: string;
  price: number;
  originalPrice: number;
  image: string;
  stock: boolean;
  shipping: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, FormsModule, FooterComponent, NavbarComponent],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent {

  // Mock Data: Carrito
  initialItems: CartItem[] = [
    {
      product: {
        id: '1',
        name: 'Portátil Gaming XYZ Pro 15" · i7 · 16GB · 1TB SSD',
        specs: 'Intel Core i7-12650H | 16GB RAM | 1TB SSD | Nvidia RTX 3050',
        price: 949.99,
        originalPrice: 1199.99,
        image: '[https://m.media-amazon.com/images/I/71p-M3sPhhL._AC_SL1500_.jpg](https://m.media-amazon.com/images/I/71p-M3sPhhL._AC_SL1500_.jpg)',
        stock: true,
        shipping: 'Recíbelo mañana'
      },
      quantity: 1
    }
  ];

  // Mock Data: Favoritos (Nueva sección)
  initialFavorites: Product[] = [
    {
      id: '3',
      name: 'Auriculares Inalámbricos 7.1 Surround',
      specs: 'Cancelación de ruido | 20h batería | Micrófono extraíble',
      price: 89.99,
      originalPrice: 129.99,
      image: '[https://m.media-amazon.com/images/I/61UxfXTUyvL._AC_SL1500_.jpg](https://m.media-amazon.com/images/I/61UxfXTUyvL._AC_SL1500_.jpg)',
      stock: true,
      shipping: 'Recíbelo el viernes'
    },
    {
      id: '4',
      name: 'Teclado Mecánico Switch Blue RGB',
      specs: 'Anti-ghosting | 105 Teclas | Reposamuñecas magnético',
      price: 59.50,
      originalPrice: 59.50,
      image: '[https://m.media-amazon.com/images/I/718b9W8J9AL._AC_SL1500_.jpg](https://m.media-amazon.com/images/I/718b9W8J9AL._AC_SL1500_.jpg)',
      stock: true,
      shipping: 'Recíbelo mañana'
    }
  ];

  // Signals
  cartItems = signal<CartItem[]>(this.initialItems);
  favoriteItems = signal<Product[]>(this.initialFavorites);

  // Computed Values
  subtotal = computed(() =>
    this.cartItems().reduce((acc, item) => acc + (item.product.originalPrice * item.quantity), 0)
  );

  total = computed(() =>
    this.cartItems().reduce((acc, item) => acc + (item.product.price * item.quantity), 0)
  );

  totalDiscount = computed(() =>
    this.subtotal() - this.total()
  );

  totalItems = computed(() =>
    this.cartItems().reduce((acc, item) => acc + item.quantity, 0)
  );

  // Actions Carrito
  updateQuantity(productId: string, newQuantity: number) {
    if (newQuantity < 1) return;
    this.cartItems.update(items =>
      items.map(item =>
        item.product.id === productId ? { ...item, quantity: newQuantity } : item
      )
    );
  }

  removeItem(productId: string) {
    this.cartItems.update(items =>
      items.filter(item => item.product.id !== productId)
    );
  }

  clearCart() {
    this.cartItems.set([]);
  }

  // Actions Favoritos (Nuevas funciones)
  removeFavorite(productId: string) {
    this.favoriteItems.update(items =>
      items.filter(item => item.id !== productId)
    );
  }

  moveToCart(product: Product) {
    // 1. Eliminar de favoritos
    this.removeFavorite(product.id);

    // 2. Añadir al carrito (comprobar si ya existe para sumar cantidad)
    this.cartItems.update(items => {
      const existingItem = items.find(i => i.product.id === product.id);
      if (existingItem) {
        return items.map(i =>
          i.product.id === product.id ? { ...i, quantity: i.quantity + 1 } : i
        );
      }
      return [...items, { product, quantity: 1 }];
    });
  }

  calculateDiscount(product: Product): number {
    return Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100);
  }
}
