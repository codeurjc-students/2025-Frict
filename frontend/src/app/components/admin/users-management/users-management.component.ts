import {Component, computed, OnInit, signal} from '@angular/core';
import { CommonModule } from '@angular/common';
import {Button} from 'primeng/button';
import {Tag} from 'primeng/tag';
import {TableModule} from 'primeng/table';
import {Avatar} from 'primeng/avatar';
import {Tooltip} from 'primeng/tooltip';
import {PageResponse} from '../../../models/pageResponse.model';
import {User} from '../../../models/user.model';
import {UserService} from '../../../services/user.service';
import {Paginator, PaginatorState} from 'primeng/paginator';
import {ConfirmationService, MessageService} from 'primeng/api';
import {UIChart} from 'primeng/chart';
import {StatData} from '../../../utils/statData.model';
import {getUserRoleTagInfo, getUserStatusTagInfo} from '../../../utils/tagManager.util';
import {Dialog} from 'primeng/dialog';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {InputText} from 'primeng/inputtext';
import {CustomValidators} from '../../../utils/customValidators.util';
import {AuthService} from '../../../services/auth.service';
import {Select} from 'primeng/select';

@Component({
  selector: 'app-users-management',
  standalone: true,
  imports: [
    CommonModule, Button, Tag, TableModule, Avatar, Tooltip, Paginator, UIChart, Dialog, FormsModule, InputText, ReactiveFormsModule, Select
  ],
  templateUrl: './users-management.component.html',
  styleUrl: 'users-management.component.css'
})
export class UsersManagementComponent implements OnInit {

  usersPage: PageResponse<User> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  selectedUser: User | null = null;

  first: number = 0;
  rows: number = 5;

  loading: boolean = true;
  error: boolean = false;

  internalRoles = [
    { name: 'Gerente', code: 'MANAGER' },
    { name: 'Conductor', code: 'DRIVER' },
    { name: 'Administrador', code: 'ADMIN' }
  ];

  rawStats = signal<StatData[]>([]);
  options = signal<any>(null);

  data = computed(() => {
    const stats = this.rawStats();
    const documentStyle = getComputedStyle(document.documentElement);
    return {
      labels: stats.map(s => s.category),
      datasets: [
        {
          data: stats.map(s => s.total),
          backgroundColor: [
            documentStyle.getPropertyValue('--p-green-600'),
            documentStyle.getPropertyValue('--p-red-600'),
            documentStyle.getPropertyValue('--p-cyan-600'),
            documentStyle.getPropertyValue('--p-yellow-600')
          ],
          hoverBackgroundColor: [
            documentStyle.getPropertyValue('--p-green-500'),
            documentStyle.getPropertyValue('--p-red-500'),
            documentStyle.getPropertyValue('--p-cyan-500'),
            documentStyle.getPropertyValue('--p-yellow-500')
          ]
        }
      ]
    };
  });

  visibleNewInternalUserDialog: boolean = false;
  visibleChangePasswordDialog: boolean = false;
  newInternalUserForm: FormGroup;
  changePasswordForm: FormGroup;
  showPassword: boolean = false;
  showNewPassword: boolean = false;
  showNewPasswordConfirmation: boolean = false;
  selectedImage: File | null = null;

  constructor(private userService: UserService,
              private confirmationService: ConfirmationService,
              private fb: FormBuilder,
              private authService: AuthService,
              private messageService: MessageService,) {

    this.changePasswordForm = this.fb.nonNullable.group({
      password: ['', Validators.required],
      repeatPassword: ['', Validators.required]
    }, { validators: CustomValidators.passwordMatchValidator });

    this.newInternalUserForm = this.fb.nonNullable.group({
      name: ['', Validators.required],
      username: ['', Validators.required, [CustomValidators.createUsernameValidator(this.userService)]],
      email: ['', [Validators.required, Validators.email], [CustomValidators.createEmailValidator(this.userService)]],
      password: ['', Validators.required],
      role: ['', Validators.required]
    });
  }

  get usernameControl() { return this.newInternalUserForm.get('username'); }
  get emailControl() { return this.newInternalUserForm.get('email'); }

  ngOnInit() {
    this.initChartOptions();
    this.loadUsers();
    this.loadStats();
  }

  cancelUserCreation() {
    this.newInternalUserForm.reset();
    this.visibleNewInternalUserDialog = false;
  }

  cancelChangePassword() {
    this.changePasswordForm.reset();
    this.visibleChangePasswordDialog = false;
  }

