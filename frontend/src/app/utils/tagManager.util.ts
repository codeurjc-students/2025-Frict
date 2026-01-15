export interface TagInformation {
  message: string;
  icon: string;
  severity: 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast'; // Tipos de PrimeNG/Tailwind
}

//USER ROLE TAGS
export function getUserRoleTagInfo(role: string): TagInformation {
  if (role === 'ADMIN') {
    return {
      message: `ADMIN`,
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
      message: 'REPARTO',
      icon: 'pi pi-truck',
      severity: 'contrast'
    };
  } else {
    return {
      message: 'USUARIO',
      icon: 'pi pi-user',
      severity: 'info'
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


//STOCK TAGS
export function getStockTagInfo(units: number): TagInformation {
  if (units > 5 && units <= 10) {
    return {
      message: `Quedan ${units}`,
      icon: 'pi pi-info-circle',
      severity: 'info'
    };
  } else if (units > 0 && units <= 5) {
    return {
      message: `¡Quedan ${units}!`,
      icon: 'pi pi-exclamation-triangle',
      severity: 'warn'
    };
  } else {
    return {
      message: 'Agotado',
      icon: 'pi pi-times',
      severity: 'danger'
    };
  }
}
