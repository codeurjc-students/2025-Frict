//Formats numbers to adapt them to a country digits system
export function formatPrice(price: number | null | undefined): string {
  if (price == null) return '';

  const isExactPrice = Math.floor(price) === price;
  const fractionDigits = isExactPrice ? 0 : 2;
  const fixed = price.toFixed(fractionDigits);
  const [intPart, decPart] = fixed.split('.');

  const intWithDots = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, '.');

  return decPart ? `${intWithDots},${decPart}€` : `${intWithDots}€`;
}


export function formatRating(rating: number): string {
  if (rating == null) {
    return '0';
  }

  const isExactRating = rating % 1 === 0;

  if (isExactRating) {
    return rating.toLocaleString('es', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    });
  } else {
    return rating.toLocaleString('es', {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1
    });
  }
}
