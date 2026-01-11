import { Component, OnInit } from '@angular/core';
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
import {ConfirmationService} from 'primeng/api';

@Component({
  selector: 'app-users-management',
  standalone: true,
  imports: [
    CommonModule, Button, Tag, TableModule, Avatar, Tooltip, Paginator
  ],
  templateUrl: './users-management.component.html',
  styleUrl: 'users-management.component.css'
})
export class UsersManagementComponent implements OnInit {

  usersPage: PageResponse<User> = { items: [], totalItems: 0, currentPage: 0, lastPage: -1, pageSize: 0};
  selectedUser: User | null = null;

  activeUsers: number = 0;
  bannedUsers: number = 0;

  first: number = 0;
  rows: number = 5;

  loading: boolean = true;
  error: boolean = false;

  constructor(private userService: UserService,
              private confirmationService: ConfirmationService) {}

  ngOnInit() {
    this.loadUsers();
  }

  onUsersPageChange(event: PaginatorState) {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadUsers();
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

  // --- Actions ---

  clearSelection() {
    this.selectedUser = null;
  }

  confirmAction(action: string) {
    this.confirmationService.confirm({
      header: 'Confirmation',
      message: '¿Estás seguro de que quieres continuar?',
      icon: 'pi pi-exclamation-circle',
      rejectButtonProps: {
        label: 'Cancel',
        icon: 'pi pi-times',
        outlined: true,
        size: 'normal'
      },
      acceptButtonProps: {
        label: 'Save',
        icon: 'pi pi-check',
        size: 'normal'
      },
      accept: () => {
        this.handleUserAction(action);
      }
    });
  }

  handleUserAction(action: string) {
    console.log(`Ejecutando acción de usuario: ${action}`);
  }

  handleGlobalAction(action: string) {
    console.log(`Ejecutando acción global: ${action}`);
  }

}
