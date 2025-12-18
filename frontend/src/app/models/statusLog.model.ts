import {LogEntry} from './logEntry.model';

export interface StatusLog {
  id: string;
  icon: string;
  status: string;
  updates: LogEntry[];
}
