import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AccountModel } from '../models/account-model';
import { UserModel } from '../models/user.model';

export interface Pocket { id: number; name: string; balance: number; targetAmount: number; }
export interface ScheduledTransfer { id: number; recipientIban: string; amount: number; scheduledAt: string; status: string; failureReason?: string; }
export interface TradeOrder { id: number; symbol: string; side: string; quantity: number; price: number; total: number; createdAt: string; }
export interface Quote { symbol: string; name: string; price: number; change: number; simulated: boolean; }
export interface Credit { id: number; userId: number; amount: number; monthlyIncome: number; termMonths: number; purpose: string; status: string; reviewNote?: string; createdAt: string; }
export interface DocumentRecord { id: number; applicationId?: number; kind: string; filename: string; scanStatus: string; extractedText?: string; }
export interface Profile { email: string; phone: string; country: string; dateOfBirth: string; occupation: string; onboardingStatus?: string; }
export interface Activity { id: number; amount: number; transactionType: string; counterpartyIban?: string; returnCode: string; timestamp: string; }
export interface Risk { id?: number; userId: number; amount: number; recipientIban: string; score: number; level: string; reasons: string; historySamples: number; modelVersion: string; status: string; createdAt?: string; }
export interface Overview { user: UserModel; account: AccountModel; currency: string; pockets: Pocket[]; scheduled: ScheduledTransfer[]; trades: TradeOrder[]; quotes: Quote[]; credits: Credit[]; documents: DocumentRecord[]; profile: Profile | null; transactions: Activity[]; }

@Injectable({ providedIn: 'root' })
export class FinanceApiService {
  constructor(private http: HttpClient) {}
  overview(): Observable<Overview> { return this.http.get<Overview>('/api/finance/overview'); }
  post<T>(path: string, body: unknown = {}): Observable<T> { return this.http.post<T>('/api/finance/' + path, body); }
  remove(path: string): Observable<void> { return this.http.delete<void>('/api/finance/' + path); }
  profile(body: Profile): Observable<Profile> { return this.http.put<Profile>('/api/finance/profile', body); }
  risk(body: unknown): Observable<Risk> { return this.post<Risk>('risk-preview', body); }
  risks(): Observable<Risk[]> { return this.http.get<Risk[]>('/api/finance/risk-assessments'); }
  reviews(): Observable<{risks: Risk[]; credits: Credit[]}> { return this.http.get<{risks: Risk[]; credits: Credit[]}>('/api/finance/reviews'); }
  creditDocuments(id: number): Observable<DocumentRecord[]> { return this.http.get<DocumentRecord[]>('/api/finance/credit/' + id + '/documents'); }
  upload(file: File, kind: string, applicationId?: number): Observable<DocumentRecord> {
    const body = new FormData(); body.append('file', file); body.append('kind', kind);
    if (applicationId != null) body.append('applicationId', String(applicationId));
    return this.post<DocumentRecord>('documents', body);
  }
}
