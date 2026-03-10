import {LogEntry} from './logEntry.model';

export interface TruckStatusLog {
  id: string;
  icon: string;
  status: string;
  updates: LogEntry[];
}
