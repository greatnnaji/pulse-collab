import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { GroupService } from '../group.service';
import { AddMemberRequest, GroupResponse, MemberResponse } from '../group.models';

@Component({
  selector: 'app-members',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './members.component.html',
  styleUrl: './members.component.scss'
})
export class MembersComponent implements OnChanges {
  @Input() selectedGroup: GroupResponse | null = null;
  @Input() currentUserId: number | null = null;
  @Input() createdMemberCount: ((count: number) => void) | null = null;
  @Output() accessDenied = new EventEmitter<string>();
  @Output() groupLeft = new EventEmitter<string>();

  readonly members = signal<MemberResponse[]>([]);
  readonly isLoadingMembers = signal(true);
  readonly membersError = signal<string | null>(null);

  readonly showAddMemberForm = signal(false);
  readonly isAddingMember = signal(false);
  readonly addMemberError = signal<string | null>(null);
  readonly addMemberSuccess = signal(false);

  readonly memberActionError = signal<string | null>(null);
  readonly memberActionSuccess = signal<string | null>(null);
  readonly isRemovingMemberId = signal<number | null>(null);
  readonly isLeavingGroup = signal(false);

  readonly addMemberForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  readonly canManageMembers = () => {
    const currentUserRole = this.getCurrentUserRole();
    return currentUserRole === 'OWNER' || currentUserRole === 'ADMIN';
  };

  readonly canCurrentUserLeaveGroup = () => {
    const currentUserRole = this.getCurrentUserRole();
    return currentUserRole === 'ADMIN' || currentUserRole === 'MEMBER';
  };

  readonly isCurrentUserOwner = () => this.getCurrentUserRole() === 'OWNER';

