export interface MessageResponse {
  id: number;
  groupId: number;
  senderId: number;
  senderUsername?: string;
  content: string;
  createdAt: string; // ISO
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages?: number;
  number: number;
  size: number;
}
