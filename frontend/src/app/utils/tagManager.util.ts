
export interface TagInformation {
  message: string;
  icon: string;
  severity: 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';
}

//USER ROLE TAGS
export function getUserRoleTagInfo(role: string): TagInformation {
  if (role === 'ADMIN') {
    return {
      message: `ADMINISTRADOR`,
      icon: 'pi pi-angle-double-up',
      severity: 'danger'
    };
  } else if (role === 'MANAGER') {
    return {
      message: `GERENTE`,
      icon: 'pi pi-angle-up',
      severity: 'warn'
    };
  } else if (role === 'DRIVER') {
    return {
      message: 'CONDUCTOR',
      icon: 'pi pi-truck',
      severity: 'info'
    };
  } else {
    return {
      message: 'USUARIO',
      icon: 'pi pi-user',
      severity: 'secondary'
    };
  }
}

//USER STATUS TAGS
export function getUserStatusTagInfo(logged: boolean, banned: boolean, deleted: boolean): TagInformation {
  if (deleted) {
    return {
      message: `ELIMINADO`,
      icon: 'pi pi-trash',
      severity: 'warn'
    };
  } else if (banned) {
    return {
      message: `BANEADO`,
      icon: 'pi pi-ban',
      severity: 'danger'
    };
  } else if (logged) {
    return {
      message: 'CONECTADO',
      icon: 'pi pi-circle-on',
      severity: 'success'
    };
  } else {
    return {
      message: 'DESCONECTADO',
      icon: 'pi pi-circle-off',
      severity: 'secondary'
    };
  }
}


export function getStockTagInfo(units: number | null | undefined, localMode: boolean): TagInformation | null {
  if (units === null || units === undefined || units > 10) {
    return null;
  }

  const prefix = localMode ? 'Local' : 'Global';

  if (units < 0) {
    return {
      message: `${prefix}: No disponible`,
      icon: 'pi pi-ban',
      severity: 'secondary'
    };
  }

  if (units > 5 && units <= 10) {
    return {
      message: `${prefix}: ${units} uds.`,
      icon: 'pi pi-info-circle',
      severity: 'info'
    };
  } else if (units > 0 && units <= 5) {
    return {
      message: `${prefix}: ${units} uds.`,
      icon: 'pi pi-exclamation-triangle',
      severity: 'warn'
    };
  } else {
    // Units = 0
    return {
      message: `${prefix}: Agotado`,
      icon: 'pi pi-times',
      severity: 'danger'
    };
  }
}

// ORDER STATUS TAGS
export function getOrderStatusTagInfo(status: string): TagInformation {
  switch (status) {
    case 'Pedido Realizado': return { message: 'Pedido Realizado', icon: 'pi pi-shopping-cart', severity: 'info' };
    case 'Enviado':          return { message: 'Enviado',          icon: 'pi pi-box',            severity: 'info' };
    case 'En Reparto':       return { message: 'En Reparto',       icon: 'pi pi-truck',          severity: 'warn' };
    case 'Completado':       return { message: 'Completado',       icon: 'pi pi-check',          severity: 'success' };
    case 'Cancelado':        return { message: 'Cancelado',        icon: 'pi pi-times',          severity: 'danger' };
    default:                 return { message: status,             icon: 'pi pi-info-circle',    severity: 'secondary' };
  }
}

// TRUCK HISTORY STATUS TAGS
export function getTruckHistoryStatusTagInfo(status: string): TagInformation {
  switch (status) {
    case 'Descanso':            return { message: 'Descanso',            icon: 'pi pi-moon',         severity: 'success' };
    case 'En ruta a la tienda': return { message: 'En ruta a la tienda', icon: 'pi pi-map-marker',   severity: 'info' };
    case 'En Reparto':          return { message: 'En Reparto',          icon: 'pi pi-send',         severity: 'warn' };
    case 'Fuera de servicio':   return { message: 'Fuera de servicio',   icon: 'pi pi-times-circle', severity: 'danger' };
    default:                    return { message: status || 'Desconocido', icon: 'pi pi-info-circle', severity: 'secondary' };
  }
}

// STOCK LEVEL TAGS
export function getStockLevelTagInfo(units: number): TagInformation {
  if (units < 0)  return { message: 'No disponible', icon: 'pi pi-ban',                  severity: 'secondary' };
  if (units > 20) return { message: `${units} uds.`, icon: 'pi pi-box',                  severity: 'success' };
  if (units > 5)  return { message: `${units} uds.`, icon: 'pi pi-exclamation-triangle', severity: 'warn' };
  if (units > 0)  return { message: `${units} uds.`, icon: 'pi pi-exclamation-triangle', severity: 'danger' };
  return           { message: 'Agotado',              icon: 'pi pi-times',                severity: 'danger' };
}

export function getOrderStatusColorClass(status: string): string {
  const m: Record<string, string> = {
    'Pedido Realizado': 'text-blue-500', 'Enviado': 'text-purple-500',
    'En Reparto': 'text-orange-500', 'Completado': 'text-green-500', 'Cancelado': 'text-red-500'
  };
  return m[status] || 'text-slate-400';
}

export function getOrderStatusBgColorClass(status: string): string {
  const m: Record<string, string> = {
    'Pedido Realizado': 'bg-blue-100 text-blue-700', 'Enviado': 'bg-purple-100 text-purple-700',
    'En Reparto': 'bg-orange-100 text-orange-700', 'Completado': 'bg-green-100 text-green-700',
    'Cancelado': 'bg-red-100 text-red-700'
  };
  return m[status] || 'bg-slate-100 text-slate-700';
}
