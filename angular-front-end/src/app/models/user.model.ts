export type UserRole = 'Admin' | 'User' | 'Analyst';

export interface UserModel {
    id?: number;
    name: string;
    password: string;
    firstName?: string;
    lastName?: string;
    role?: UserRole;
    accountId?: number | null;
}

export interface LoginResponseModel {
    token: string;
    userId: number;
    role: UserRole;
    name: string;
    passwordChangeRequired: boolean;
    accountCardNumber?: string | null;
}
