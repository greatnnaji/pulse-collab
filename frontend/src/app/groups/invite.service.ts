import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { InviteResponse, MemberResponse } from './group.models';

@Injectable({
  providedIn: 'root'
})
export class InviteService {
  private readonly baseUrl = `${environment.apiBaseUrl}/invites`;

  constructor(private readonly http: HttpClient) {}

  getMyInvites(): Observable<InviteResponse[]> {
    return this.http.get<InviteResponse[]>(`${this.baseUrl}/me`);
  }

  acceptInvite(inviteId: number): Observable<MemberResponse> {
    return this.http.post<MemberResponse>(`${this.baseUrl}/${inviteId}/accept`, {});
  }

  declineInvite(inviteId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${inviteId}/decline`, {});
  }
}
