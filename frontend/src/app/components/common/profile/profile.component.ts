import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {Router, RouterModule} from '@angular/router';

import {ButtonModule} from 'primeng/button';
import {TagModule} from 'primeng/tag';
import {AvatarModule} from 'primeng/avatar';
import {RatingModule} from 'primeng/rating';
import {FormsModule} from '@angular/forms';
import {User} from '../../../models/user.model';
import {UserService} from '../../../services/user.service';
import {OrderService} from '../../../services/order.service';
import {ReviewService} from '../../../services/review.service';
import {LoadingScreenComponent} from '../loading-screen/loading-screen.component';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {Dialog} from 'primeng/dialog';
import {InputText} from 'primeng/inputtext';
import {Address} from '../../../models/address.model';
import {PaymentCard} from '../../../models/paymentCard.model';
import {ConfirmationService, MessageService} from 'primeng/api';
import {HttpErrorResponse} from '@angular/common/http';
import {InputMask} from 'primeng/inputmask';
import {AuthService} from '../../../services/auth.service';
import {PageResponse} from '../../../models/pageResponse.model';
import {Order} from '../../../models/order.model';
import {Review} from '../../../models/review.model';
import {formatPrice} from '../../../utils/numberFormat.util';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ButtonModule,
    TagModule,
    AvatarModule,
    RatingModule,
    LoadingScreenComponent,
    Paginator,
    Dialog,
    InputText,
    InputMask
  ],
  templateUrl: './profile.component.html',
  styles: []
})
export class ProfileComponent implements OnInit {

  user!: User;

  foundOrders : PageResponse<Order> = {items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstOrder: number = 0;
  ordersRows: number = 5;

  foundReviews: PageResponse<Review> = {items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  firstReview: number = 0;
  reviewsRows: number = 5;

  loading: boolean = true;
  error: boolean = false;

  visibleImageDialog: boolean = false;
  visibleDataDialog: boolean = false;
  visibleAddressDialog: boolean = false;
  visibleCardDialog: boolean = false;

  newUserData!: User;
  newAddress: Address = {id: "", alias: "", street: "", number: "", floor: "", postalCode: "", city: "", country: ""};
  newCard: PaymentCard = {id: "", alias: "", cardOwnerName: "", number: "", numberEnding: "", cvv: "", dueDate: ""};

  selectedImage: File | null = null;
  isDragging = false;
  previewUrl: string | ArrayBuffer | null = null;


  constructor(private authService: AuthService,
              private userService: UserService,
              private orderService: OrderService,
              private reviewService: ReviewService,
              private messageService: MessageService,
              private confirmationService: ConfirmationService,
              protected router: Router) {}


  //Delete account confirmation
  confirm(event: Event) {
    this.confirmationService.confirm({
      target: event.target as EventTarget,
      message: '¿Estás seguro de querer eliminar tu cuenta? Se eliminará tu toda tu información de envío y facturación, y no podrás acceder a ella de nuevo.',
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
    this.loadUser();
  }

  protected loadUser(){
    this.userService.getLoggedUserInfo().subscribe({
      next: (user) => {
        this.user = user;
        this.loadUserOrders();
        this.loadUserReviews();
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
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
    this.visibleAddressDialog = true;
  }

  showCardCreationDialog() {
    this.visibleCardDialog = true;
  }

  showEditAddressDialog(id: string) {
    const addressFound = this.user.addresses.find(addr => addr.id === id);
    if(addressFound){
      this.newAddress = structuredClone(addressFound);
      this.visibleAddressDialog = true;
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
    this.userService.submitAddress(this.newAddress).subscribe({
      next: (user) => {
        this.user.addresses = user.addresses;
        this.cancelNewAddress();
      }
    })
  }

  protected submitCard() {
    if (this.isValidDueDate(this.newCard.dueDate)){
      this.userService.submitPaymentCard(this.newCard).subscribe({
        next: (user) => {
          this.user.cards = user.cards;
          this.cancelNewCard();
        }
      })
    }
    else {
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'El mes de caducidad no puede ser mayor a 12' });
    }
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
            detail: 'El nombre del usuario ya está en uso. Elige otro.' // Usamos el texto exacto del servidor
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

  protected cancelNewAddress() {
    this.newAddress = {id: "", alias: "", street: "", number: "", floor: "", postalCode: "", city: "", country: ""};
    this.visibleAddressDialog = false;
  }

  protected cancelNewCard() {
    this.newCard = {id: "", alias: "", cardOwnerName: "", number: "", numberEnding: "", cvv: "", dueDate: ""};
    this.visibleCardDialog = false;
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

  protected getStatusSeverity(status: string): "success" | "info" | "warn" | "danger" | "secondary" | "contrast" {
    switch (status) {
      case 'Pedido realizado': return 'success';
      case 'Enviado': return 'warn';
      case 'En reparto': return 'info';
      case 'Completado': return 'contrast';
      case 'Cancelado': return 'danger';
      default: return 'secondary';
    }
  }

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
}
