export interface LoginResponse {
  user: {
    id: number;
    email: string;
    role: string;
  };
  token: string;
}
