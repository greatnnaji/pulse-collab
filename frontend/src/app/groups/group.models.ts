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

export type MemberRole = 'ADMIN' | 'MEMBER';

export interface MemberResponse {
  id: number;
  userId: number;
  username: string;
  email: string;
  displayName: string | null;
  avatarUrl: string | null;
  role: MemberRole;
  joinedAt: string;
}

export interface AddMemberRequest {
  email: string;
}
