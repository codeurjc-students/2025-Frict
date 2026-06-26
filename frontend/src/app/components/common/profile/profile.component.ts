import {Component, computed, DestroyRef, inject, OnInit, PLATFORM_ID, signal} from '@angular/core';
import {CommonModule, isPlatformBrowser} from '@angular/common';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {catchError, debounceTime, filter, of, Subject, switchMap} from 'rxjs';
import * as L from 'leaflet';
import {Router, RouterModule} from '@angular/router';

import {FormsModule} from '@angular/forms';
import {User} from '../../../models/user.model';
import {UserService} from '../../../services/user.service';
import {OrderService} from '../../../services/order.service';
import {ReviewService} from '../../../services/review.service';
import {ShopService} from '../../../services/shop.service';
import {TruckService} from '../../../services/truck.service';
import {AuthService} from '../../../services/auth.service';

import {Paginator, PaginatorState} from 'primeng/paginator';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {InputMask} from 'primeng/inputmask';
import {Select} from 'primeng/select';
import {Button} from 'primeng/button';
import {Rating} from 'primeng/rating';
import {Tag} from 'primeng/tag';
import {Avatar} from 'primeng/avatar';
import {ConfirmationService, MessageService, SharedModule} from 'primeng/api';

import {Address} from '../../../models/address.model';
import {PaymentCard} from '../../../models/paymentCard.model';
import {PageResponse} from '../../../models/pageResponse.model';
import {Order} from '../../../models/order.model';
import {Review} from '../../../models/review.model';
import {Shop} from '../../../models/shop.model';
import {Truck} from '../../../models/truck.model';
import {ThemeColor, UiService} from '../../../utils/ui.service';
import {formatAddress, formatPrice} from '../../../utils/textFormat.util';
import {getOrderStatusTagInfo, getUserRoleTagInfo} from '../../../utils/tagManager.util';
import {HttpErrorResponse} from '@angular/common/http';
import {LoadingScreenComponent} from '../loading-screen/loading-screen.component';
import {BreadcrumbReloadComponent} from '../breadcrumb-reload/breadcrumb-reload.component';
import {LocationService} from '../../../services/location.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    LoadingScreenComponent,
    Paginator,
    Dialog,
    InputText,
    InputMask,
    Select,
    SharedModule,
    Button,
    Rating,
    Tag,
    Avatar,
    BreadcrumbReloadComponent
  ],
  templateUrl: './profile.component.html',
  styles: []
})
export class ProfileComponent implements OnInit {

