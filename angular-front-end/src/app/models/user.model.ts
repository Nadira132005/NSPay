export type UserRole = 'Admin' | 'User' | 'Analyst';

export interface UserModel {
    id?: number;
    name: string;
    password: string;
    role?: UserRole;
}

export interface LoginResponseModel {
    token: string;
    userId: number;
    role: UserRole;
    name: string;
}
