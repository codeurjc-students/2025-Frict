
export function formatPrice(price: number): string {
  if (price == null) {
    return '';
  }

  const isExactPrice = price % 1 === 0;

  if (isExactPrice) {
    return price.toLocaleString('es', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }) + '€';
  } else {
    return price.toLocaleString('es', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }) + '€';
  }
}
