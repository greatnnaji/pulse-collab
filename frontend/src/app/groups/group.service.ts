import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { CreateGroupRequest, GroupResponse } from './group.models';

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
}
