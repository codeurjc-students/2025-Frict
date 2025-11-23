import { Routes } from '@angular/router';
import {LoginComponent} from './components/common/login/login.component';
import {SignupComponent} from './components/common/signup/signup.component';
import {SearchComponent} from './components/common/search/search.component';
import {ProductInfoComponent} from './components/client/product-info/product-info.component';


const icons: Record<'client' | 'admin', string> = {
  client: '/shopLogo.png',
  admin: '/frictLogo.png'
};

export const routes: Routes = [
  //Client side routes (anon users, registered users and store managers)
  //ClientHomeComponent is imported this way in order to avoid circular references between the component and the router (as ClientHomeComponent needs routerLink, but router needs ClientHomeComponent)
  {path: '', loadComponent: () => import('./components/client/client-home/client-home.component').then(m => m.ClientHomeComponent), data: { title: 'Producto - MiTienda', icon: icons.client }},
  { path: 'product/:id', component: ProductInfoComponent, data: { title: 'Producto - MiTienda', icon: icons.client } },
  { path: 'search', component: SearchComponent, data: { title: 'Búsqueda - MiTienda', icon: icons.client } },


  //Common routes
  { path: 'login', component: LoginComponent, data: { title: 'Iniciar sesión - MiTienda', icon: icons.client } },
  { path: 'signup', component: SignupComponent, data: { title: 'Registro - MiTienda', icon: icons.client } },

  //Inexistent routes
  {path: '**', redirectTo: ''},
];
