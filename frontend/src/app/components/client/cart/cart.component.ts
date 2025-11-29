import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {FooterComponent} from '../../common/footer/footer.component';
import {NavbarComponent} from '../../common/navbar/navbar.component';
import {OrderService} from '../../../services/order.service';
import {AuthService} from '../../../services/auth.service';
import {ProductsPage} from '../../../models/productsPage.model';
import {OrderItemsPage} from '../../../models/orderItemsPage.model';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {ProductService} from '../../../services/product.service';
import {formatPrice} from '../../../utils/numberFormat.util';
import {InputNumber} from 'primeng/inputnumber';
import {RouterLink} from '@angular/router';
import {Select} from 'primeng/select';
import {StockTagComponent} from '../../common/stock-tag/stock-tag.component';

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
  imports: [CommonModule, FormsModule, FooterComponent, NavbarComponent, InputNumber, RouterLink, Select, Paginator, StockTagComponent],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit {

  protected readonly formatPrice = formatPrice;

  loading: boolean = true;
  error: boolean = false;

  //Global pagination options
  options = [
    { label: 5, value: 5 },
    { label: 10, value: 10 },
    { label: 20, value: 20 }
  ];

  //Cart items pagination
  foundItems : OrderItemsPage = {orderItems: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstItem: number = 0;
  itemsRows: number = 10;

  //Favourite products pagination
  foundProducts : ProductsPage = {products: [], totalProducts: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstProduct: number = 0;
  productsRows: number = 10;

  constructor(private orderService: OrderService,
              private productService: ProductService,
              private authService: AuthService) {}

  ngOnInit(){
    this.getUserCartItems();
    this.getUserFavouriteProducts();
  }


  protected getTotalItems(): number {
    if (!this.foundItems || !this.foundItems.orderItems || this.foundItems.orderItems.length === 0) {
      return 0;
    }

    const totalUnits = this.foundItems.orderItems.reduce((acumulador, item) => {
      const quantity = item.quantity ?? 0;
      return acumulador + quantity;

    }, 0);

    return totalUnits;
  }


  public formatCategories(categories: any[]): string {
    if (!categories || categories.length === 0) {
      return "";
    }
    return categories.map(category => category.name).join(', ');
  }

  onCartItemsPageChange(event: PaginatorState) {
    this.firstItem = event.first ?? 0;
    this.itemsRows = event.rows ?? 10;
    this.getUserCartItems();
  }

  onFavouriteProductsPageChange(event: PaginatorState) {
    this.firstProduct = event.first ?? 0;
    this.productsRows = event.rows ?? 10;
    this.getUserCartItems();
  }

  protected clearCart() {
    this.orderService.clearUserCartItems().subscribe({
      next: () => {
        this.foundItems.orderItems = [];
        this.foundItems.totalItems = 0;
        this.foundItems.currentPage = 0;
        this.foundItems.lastPage = -1;
        this.foundItems.pageSize = 0;
      }
    })
  }

  protected getUserCartItems(){
    if(this.authService.isLogged()){
      this.orderService.getUserCartItemsPage(this.firstItem/this.itemsRows, this.itemsRows).subscribe({
        next: (items) => {
          console.log(items);
          this.foundItems = items;
        }
      })
    }
  }

  protected getUserFavouriteProducts(){
    if(this.authService.isLogged()){
      this.productService.getUserFavouriteProductsPage(this.firstProduct/this.productsRows, this.productsRows).subscribe({
        next: (items) => {
          this.foundProducts = items;
        }
      })
    }
  }

  protected removeItem(id: string) {
    //Llamar a removeItem
    this.orderService.deleteItem(id).subscribe({
      next: () => {
        this.foundItems.orderItems = this.foundItems.orderItems.filter(item => item.id !== id);
        this.foundItems.totalItems = this.foundItems.totalItems > 0 ? this.foundItems.totalItems - 1 : 0;
      }
    })
  }

  protected moveToCart(id: string) {
    //Mover al carrito
  }

  protected removeFavorite(id: string) {
    this.productService.deleteProductFromFavourites(id).subscribe({
      next: () => {
        this.foundProducts.products = this.foundProducts.products.filter(product => product.id !== id);
        this.foundProducts.totalProducts = this.foundProducts.totalProducts > 0 ? this.foundProducts.totalProducts - 1 : 0;
      }
    })
  }

  protected calculateSum(isTotal: boolean): number {
    if (!this.foundItems || !this.foundItems.orderItems || this.foundItems.orderItems.length === 0) {
      return 0;
    }

    let totalSum = this.foundItems.orderItems.reduce((acumulador, item) => {
      const quantity = item.quantity ?? 0;

      let usedPrice: number;
      const previousPrice = item.product?.previousPrice ?? 0;
      const currentPrice = item.product?.currentPrice ?? 0;

      if (!isTotal) {
        if (previousPrice === 0) {
          usedPrice = currentPrice;
        } else {
          usedPrice = previousPrice;
        }
      } else usedPrice = currentPrice;

      return acumulador + (usedPrice * quantity);

    }, 0);

    if (isTotal && totalSum < 50) {
      totalSum += 5;
    }
    return totalSum;
  }

  protected calculateDiscount(): number {
    if (!this.foundItems || !this.foundItems.orderItems || this.foundItems.orderItems.length === 0) {
      return 0.0;
    }

    return this.foundItems.orderItems.reduce((acumulador, item) => {

      const previousPrice = item.product?.previousPrice ?? 0;
      const currentPrice = item.product?.currentPrice ?? 0;
      const quantity = item.quantity ?? 0;

      if (previousPrice > 0 && quantity > 0) {
        const descuentoUnidad = previousPrice - currentPrice;
        const descuentoTotalItem = descuentoUnidad * quantity;
        return acumulador + descuentoTotalItem;
      } else {
        return acumulador;
      }

    }, 0.0);
  }

}
