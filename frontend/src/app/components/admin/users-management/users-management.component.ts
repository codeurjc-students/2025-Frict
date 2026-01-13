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
import {ConfirmationService} from 'primeng/api';
import {UIChart} from 'primeng/chart';
import {StatData} from '../../../utils/statData.model';

@Component({
  selector: 'app-users-management',
  standalone: true,
  imports: [
    CommonModule, Button, Tag, TableModule, Avatar, Tooltip, Paginator, UIChart
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

  constructor(private userService: UserService,
              private confirmationService: ConfirmationService) {
  }

  ngOnInit() {
    this.initChartOptions();
    this.loadUsers();
    this.loadStats();
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
        console.log(this.usersPage);
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
        console.log(this.rawStats());
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
    console.log("Ejecutando acción grobal: " + action);
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
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--text-color');

    this.options.set({
      plugins: {
        legend: {
          display: false
        }
      }
    });
  }

}
