import {LogEntry} from './logEntry.model';

export interface OrderStatusLog {
  id: string;
  icon: string;
  status: string;
  updates: LogEntry[];
}
