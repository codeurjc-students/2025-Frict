import {Routes} from '@angular/router';
import {routeGuard} from './utils/routeGuard.util';

export const routes: Routes = [

  // ============================================================
  // USER LAYOUT (Navbar + Circuit background + Footer)
  // ============================================================
  {
    path: '',
    loadComponent: () => import('./layouts/user-layout/user-layout.component').then(m => m.UserLayoutComponent),
    children: [

      // --- GROUP 1: COMMON ROUTES ---
      {
        path: '',
        loadComponent: () => import('./components/client/client-home/client-home.component').then(m => m.ClientHomeComponent),
        data: { title: 'Inicio', icon: 'client', breadcrumb: 'Inicio' }
      },
      {
        path: 'category/:id',
        loadComponent: () => import('./components/client/category-info/category-info.component').then(m => m.CategoryInfoComponent),
        data: { title: 'Categoría', icon: 'client', breadcrumb: 'Categoría' }
      },
      {
        path: 'product/:id',
        loadComponent: () => import('./components/client/product-info/product-info.component').then(m => m.ProductInfoComponent),
        data: { title: 'Producto', icon: 'client', breadcrumb: 'Producto' }
      },
      {
        path: 'search',
        loadComponent: () => import('./components/common/search/search.component').then(m => m.SearchComponent),
        data: { title: 'Búsqueda', icon: 'client', breadcrumb: 'Búsqueda' }
      },


      // --- GROUP 2: USER-ONLY ROUTES ---
      {
        path: '',
        canActivateChild: [routeGuard], // Only the users defined in each component will be able to access the route
        children: [
          {
            path: 'order/:id',
            loadComponent: () => import('./components/common/order-details/order-details.component').then(m => m.OrderDetailsComponent),
            data: { roles: ['USER'], title: 'Detalles del pedido', icon: 'client', breadcrumb: 'Pedido' }
          },

          {
            path: 'cart',
            loadComponent: () => import('./components/client/cart/cart.component').then(m => m.CartComponent),
            data: { roles: ['USER'], title: 'Carrito', icon: 'client', breadcrumb: 'Carrito' }
          },

          {
            path: 'summary',
            loadComponent: () => import('./components/client/order-summary/order-summary.component').then(m => m.OrderSummaryComponent),
            data: { roles: ['USER'], title: 'Resumen del pedido', icon: 'client', breadcrumb: 'Resumen' }
          },

          {
            path: 'success',
            loadComponent: () => import('./components/client/order-confirmed/order-confirmed.component').then(m => m.OrderConfirmedComponent),
            data: { roles: ['USER'], title: 'Pedido confirmado', icon: 'client', breadcrumb: 'Completado' }
          },

          {
            path: 'profile',
            loadComponent: () => import('./components/common/profile/profile.component').then(m => m.ProfileComponent),
            data: { roles: ['USER'], title: 'Perfil', icon: 'client', breadcrumb: 'Perfil' }
          }
        ]
      }
    ]
  },


  // ============================================================
  // ADMIN LAYOUT (Navbar + Admin background + Footer)
  // ============================================================
  {
    path: 'admin',
    loadComponent: () => import('./layouts/admin-layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    canActivateChild: [routeGuard], // Users are blocked
    children: [
      // Admin-only components and routes
    ]
  },


  // ============================================================
  // NO LAYOUT (directly on app.component)
  // ============================================================
  {
    path: 'login',
    loadComponent: () => import('./components/common/login/login.component').then(m => m.LoginComponent),
    data: { title: 'Iniciar sesión', icon: 'client', breadcrumb: 'Login' }
  },

  {
    path: 'signup',
    loadComponent: () => import('./components/common/signup/signup.component').then(m => m.SignupComponent),
    data: { title: 'Registro', icon: 'client', breadcrumb: 'Registro' }
  },

  {
    path: 'recover',
    loadComponent: () => import('./components/common/recover-account/recover-account.component').then(m => m.RecoverAccountComponent),
    data: { title: 'Recuperar cuenta', icon: 'client', breadcrumb: 'Recuperación' }
  },

  {
    path: 'reset',
    loadComponent: () => import('./components/common/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
    data: { title: 'Restablecer contraseña', icon: 'client', breadcrumb: 'Restablecer' }
  },

  { path: '**', redirectTo: '' }
];
