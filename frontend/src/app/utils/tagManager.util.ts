
export interface TagInformation {
  message: string;
  icon: string;
  severity: 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';
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
    // Si units es 0 o menor
    return {
      message: `${prefix}: Agotado`,
      icon: 'pi pi-times',
      severity: 'danger'
    };
  }
}


//TRUCK STATUS TAGS
export function getTruckStatusTagInfo(activeOrders: number, logged: boolean): TagInformation {
  if (activeOrders > 0){
    if (logged){
      return {
        message: `En servicio`,
        icon: 'pi pi-truck',
        severity: 'warn'
      };
    }
    else {
      return {
        message: `Ausente`,
        icon: 'pi pi-clock',
        severity: 'info'
      };
    }
  }
  else {
    return {
      message: `Libre`,
      icon: 'pi pi-verified',
      severity: 'success'
    };
  }
}
