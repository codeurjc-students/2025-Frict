import { Routes } from '@angular/router';
import {HomeComponent} from './components/home/home.component';
import {LoginComponent} from './components/login/login.component';
import {ProfileComponent} from './components/profile/profile.component';
import {ShopsComponent} from './components/shops/shops.component';
import {DeliveryComponent} from './components/delivery/delivery.component';
import {ReportsComponent} from './components/reports/reports.component';
import {UsersComponent} from './components/users/users.component';

export const routes: Routes = [
  {path: '', component: HomeComponent}, //La ruta '' será la que se muestre por defecto en el router cuando se inicia la aplicación. Si no se define, el router no mostrará nada dentro de su componente
  {path: 'login', component: LoginComponent},
  {path: 'profile', component: ProfileComponent},
  {path: 'shops', component: ShopsComponent},
  {path: 'delivery', component: DeliveryComponent},
  {path: 'reports', component: ReportsComponent},
  {path: 'users', component: UsersComponent},
  {path: '**', redirectTo: 'login'},
];
