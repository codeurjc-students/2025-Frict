export interface Connection {
  online: boolean;
  lastConnection: string | null;
  lastSessionDurationSeconds: number;
}
