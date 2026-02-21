import {Component, OnDestroy, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {OrderService} from '../../../services/order.service';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {ProductService} from '../../../services/product.service';
import {formatPrice} from '../../../utils/textFormat.util';
import {RouterLink} from '@angular/router';
import {OrderItem} from '../../../models/orderItem.model';
import {catchError, debounceTime, distinctUntilChanged, of, Subject, switchMap} from 'rxjs';
import {CartSummary} from '../../../models/cartSummary.model';
import {Button} from 'primeng/button';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {PageResponse} from '../../../models/pageResponse.model';
import {Product} from '../../../models/product.model';
import {Tooltip} from 'primeng/tooltip';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, Paginator, Button, LoadingScreenComponent, Tooltip],
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit, OnDestroy {

  protected readonly formatPrice = formatPrice;

  loading: boolean = true;
  error: boolean = false;

  //Cart items pagination
  foundItems : PageResponse<OrderItem> = {items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  cartSummary!: CartSummary;
  firstItem: number = 0;
  itemsRows: number = 5;

  //Favourite products pagination
  foundProducts : PageResponse<Product> = {items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstProduct: number = 0;
  productsRows: number = 5;

  private quantityUpdateSubject = new Subject<{item: OrderItem, quantity: number}>();

  constructor(private orderService: OrderService,
              private productService: ProductService) {}

  ngOnInit(){
    this.quantityUpdateSubject.pipe(
      debounceTime(250),
      distinctUntilChanged((prev, curr) => {
        return prev.item.product.id === curr.item.product.id && prev.quantity === curr.quantity;
      }),
      switchMap(data => {
        return this.orderService.updateItemQuantity(data.item.product.id, data.quantity).pipe(
          catchError(error => {
            return of(null);
          })
        );
      })
    ).subscribe({
      next: (summary) => {
        if(summary){
          this.cartSummary = summary;
          this.orderService.setItemsCount(summary.totalItems);
        }
      }
    });

    this.getUserCartItemsPage();
    this.getUserCartSummary();
    this.getUserFavouriteProducts();
  }

  ngOnDestroy() {
    this.quantityUpdateSubject.complete();
  }

  protected updateItemQuantity(item: OrderItem, newQuantity: number) {
    if (newQuantity === null || newQuantity === undefined) {
      return;
    }

    let finalQuantity = newQuantity;

    if (newQuantity < 1) {
      finalQuantity = 1;
    } else if (newQuantity > item.product.totalUnits) {
      finalQuantity = item.product.totalUnits;
    }

    if (finalQuantity !== newQuantity) {
      setTimeout(() => {
        item.quantity = finalQuantity;
      }, 0);
    } else {
      item.quantity = finalQuantity;
    }

    this.quantityUpdateSubject.next({ item, quantity: finalQuantity });
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
    this.getUserCartItemsPage();
  }

  onFavouriteProductsPageChange(event: PaginatorState) {
    this.firstProduct = event.first ?? 0;
    this.productsRows = event.rows ?? 10;
    this.getUserFavouriteProducts();
  }

  isProductInCart(productId: string): boolean {
    return this.foundItems?.items?.some(item => item.product.id === productId) ?? false;
  }

  protected clearCart() {
    this.orderService.clearUserCartItems().subscribe({
      next: (summary) => {
        this.cartSummary = summary;
        this.orderService.setItemsCount(summary.totalItems);
        this.foundItems.items = [];
        this.foundItems.totalItems = 0;
        this.foundItems.currentPage = 0;
        this.foundItems.lastPage = -1;
        this.foundItems.pageSize = 0;
      }
    })
  }

  protected getUserCartItemsPage(){
    this.orderService.getUserCartItemsPage(this.firstItem/this.itemsRows, this.itemsRows).subscribe({
      next: (items) => {
        this.foundItems = items;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  protected getUserCartSummary(){
    this.orderService.getUserCartSummary().subscribe({
      next: (summary) => {
        this.cartSummary = summary;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  protected getUserFavouriteProducts(){
    this.productService.getUserFavouriteProductsPage(this.firstProduct/this.productsRows, this.productsRows).subscribe({
      next: (items) => {
        this.foundProducts = items;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  protected removeItem(id: string) {
    this.orderService.deleteItem(id).subscribe({
      next: (summary) => {
        this.foundItems.items = this.foundItems.items.filter(item => item.id !== id);
        this.foundItems.totalItems = this.foundItems.totalItems > 0 ? this.foundItems.totalItems - 1 : 0;
        this.cartSummary = summary;
        this.orderService.setItemsCount(summary.totalItems);
      }
    })
  }

  protected moveToCart(id: string) {
    this.orderService.addItemToCart(id, 1).subscribe({
      next: (item) => {
        this.removeFavorite(id);
        this.getUserCartItemsPage();
        this.getUserCartSummary();
      }
    })
  }

  protected removeFavorite(id: string) {
    this.productService.deleteProductFromFavourites(id).subscribe({
      next: () => {
        this.foundProducts.items = this.foundProducts.items.filter(product => product.id !== id);
        this.foundProducts.totalItems = this.foundProducts.totalItems > 0 ? this.foundProducts.totalItems - 1 : 0;
      }
    })
  }
}
