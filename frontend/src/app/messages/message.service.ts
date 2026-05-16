import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { MessageResponse, PageResponse } from './message.models';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private readonly baseUrl = `${environment.apiBaseUrl}/groups`;

  constructor(private readonly http: HttpClient) {}

  getMessages(groupId: number, page = 0, size = 20): Observable<PageResponse<MessageResponse>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<MessageResponse>>(`${this.baseUrl}/${groupId}/messages`, { params });
  }
}
