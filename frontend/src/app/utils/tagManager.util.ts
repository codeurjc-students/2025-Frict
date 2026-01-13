//USER STATUS TAGS


//STOCK TAGS
export function getStockMessage(units: number): string {
  if (units <= 10 && units > 5) {
    return 'Quedan ' + units;
  } else if (units <= 5 && units > 0) {
    return '¡Quedan ' + units + '!';
  } else {
    return 'Agotado';
  }
}

export function getStockIcon(units: number): string {
  if (units <= 10 && units > 5) {
    return 'pi pi-info-circle';
  } else if (units <= 5 && units > 0) {
    return 'pi pi-exclamation-triangle';
  } else {
    return 'pi pi-times';
  }
}

export function getStockSeverity(units: number): string {
  if (units <= 10 && units > 5) {
    return 'info';
  } else if (units <= 5 && units > 0) {
    return 'warn';
  } else {
    return 'danger';
  }
}
