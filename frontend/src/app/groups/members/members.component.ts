import { Component, Input, OnInit, signal } from '@angular/core';
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
export class MembersComponent implements OnInit {
  @Input() selectedGroup: GroupResponse | null = null;
  @Input() currentUserId: number | null = null;
  @Input() createdMemberCount: ((count: number) => void) | null = null;

  readonly members = signal<MemberResponse[]>([]);
  readonly isLoadingMembers = signal(true);
  readonly membersError = signal<string | null>(null);

  readonly showAddMemberForm = signal(false);
  readonly isAddingMember = signal(false);
  readonly addMemberError = signal<string | null>(null);
  readonly addMemberSuccess = signal(false);

  readonly addMemberForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  readonly isGroupCreator = () =>
    this.selectedGroup && this.currentUserId ? this.selectedGroup.createdBy === this.currentUserId : false;

  constructor(
    private readonly groupService: GroupService,
    private readonly formBuilder: FormBuilder
  ) {}

  ngOnInit(): void {
    if (this.selectedGroup) {
      this.loadMembers();
    }
  }

  loadMembers(): void {
    if (!this.selectedGroup) {
      return;
    }

    this.isLoadingMembers.set(true);
    this.membersError.set(null);

    this.groupService
      .getGroupMembers(this.selectedGroup.id)
      .pipe(finalize(() => this.isLoadingMembers.set(false)))
      .subscribe({
        next: (members) => {
          this.members.set(members);
          if (this.createdMemberCount) {
            this.createdMemberCount(members.length);
          }
        },
        error: () => {
          this.membersError.set('Unable to load members. Please try again.');
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
        next: (newMember) => {
          this.members.update((members) => [...members, newMember]);
          if (this.createdMemberCount) {
            this.createdMemberCount(this.members().length);
          }
          this.addMemberSuccess.set(true);
          setTimeout(() => {
            this.cancelAddMember();
          }, 1500);
        },
        error: (error) => {
          if (error.status === 404) {
            this.addMemberError.set('User with this email not found.');
          } else if (error.status === 409) {
            this.addMemberError.set('User is already a member of this group.');
          } else if (error.status === 403) {
            this.addMemberError.set('Only the group creator can add members.');
          } else {
            this.addMemberError.set('Could not add member. Please try again.');
          }
        }
      });
  }

  hasAddMemberError(controlName: 'email'): boolean {
    const control = this.addMemberForm.controls[controlName];
    return control.invalid && control.touched;
  }
}
