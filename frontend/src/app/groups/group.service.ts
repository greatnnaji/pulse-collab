import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { AddMemberRequest, CreateGroupRequest, GroupResponse, InviteResponse, MemberResponse } from './group.models';

@Injectable({
  providedIn: 'root'
})
export class GroupService {
  private readonly baseUrl = `${environment.apiBaseUrl}/groups`;

  constructor(private readonly http: HttpClient) {}

  getGroups(): Observable<GroupResponse[]> {
    return this.http.get<GroupResponse[]>(this.baseUrl);
  }

  createGroup(payload: CreateGroupRequest): Observable<GroupResponse> {
    return this.http.post<GroupResponse>(this.baseUrl, payload);
  }

  getGroupMembers(groupId: number): Observable<MemberResponse[]> {
    return this.http.get<MemberResponse[]>(`${this.baseUrl}/${groupId}/members`);
  }

  addMemberToGroup(groupId: number, payload: AddMemberRequest): Observable<InviteResponse> {
    return this.http.post<InviteResponse>(`${this.baseUrl}/${groupId}/members`, payload);
  }

  removeMemberFromGroup(groupId: number, memberUserId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${groupId}/members/${memberUserId}`);
  }

  leaveGroup(groupId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${groupId}/members/me`);
  }
}
