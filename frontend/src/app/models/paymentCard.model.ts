export interface PaymentCard {
  id: string;
  alias: string;
  cardOwnerName: string;
  number: string;
  numberEnding: string;
  cvv: string;
  dueDate: string;
}
