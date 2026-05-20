import {Component, DestroyRef, inject, OnInit, PLATFORM_ID} from '@angular/core';
import {CommonModule, isPlatformBrowser} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {catchError, debounceTime, filter, of, Subject, switchMap} from 'rxjs';
import * as L from 'leaflet';
import {Router, RouterModule} from '@angular/router';
import {StepperModule} from 'primeng/stepper';
import {ButtonModule} from 'primeng/button';
import {RadioButtonModule} from 'primeng/radiobutton';
import {TagModule} from 'primeng/tag';
import {DividerModule} from 'primeng/divider';
import {InputTextModule} from 'primeng/inputtext';
import {CheckboxModule} from 'primeng/checkbox';
import {formatPrice} from '../../../utils/textFormat.util';
import {CartSummary} from '../../../models/cartSummary.model';
import {OrderService} from '../../../services/order.service';
import {LoadingScreenComponent} from '../../common/loading-screen/loading-screen.component';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {UserService} from '../../../services/user.service';
import {User} from '../../../models/user.model';
import {Address} from '../../../models/address.model';
import {PaymentCard} from '../../../models/paymentCard.model';
import {InputMask} from 'primeng/inputmask';
import {MessageService} from 'primeng/api';
import {PageResponse} from '../../../models/pageResponse.model';
import {OrderItem} from '../../../models/orderItem.model';
import {BreadcrumbReloadComponent} from '../../common/breadcrumb-reload/breadcrumb-reload.component';
import {BreadcrumbService} from '../../../utils/breadcrumb.service';
import {LocationService} from '../../../services/location.service';

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
    BreadcrumbReloadComponent
  ],
  templateUrl: './order-summary.component.html',
  standalone: true,
  styleUrl: './order-summary.component.css'
})
export class OrderSummaryComponent implements OnInit {

