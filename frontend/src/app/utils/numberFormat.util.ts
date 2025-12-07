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

export function formatDueDate(isoDate: string | undefined): string { //Input: 2028-05, Output: 05/28
  if (isoDate){
    const [year, month] = isoDate.split('-');
    return `${month}/${year.slice(-2)}`; // year.slice(-2) takes the two last characters of the year (2028 -> 28)
  }
  return '';
}
