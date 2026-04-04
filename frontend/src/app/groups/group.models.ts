export interface GroupResponse {
  id: number;
  name: string;
  description: string | null;
  avatarUrl: string | null;
  createdBy: number;
  createdAt: string;
  memberCount: number | null;
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
  avatarUrl?: string;
}