  constructor(
    private readonly groupService: GroupService,
    private readonly formBuilder: FormBuilder
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['selectedGroup']) {
      const previousGroup = changes['selectedGroup'].previousValue as GroupResponse | null;
      const currentGroup = changes['selectedGroup'].currentValue as GroupResponse | null;

      if (previousGroup?.id === currentGroup?.id) {
        return;
      }

      if (!currentGroup) {
        this.members.set([]);
        this.membersError.set(null);
        this.memberActionError.set(null);
        this.memberActionSuccess.set(null);
        this.isLoadingMembers.set(false);
        return;
      }

      this.loadMembers();
    }
  }

  loadMembers(): void {
    if (!this.selectedGroup) {
      return;
    }

    const loadingGroupId = this.selectedGroup.id;

    this.isLoadingMembers.set(true);
    this.membersError.set(null);
    this.memberActionError.set(null);
    this.memberActionSuccess.set(null);

    this.groupService
      .getGroupMembers(loadingGroupId)
      .pipe(finalize(() => this.isLoadingMembers.set(false)))
      .subscribe({
        next: (members) => {
          if (this.selectedGroup?.id !== loadingGroupId) {
            return;
          }

          this.members.set(members);
          if (this.createdMemberCount && this.selectedGroup.memberCount !== members.length) {
            this.createdMemberCount(members.length);
          }
        },
        error: (error) => {
          if (this.selectedGroup?.id !== loadingGroupId) {
            return;
          }

          if (error?.status === 404) {
            this.members.set([]);
            if (this.createdMemberCount && this.selectedGroup.memberCount !== 0) {
              this.createdMemberCount(0);
            }
            return;
          }

          if (error?.status === 403) {
            const message = 'You are not a member of this private group.';
            this.membersError.set(message);
            this.accessDenied.emit(message);
            return;
          }

          this.membersError.set('Unable to load members. Please try again.');
        }
      });
  }

  canRemoveMember(member: MemberResponse): boolean {
    if (!this.currentUserId || member.userId === this.currentUserId) {
      return false;
    }

    if (!this.canManageMembers()) {
      return false;
    }

    return member.role !== 'OWNER';
  }

  canCurrentUserLeave(member: MemberResponse): boolean {
    if (!this.currentUserId || member.userId !== this.currentUserId) {
      return false;
    }

    return this.canCurrentUserLeaveGroup();
  }

  removeMember(member: MemberResponse): void {
    if (!this.selectedGroup || !this.canRemoveMember(member)) {
      return;
    }

    const confirmed = window.confirm(
      `Remove ${member.displayName || member.username} from ${this.selectedGroup.name}?`
    );
    if (!confirmed) {
      return;
    }

    this.memberActionError.set(null);
    this.memberActionSuccess.set(null);
    this.isRemovingMemberId.set(member.id);

    this.groupService
      .removeMemberFromGroup(this.selectedGroup.id, member.userId)
      .pipe(finalize(() => this.isRemovingMemberId.set(null)))
      .subscribe({
        next: () => {
          this.members.update((members) => members.filter((candidate) => candidate.id !== member.id));
          if (this.createdMemberCount) {
            this.createdMemberCount(this.members().length);
          }
          this.memberActionSuccess.set(`${member.displayName || member.username} was removed.`);
        },
        error: (error) => {
          if (error?.status === 403) {
            this.memberActionError.set('You do not have permission to remove this member.');
          } else if (error?.status === 404) {
            this.memberActionError.set('Group or member was not found.');
          } else if (error?.status === 400) {
            this.memberActionError.set('This member cannot be removed.');
          } else {
            this.memberActionError.set('Could not remove member. Please try again.');
          }
        }
      });
  }

  leaveCurrentGroup(member: MemberResponse): void {
    if (!this.selectedGroup || !this.canCurrentUserLeave(member)) {
      return;
    }

    const confirmed = window.confirm(`Leave ${this.selectedGroup.name}?`);
    if (!confirmed) {
      return;
    }

    this.memberActionError.set(null);
    this.memberActionSuccess.set(null);
    this.isLeavingGroup.set(true);

    this.groupService
      .leaveGroup(this.selectedGroup.id)
      .pipe(finalize(() => this.isLeavingGroup.set(false)))
      .subscribe({
        next: () => {
          this.groupLeft.emit(`You left ${this.selectedGroup?.name ?? 'the group'}.`);
        },
        error: (error) => {
          if (error?.status === 403) {
            this.memberActionError.set('You are not a member of this group.');
          } else if (error?.status === 400) {
            this.memberActionError.set('Owners must transfer ownership before leaving.');
          } else if (error?.status === 404) {
            this.memberActionError.set('This group no longer exists.');
          } else {
            this.memberActionError.set('Could not leave group. Please try again.');
          }
        }
      });
  }

  openAddMemberForm(): void {
    this.showAddMemberForm.set(true);
    this.addMemberError.set(null);
    this.addMemberSuccess.set(false);
  }

  cancelAddMember(): void {
    this.showAddMemberForm.set(false);
    this.addMemberError.set(null);
    this.addMemberSuccess.set(false);
    this.addMemberForm.reset({ email: '' });
  }

  submitAddMember(): void {
    this.addMemberError.set(null);

    if (this.addMemberForm.invalid) {
      this.addMemberForm.markAllAsTouched();
      return;
    }

    if (!this.selectedGroup) {
      return;
    }

    this.isAddingMember.set(true);
    const formValue = this.addMemberForm.getRawValue();
    const payload: AddMemberRequest = {
      email: formValue.email.trim()
    };

    this.groupService
      .addMemberToGroup(this.selectedGroup.id, payload)
      .pipe(finalize(() => this.isAddingMember.set(false)))
      .subscribe({
        next: () => {
          this.addMemberSuccess.set(true);
          setTimeout(() => {
            this.cancelAddMember();
          }, 1500);
        },
        error: (error) => {
          if (error.status === 404) {
            this.addMemberError.set('User with this email not found.');
          } else if (error.status === 409) {
            this.addMemberError.set(
              error.error?.message?.includes('already been invited')
                ? 'This user has already been invited.'
                : 'User is already a member of this group.'
            );
          } else if (error.status === 403) {
            this.addMemberError.set('Only group owner or admin can invite members.');
          } else {
            this.addMemberError.set('Could not send invite. Please try again.');
          }
        }
      });
  }

  hasAddMemberError(controlName: 'email'): boolean {
    const control = this.addMemberForm.controls[controlName];
    return control.invalid && control.touched;
  }

  private getCurrentUserRole(): MemberResponse['role'] | null {
    if (!this.currentUserId) {
      return null;
    }

    return this.members().find((member) => member.userId === this.currentUserId)?.role ?? null;
  }
}