  protected uiService = inject(UiService);
  public authService = inject(AuthService);
  private userService = inject(UserService);
  private orderService = inject(OrderService);
  private reviewService = inject(ReviewService);
  private shopService = inject(ShopService);
  private truckService = inject(TruckService);
  private locationService = inject(LocationService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private destroyRef = inject(DestroyRef);
  private platformId = inject(PLATFORM_ID);
  protected router = inject(Router);

  user!: User;

  foundOrders : PageResponse<Order> = {items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstOrder: number = 0;
  ordersRows: number = 5;

  foundReviews: PageResponse<Review> = {items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstReview: number = 0;
  reviewsRows: number = 5;

  // Employee-only state
  assignedTruck: Truck | null = null;
  assignedShopsPage: PageResponse<Shop> | null = null;
  firstShop: number = 0;
  shopsRows: number = 5;

  loading: boolean = true;
  error: boolean = false;

  visibleImageDialog: boolean = false;
  visibleDataDialog: boolean = false;
  visibleAddressDialog: boolean = false;
  visibleCardDialog: boolean = false;

  newUserData!: User;
  submittedAddress = false;
  newAddress: Address = {id: "", alias: "", street: "", number: "", floor: "", postalCode: "", city: "", country: ""};
  newCard: PaymentCard = {id: "", alias: "", cardOwnerName: "", number: "", numberEnding: "", cvv: "", dueDate: ""};
  submittedCard = false;

  private addressMap: L.Map | undefined;
  private addressMarker: L.Marker | undefined;
  private isGeocodingActive = false;
  private lastAddressCheck = '';
  private addressChangeSubject = new Subject<void>();

  selectedImage: File | null = null;
  isDragging = false;
  previewUrl: string | ArrayBuffer | null = null;

  //Customization
  shops = signal<Shop[]>([]);
  selectedShop = signal<any>(null);
  isListLoaded = false;
  isButtonDisabled = computed(() => {
    const shop = this.selectedShop();
    const currentSavedId = this.authService.selectedShopId();
    const selectedId = shop ? shop.id : null;
    return selectedId === currentSavedId;
  });

  //Connection
  isOnline = signal<boolean>(true);
  lastCheckTime = signal<Date | null>(null);

  // Call uiService to change the theme color
  onColorChange(color: ThemeColor) {
    if (color) {
      this.uiService.changeThemeColor(color);
    }
  }

  onSaveStore() {
    this.confirmationService.confirm({
      message: '¿Estás seguro de querer cambiar de tienda seleccionada? Se eliminarán todos los productos actualmente en el carrito.',
      header: 'Cambiar tienda seleccionada',
      icon: 'pi pi-info-circle',
      rejectLabel: 'Cancel',
      rejectButtonProps: {
        label: 'Cancelar',
        severity: 'secondary',
        outlined: true,
      },
      acceptButtonProps: {
        label: 'Aceptar',
        severity: 'warn',
      },

      accept: () => {
        this.userService.setSelectedShopId(this.selectedShop()?.id ?? null).subscribe({
          next: () => {
            this.authService.setSelectedShopId(this.selectedShop()?.id ?? null);
            this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Se ha cambiado correctamente la tienda seleccionada.` });
            this.orderService.itemsCount.set(0);
          },
          error: () => {
            this.selectedShop.set(this.authService.selectedShopId());
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Ha ocurrido un error cambiando la tienda seleccionada. Operación cancelada.' });
          }
        })
      }
    });
  }

  //Delete account confirmation
  confirm(event: Event) {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: '¿Estás seguro de querer eliminar tu cuenta? Se eliminará toda tu información de envío y facturación, y no podrás acceder a ella de nuevo.',
      header: 'Eliminar cuenta',
      icon: 'pi pi-info-circle',
      rejectLabel: 'Cancel',
      rejectButtonProps: {
        label: 'Cancelar',
        severity: 'secondary',
        outlined: true,
      },
      acceptButtonProps: {
        label: 'Eliminar',
        severity: 'danger',
      },

      accept: () => {
        this.loading = true;
        this.userService.anonLoggedUser().subscribe({
          next: () => {
            this.logoutOnDelete();
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Ha ocurrido un error borrando al usuario. Inténtalo de nuevo.' });
            this.loading = false;
          }
        });
      }
    });
  }

  protected logoutOnDelete() {
    this.authService.logout().subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/']);
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    });
  }

  ngOnInit() {
    this.setupAddressListener();
    this.loadUser();
  }

  protected loadUser(){
    this.loading = true;
    this.error = false;

    this.userService.getLoggedUserInfo().subscribe({
      next: (user) => {
        this.user = user;

        // Load data conditionally based on the user's role
        if (this.authService.isUser()) {
          this.loadSelectedShop();
          this.loadUserOrders();
          this.loadUserReviews();
        } else if (this.authService.isManager()) {
          this.loadManagerShops();
          this.loading = false;
        } else if (this.authService.isDriver()) {
          this.loadDriverTruck();
          this.loading = false;
        } else {
          this.loading = false;
        }
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  // Load employee-only entities
  protected loadManagerShops() {
    this.shopService.getAssignedShopsPage(this.firstShop / this.shopsRows, this.shopsRows).subscribe({
      next: (page) => {
        this.assignedShopsPage = page;
      },
      error: () => this.assignedShopsPage = null
    });
  }

  onShopsPageChange(event: PaginatorState) {
    this.firstShop = event.first ?? 0;
    this.shopsRows = event.rows ?? 5;
    this.loadManagerShops();
  }

  protected loadDriverTruck() {
    this.truckService.getAssignedTruckByDriverId(this.user.id).subscribe({
      next: (truck) => {
        this.assignedTruck = truck;
      },
      error: () => this.assignedTruck = null
    });
  }

  loadSelectedShop() {
    const selectedShopId = this.user.selectedShopId;
    if (selectedShopId){
      this.shopService.getShopById(selectedShopId).subscribe({
        next: (shop) => {
          this.selectedShop.set(shop);
          this.shops.set([shop]);
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.error = true;
        }
      });
    }
  }

  onDropdownOpen() {
    if (this.isListLoaded) return;
    this.shopService.getAllShopsList().subscribe({
      next: (allShops) => {
        this.shops.set(allShops);
        this.isListLoaded = true;
      }
    });
  }

  onUserOrdersPageChange(event: PaginatorState) {
    this.firstOrder = event.first ?? 0;
    this.ordersRows = event.rows ?? 10;
    this.loadUserOrders();
  }

  onUserReviewsPageChange(event: PaginatorState) {
    this.firstReview = event.first ?? 0;
    this.reviewsRows = event.rows ?? 10;
    this.loadUserReviews();
  }

  //Profile image drop zone logic
  onFileSelected(event: any) {
    const file = event.target.files[0];
    this.processFile(file);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
    const file = event.dataTransfer?.files[0];
    this.processFile(file);
  }

  processFile(file: File | undefined) {
    if (file && file.type.startsWith('image/jpeg')) {
      this.selectedImage = file;

      //Previews logic
      const reader = new FileReader();
      reader.onload = (e) => {
        this.previewUrl = e.target?.result || null;
      };
      reader.readAsDataURL(file);

    } else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'La imagen debe tener formato JPG.' });
    }
  }

  clearSelection() {
    this.selectedImage = null;
    this.previewUrl = null;

    // Reset of HTML input to allow choosing the same file if desired
    const input = document.getElementById('dropzone-file') as HTMLInputElement;
    if(input) input.value = '';
  }

  cancelUploadImage() {
    this.visibleImageDialog = false;
    setTimeout(() => this.clearSelection(), 300);
  }

  protected submitUploadImage() {
    if (this.selectedImage){
      this.userService.uploadUserImage(this.user.id, this.selectedImage).subscribe({
        next: (user) => {
          this.user.imageInfo = user.imageInfo;
          this.messageService.add({ severity: 'success', summary: 'Éxito', detail: 'Imagen de perfil actualizada.' });
          this.cancelUploadImage();
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se ha podido actualizar la imagen de perfil.' });
        }
      })
    }
  }

  //Show creation and edition dialogs
  showDataDialog() {
    this.newUserData = structuredClone(this.user);
    this.visibleDataDialog = true;
  }

  showAddressCreationDialog() {
    this.newAddress.alias = 'Nueva dirección de envío';
    this.visibleAddressDialog = true;
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => this.initAddressMap(), 100);
    }
  }

  showCardCreationDialog() {
    this.newCard.alias = 'Nueva tarjeta';
    this.visibleCardDialog = true;
  }

  showEditAddressDialog(id: string) {
    const addressFound = this.user.addresses.find(addr => addr.id === id);
    if (addressFound) {
      this.newAddress = structuredClone(addressFound);
      this.visibleAddressDialog = true;
      if (isPlatformBrowser(this.platformId)) {
        setTimeout(() => this.initAddressMap(), 100);
      }
    }
  }

  showEditCardDialog(id: string) {
    const cardFound = this.user.cards.find(card => card.id === id);
    if(cardFound){
      this.newCard = structuredClone(cardFound);
      this.visibleCardDialog = true;
    }
  }

  //Create/Edit operations

  protected submitAddress() {
    if (this.hasAddressValidationErrors()) {
      this.submittedAddress = true;
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos obligatorios.' });
      return;
    }
    this.userService.submitAddress(this.newAddress).subscribe({
      next: (user) => {
        this.user.addresses = user.addresses;
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

  protected submitCard() {
    const c = this.newCard;
    const isNew = !c.id;
    if (!c.alias?.trim() || !c.cardOwnerName?.trim() || !this.isValidDueDate(c.dueDate) ||
        (isNew && (!c.number?.trim() || !c.cvv?.trim()))) {
      this.submittedCard = true;
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos obligatorios.' });
      return;
    }
    this.userService.submitPaymentCard(c).subscribe({
      next: (user) => {
        this.user.cards = user.cards;
        this.cancelNewCard();
      }
    });
  }

  isCardFieldInvalid(value: string | undefined): boolean {
    return this.submittedCard && !value?.trim();
  }

  isCardDueDateInvalid(): boolean {
    return this.submittedCard && !this.isValidDueDate(this.newCard.dueDate ?? '');
  }

  protected saveEditData() {
    this.userService.submitUserData(this.newUserData).subscribe({
      next: (user) => {
        this.user = user;
        this.cancelEditData();
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 403) {
          this.messageService.add({
            severity: 'error',
            summary: 'Error de registro',
            detail: 'El nombre del usuario ya está en uso. Elige otro.'
          });
        } else this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se ha podido editar la información del usuario.'
        });
      }
    })
  }

  //Delete operations
  protected deleteAddress(id: string) {
    this.userService.deleteAddress(id).subscribe({
      next: (user) => {
        this.user.addresses = user.addresses;
      }
    })
  }

  protected deleteCard(id: string) {
    this.userService.deletePaymentCard(id).subscribe({
      next: (user) => {
        this.user.cards = user.cards;
      }
    })
  }

  //Cancel operations
  protected cancelEditData() {
    this.newUserData = structuredClone(this.user);
    this.visibleDataDialog = false;
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

  private initAddressMap(): void {
    if (this.addressMap) return;
    const container = document.getElementById('profile-address-map');
    if (!container) return;

    const hasCoords = !!(this.newAddress.latitude && this.newAddress.longitude);
    const lat = hasCoords ? this.newAddress.latitude! : 40.4168;
    const lng = hasCoords ? this.newAddress.longitude! : -3.7038;
    const zoom = hasCoords ? 15 : 6;

    this.addressMap = L.map('profile-address-map').setView([lat, lng], zoom);

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

    if (hasCoords) {
      this.updateAddressMarker(lat, lng);
    }

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
    this.visibleAddressDialog = false;
  }

  protected cancelNewCard() {
    this.submittedCard = false;
    this.newCard = {id: "", alias: "", cardOwnerName: "", number: "", numberEnding: "", cvv: "", dueDate: ""};
    this.visibleCardDialog = false;
  }

  protected isValidDueDate(input: string): boolean {
    const regex = /^(0[1-9]|1[0-2])\/\d{2}$/;
    return regex.test(input);
  }

  protected loadUserOrders(){
    this.orderService.getLoggedUserOrders(this.firstOrder/this.ordersRows, this.ordersRows).subscribe({
      next: (orders) => {
        this.foundOrders = orders;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  protected loadUserReviews(){
    this.reviewService.getLoggedUserReviews(this.firstReview/this.reviewsRows, this.reviewsRows).subscribe({
      next: (reviews) => {
        this.foundReviews = reviews;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  checkBackendConnection() {
    this.userService.checkBackendConnection().subscribe({
      next: (res) => {
        this.isOnline.set(res.status === 'UP');
        this.lastCheckTime.set(new Date());

        if (res.status !== 'UP') {
          this.messageService.add({ severity: 'warn', summary: 'Sistema', detail: 'El servidor responde pero la BDD podría tener problemas.' });
        }
      },
      error: () => {
        this.isOnline.set(false);
        this.lastCheckTime.set(new Date());
        this.messageService.add({ severity: 'error', summary: 'Error de Red', detail: 'No se ha podido contactar con el backend.' });
      }
    });
  }

  protected readonly getOrderStatusTagInfo = getOrderStatusTagInfo;

  protected getRoleLabel(status: string): string {
    const labels: Record<string, string> = {
      USER: 'Usuario registrado',
      MANAGER: 'Gerente',
      DRIVER: 'Repartidor',
      ADMIN: 'Administrador'
    };
    return labels[status] || status;
  }

  protected readonly formatPrice = formatPrice;
  protected readonly formatAddress = formatAddress;
  protected readonly getUserRoleTagInfo = getUserRoleTagInfo;
}