  private orderService = inject(OrderService);
  private userService = inject(UserService);
  private locationService = inject(LocationService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  private breadcrumbService = inject(BreadcrumbService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);

  protected readonly formatPrice = formatPrice;

  cartSummary!: CartSummary;
  cartItemsPage: PageResponse<OrderItem> = {items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  user!: User;
  firstItem: number = 0;
  itemsRows: number = 5;

  selectedAddress: Address | undefined = undefined;
  selectedPaymentCard: PaymentCard | undefined = undefined;

  showNewAddressForm = false;
  submittedAddress = false;
  newAddress: Address = {id: "", alias: "", street: "", number: "", floor: "", postalCode: "", city: "", country: ""};

  private addressMap: L.Map | undefined;
  private addressMarker: L.Marker | undefined;
  private isGeocodingActive = false;
  private lastAddressCheck = '';
  private addressChangeSubject = new Subject<void>();

  showNewPaymentForm = false;
  submittedCard = false;
  newCard: PaymentCard = {id: "", alias: "", cardOwnerName: "", number: "", numberEnding: "", cvv: "", dueDate: ""};

  loading: boolean = true;
  error: boolean = false;
  loadingText: string = "Cargando, por favor espera...";

  loadingAddresses: boolean = true;
  loadingCards: boolean = true;

  activeStep: number = 1;

  ngOnInit() {
    this.setupAddressListener();
    this.getUserInfo();
  }

  protected reloadAll() {
    this.selectedAddress = undefined;
    this.selectedPaymentCard = undefined;
    this.activeStep = 1;
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
    this.loading = true;
    this.error = false;

    this.breadcrumbService.insertPenultimateNodesForUrl(this.router.url, [{ label: "Carrito", routerLink: '/cart' }]);
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

  private setupAddressListener() {
    this.addressChangeSubject.pipe(
      filter(() => this.isGeocodingActive),
      debounceTime(1000),
      filter(() => {
        const addr = this.newAddress;
        const currentCheck = `${addr.street}|${addr.number}|${addr.city}|${addr.postalCode}|${addr.country}`;
        if (this.lastAddressCheck === currentCheck) return false;
        this.lastAddressCheck = currentCheck;
        return !!(addr.street && addr.city);
      }),
      switchMap(() =>
        this.locationService.getCoordinatesFromAddress(this.newAddress).pipe(
          catchError(() => of(null))
        )
      ),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(coords => {
      if (coords) {
        this.newAddress.latitude = coords.latitude;
        this.newAddress.longitude = coords.longitude;
        this.updateAddressMarker(coords.latitude, coords.longitude);
        if (this.addressMap) this.addressMap.setView([coords.latitude, coords.longitude], 16);
        this.messageService.add({ severity: 'info', summary: 'Ubicación actualizada', detail: 'Se ha movido el marcador según la dirección ingresada.' });
      } else {
        this.messageService.add({ severity: 'warn', summary: 'Dirección no encontrada', detail: 'No se encontraron coordenadas para la dirección indicada.' });
      }
    });
  }

  onAddressFieldChange() {
    this.addressChangeSubject.next();
  }

  toggleAddressForm() {
    this.newAddress.alias = 'Nueva dirección de envío';
    this.showNewAddressForm = true;
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => this.initAddressMap(), 50);
    }
  }

  private initAddressMap(): void {
    if (this.addressMap) return;
    const container = document.getElementById('order-address-map');
    if (!container) return;

    const lat = this.newAddress.latitude || 40.4168;
    const lng = this.newAddress.longitude || -3.7038;

    this.addressMap = L.map('order-address-map').setView([lat, lng], 6);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.addressMap);

    this.addressMap.attributionControl.setPrefix('Leaflet');

    const iconDefault = L.icon({
      iconUrl: './location-pointer.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41]
    });
    L.Marker.prototype.options.icon = iconDefault;

    this.addressMap.on('click', (e: L.LeafletMouseEvent) => {
      this.updateAddressMarker(e.latlng.lat, e.latlng.lng);
      this.newAddress.latitude = e.latlng.lat;
      this.newAddress.longitude = e.latlng.lng;
      this.getAddressFromCoordinates(e.latlng.lat, e.latlng.lng);
    });

    setTimeout(() => {
      this.syncAddressMemory();
      this.isGeocodingActive = true;
    }, 100);
  }

  private updateAddressMarker(lat: number, lng: number) {
    if (this.addressMarker) {
      this.addressMarker.setLatLng([lat, lng]);
    } else {
      this.addressMarker = L.marker([lat, lng]).addTo(this.addressMap!);
    }
  }

  private syncAddressMemory() {
    const addr = this.newAddress;
    this.lastAddressCheck = `${addr.street}|${addr.number}|${addr.city}|${addr.postalCode}|${addr.country}`;
  }

  private getAddressFromCoordinates(lat: number, lng: number) {
    this.isGeocodingActive = false;
    this.locationService.getAddressFromCoordinates(lat, lng).subscribe({
      next: (addressData) => {
        if (addressData) {
          this.newAddress = { ...this.newAddress, ...addressData, alias: this.newAddress.alias };
          this.syncAddressMemory();
          if (addressData.number) {
            this.messageService.add({ severity: 'info', summary: 'Dirección Exacta', detail: `Detectado nº ${addressData.number}` });
          } else {
            this.messageService.add({ severity: 'info', summary: 'Zona detectada', detail: 'Ubicación aproximada (sin número exacto)' });
          }
          setTimeout(() => { this.isGeocodingActive = true; }, 50);
        } else {
          this.isGeocodingActive = true;
          this.messageService.add({ severity: 'warn', summary: 'Dirección no encontrada', detail: 'No se encontró ninguna dirección para la ubicación indicada.' });
        }
      },
      error: () => {
        this.isGeocodingActive = true;
        this.messageService.add({ severity: 'warn', summary: 'Aviso', detail: 'No se pudo recuperar la dirección automática.' });
      }
    });
  }

  protected cancelNewAddress() {
    if (this.addressMap) {
      this.addressMap.remove();
      this.addressMap = undefined;
      this.addressMarker = undefined;
    }
    this.isGeocodingActive = false;
    this.lastAddressCheck = '';
    this.submittedAddress = false;
    this.newAddress = {id: "", alias: "", street: "", number: "", floor: "", postalCode: "", city: "", country: ""};
    this.showNewAddressForm = false;
  }

  togglePaymentForm() {
    if (this.showNewPaymentForm) {
      this.cancelNewCard();
    } else {
      this.newCard.alias = 'Nueva tarjeta';
      this.showNewPaymentForm = true;
    }
  }

  isCardFieldInvalid(value: string | undefined): boolean {
    return this.submittedCard && !value?.trim();
  }

  isCardDueDateInvalid(): boolean {
    return this.submittedCard && !this.isValidDueDate(this.newCard.dueDate ?? '');
  }

  protected cancelNewCard() {
    this.submittedCard = false;
    this.newCard = {id: "", alias: "", cardOwnerName: "", number: "", numberEnding: "", cvv: "", dueDate: ""};
    this.showNewPaymentForm = false;
  }

  protected saveNewAddress() {
    if (this.hasAddressValidationErrors()) {
      this.submittedAddress = true;
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos obligatorios.' });
      return;
    }
    this.loadingAddresses = true;
    this.userService.submitAddress(this.newAddress).subscribe({
      next: (user) => {
        this.user = user;
        this.loadingAddresses = false;
        this.cancelNewAddress();
      }
    })
  }

  protected hasAddressValidationErrors(): boolean {
    const a = this.newAddress;
    return !a.alias?.trim() || !a.street?.trim() || !a.number?.trim() || !a.postalCode?.trim() || !a.city?.trim() || !a.country?.trim();
  }

  isAddressFieldInvalid(value: string | undefined): boolean {
    return this.submittedAddress && !value?.trim();
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
    const c = this.newCard;
    if (!c.alias?.trim() || !c.number?.trim() || !c.cardOwnerName?.trim() || !c.cvv?.trim() || !this.isValidDueDate(c.dueDate)) {
      this.submittedCard = true;
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos obligatorios.' });
      return;
    }
    this.userService.submitPaymentCard(c).subscribe({
      next: (user) => {
        this.user = user;
        this.cancelNewCard();
      }
    });
  }
}
