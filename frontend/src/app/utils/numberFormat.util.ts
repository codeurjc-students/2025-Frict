//Formats numbers to adapt them to a country digits system
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