  onChangePasswordSubmit(id: string){
    this.authService.changeInternalUserPassword(id, this.changePasswordForm.value).subscribe({
      next: () => {
        this.cancelChangePassword();
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Contraseña del usuario ${this.selectedUser?.name} cambiada correctamente`});
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: `Ha ocurrido un error cambiando la contraseña del usuario ${this.selectedUser?.name}. Inténtalo de nuevo.` });
      }
    })
  }

  onNewUserSubmit() {
    this.authService.signup(this.newInternalUserForm.value).subscribe({
      next: (loginInfo) => { //Backend returns some fields, one of them being the id of the user created
        if (this.selectedImage) {
          this.uploadUserImage(loginInfo.id, this.selectedImage);
        }
        else this.loadUsers();
        this.cancelUserCreation();
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Usuario ${loginInfo.name} creado correctamente` });
        this.loadStats();
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Ha ocurrido un error creando al usuario. Inténtalo de nuevo.' });
      }
    })
  }

  private uploadUserImage(userId: string, selectedImage: File) {
    this.userService.uploadUserImage(userId, selectedImage).subscribe({
      next: () => {
        this.selectedImage = null;
        this.loadUsers();
      },
      error: () => {
        alert('Error subiendo la imagen.');
      }
    })
  }

  changeSelectedImage(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input && input.files && input.files.length) {
      this.selectedImage = input.files[0];
    }
  }

  onUsersPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadUsers();
  }

  resetPaginator() {
    this.first = 0;
    this.rows = 5;
  }

  loadUsers() {
    this.userService.getAllUsers(this.first/this.rows, this.rows).subscribe({
      next: (users) => {
        this.usersPage = users;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      }
    })
  }

  loadStats() {
    this.userService.getUsersStats().subscribe({
      next: (stats: StatData[]) => {
        this.rawStats.set(stats);
      },
      error: (err) => console.error('Error loading stats: ', err)
    });
  }

  getStatValue(category: string): number {
    const stat = this.rawStats().find(s => s.category === category);
    return stat ? stat.total : 0;
  }


  clearSelection() {
    this.selectedUser = null;
  }

  confirmAction(action: string) {
    this.confirmationService.confirm({
      header: action,
      message: '¿Estás seguro de que quieres continuar? Esta acción es irreversible.',
      icon: 'pi pi-exclamation-circle',
      rejectButtonProps: {
        label: 'Cancelar',
        icon: 'pi pi-times',
        severity: 'secondary',
        outlined: true,
        size: 'normal'
      },
      acceptButtonProps: {
        label: 'Aceptar',
        icon: 'pi pi-check',
        severity: 'warn',
        size: 'normal'
      },
      accept: () => {
        this.handleGlobalAction(action);
      }
    });
  }

  handleGlobalAction(action: string) {
    switch (action) {
      case 'Desbanear Todos':
      case 'Banear Todos': {
        let bannedState = false;
        if (action === "Banear Todos") {
          bannedState = true;
        }
        this.userService.toggleAllBans(bannedState).subscribe({
          next: () => {
            this.loadUsers();
            this.loadStats();
          }
        });
        break;
      }
      case 'Anonimizar Todos': {
        this.userService.anonAll().subscribe({
          next: () => {
            this.loadUsers();
            this.loadStats();
          }
        });
        break;
      }
      case 'Borrar Todos': {
        this.userService.deleteAll().subscribe({
          next: () => {
            this.selectedUser = null;
            this.loadUsers();
            this.loadStats();
          }
        });
        break;
      }
    }
  }

  handleUserAction(action: string) {
    if(this.selectedUser){
      switch (action) {
        case 'Banear Usuario':
        case 'Desbanear Usuario': {
          let bannedState = false;
          if (action === "Banear Usuario") {
            bannedState = true;
          }
          this.userService.toggleUserBan(this.selectedUser.id, bannedState).subscribe({
            next: (user) => {
              if (user) {
                if (this.selectedUser){
                  this.selectedUser.banned = user.banned;
                  this.loadStats();
                }
              }
            }
          });
          break;
        }
        case 'Anonimizar Usuario': {
          this.userService.anonUser(this.selectedUser.id).subscribe({
            next: (user) => {
              const index = this.usersPage.items.findIndex(u => u.id === this.selectedUser?.id);
              if (index !== -1) {
                this.usersPage.items[index] = user;
                this.selectedUser = user;
                this.loadStats();
              }
            }
          });
          break;
        }
        case 'Borrar Usuario': {
          this.userService.deleteUser(this.selectedUser.id).subscribe({
            next: () => {
              this.selectedUser = null;
              this.loadUsers();
              this.loadStats();
            }
          });
          break;
        }
      }
    }
  }


  initChartOptions() {
    this.options.set({
      plugins: {
        legend: {
          display: false
        }
      }
    });
  }

  protected readonly getUserStatusTagInfo = getUserStatusTagInfo;
  protected readonly getUserRoleTagInfo = getUserRoleTagInfo;
}
