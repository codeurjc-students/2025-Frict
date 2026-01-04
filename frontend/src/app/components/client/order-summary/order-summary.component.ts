import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router, RouterModule} from '@angular/router';
import {StepperModule} from 'primeng/stepper';
import {ButtonModule} from 'primeng/button';
import {RadioButtonModule} from 'primeng/radiobutton';
import {TagModule} from 'primeng/tag';
import {DividerModule} from 'primeng/divider';
import {InputTextModule} from 'primeng/inputtext';
import {CheckboxModule} from 'primeng/checkbox';
import {formatDueDate, formatPrice} from '../../../utils/numberFormat.util';
import {CartSummary} from '../../../models/cartSummary.model';
import {OrderService} from '../../../services/order.service';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {OrderItemsPage} from '../../../models/orderItemsPage.model';
import {UserService} from '../../../services/user.service';
import {User} from '../../../models/user.model';
import {Address} from '../../../models/address.model';
import {PaymentCard} from '../../../models/paymentCard.model';
import {InputMask} from 'primeng/inputmask';
import {Toast} from 'primeng/toast';
import {MessageService} from 'primeng/api';

@Component({
  selector: 'app-order-summary',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    StepperModule, // This imports p-stepper, p-step-list, p-step, p-step-panels, p-step-panel
    ButtonModule,
    RadioButtonModule,
    TagModule,
    DividerModule,
    InputTextModule,
    CheckboxModule,
    LoadingScreenComponent,
    Paginator,
    InputMask,
    Toast
  ],
  templateUrl: './order-summary.component.html',
  standalone: true,
  styleUrl: './order-summary.component.css'
})
export class OrderSummaryComponent implements OnInit {

  protected readonly formatPrice = formatPrice;
  protected readonly formatDueDate = formatDueDate;

  cartSummary!: CartSummary;
  cartItemsPage: OrderItemsPage = {orderItems: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  user!: User;
  firstItem: number = 0;
  itemsRows: number = 5;

  selectedAddress: Address | undefined = undefined;
  selectedPaymentCard: PaymentCard | undefined = undefined;

  showNewAddressForm = false;
  newAddress: Address = {id: "", alias: "", street: "", number: "", floor: "", postalCode: "", city: "", country: ""};

  showNewPaymentForm = false;
  newCard: PaymentCard = {id: "", alias: "", cardOwnerName: "", number: "", numberEnding: "", cvv: "", dueDate: ""};

  loading: boolean = true;
  error: boolean = false;
  loadingText: string = "Cargando, por favor espera...";

  loadingAddresses: boolean = true;
  loadingCards: boolean = true;

  activeStep: number = 1;

  constructor(private orderService: OrderService,
              private userService: UserService,
              private messageService: MessageService,
              private router: Router) {}

  ngOnInit() {
    this.getUserInfo();
  }

  protected confirmOrder() {
    if(this.selectedPaymentCard && this.selectedAddress){
      this.loadingText = 'Realizando tu pedido...'
      this.loading = true;
      this.orderService.createOrder(this.selectedAddress.id, this.selectedPaymentCard.id).subscribe({
        next: (order) => {
          this.orderService.setItemsCount(0);
          this.router.navigate(['/success'], {
            queryParams: { id: order.id, ref: order.referenceCode }
          });
        }
      })
    }
    else{
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Debes seleccionar una dirección y una tarjeta' });
    }
  }

  onCartItemsPageChange(event: PaginatorState) {
    this.firstItem = event.first ?? 0;
    this.itemsRows = event.rows ?? 10;
    this.getUserInfo();
  }

  protected getUserInfo(){
    this.userService.getLoggedUserInfo().subscribe({
      next: (user) => {
        this.user = user;
        this.loadingAddresses = false;
        this.loadingCards = false;
        this.getUserCartItemsPage();
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  protected getUserCartItemsPage(){
    this.orderService.getUserCartItemsPage(this.firstItem/this.itemsRows, this.itemsRows).subscribe({
      next: (items) => {
        this.cartItemsPage = items;
        this.loadCartSummary();
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  protected loadCartSummary() {
    this.orderService.getUserCartSummary().subscribe({
      next: (summary) => {
        this.cartSummary = summary;
        this.loading = false;
      }
    })
  }

  protected changeAddress(addr: Address) {
    this.selectedAddress = addr;
  }

  protected changePaymentCard(card: PaymentCard) {
    this.selectedPaymentCard = card;
  }

  protected cancelNewAddress() {
    this.newAddress = {id: "", alias: "", street: "", number: "", floor: "", postalCode: "", city: "", country: ""};
    this.showNewAddressForm = false;
  }

  protected cancelNewCard() {
    this.newCard = {id: "", alias: "", cardOwnerName: "", number: "", numberEnding: "", cvv: "", dueDate: ""};
    this.showNewPaymentForm = false;
  }

  protected saveNewAddress() {
    this.loadingAddresses = true;
    this.userService.submitAddress(this.newAddress).subscribe({
      next: (user) => {
        this.user = user;
        this.loadingAddresses = false;
        this.cancelNewAddress();
      }
    })
  }

  protected isValidDueDate(input: string): boolean {
    // 0[1-9] -> Accepts from 01 to 09
    // |      -> or
    // 1[0-2] -> Accepts 10, 11 and 12
    // \/     -> Searches for the bar
    // \d{2}  -> Searches for two digits for the year
    const regex = /^(0[1-9]|1[0-2])\/\d{2}$/;
    return regex.test(input);
  }

  protected saveNewCard() {
    if (this.isValidDueDate(this.newCard.dueDate)){
      this.userService.submitPaymentCard(this.newCard).subscribe({
        next: (user) => {
          this.user = user;
          this.cancelNewCard();
        }
      })
    }
    else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'El mes de caducidad no puede ser mayor a 12' });
    }
  }
}
