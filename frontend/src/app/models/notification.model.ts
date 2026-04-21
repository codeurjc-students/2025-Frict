export interface Notification {
  id: string;
  subject: string;
  description: string;
  timestamp: string;
  read: boolean;

  type: string;
}
