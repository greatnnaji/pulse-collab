import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin, map, switchMap, take } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { GroupResponse } from '../groups/group.models';
import { GroupService } from '../groups/group.service';
import { MembersComponent } from '../groups/members/members.component';

@Component({
  selector: 'app-app-shell',
  standalone: true,
  imports: [ReactiveFormsModule, MembersComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss'
})
export class AppShellComponent implements OnInit {
  private readonly themeStorageKey = 'pulse-theme';

  readonly isLoading = signal(true);
  readonly loadingError = signal<string | null>(null);

  readonly groups = signal<GroupResponse[]>([]);
  readonly selectedGroupId = signal<number | null>(null);
  readonly groupAccessError = signal<string | null>(null);
  readonly groupActionNotice = signal<string | null>(null);

  readonly darkMode = signal(false);

  readonly createGroupOpen = signal(false);
  readonly isCreatingGroup = signal(false);
  readonly createGroupError = signal<string | null>(null);

  readonly createGroupForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]]
  });

  readonly currentUser = this.authService.currentUser;

  readonly selectedGroup = computed(() => {
    const selectedId = this.selectedGroupId();
    if (selectedId === null) {
      return null;
    }

    return this.groups().find((group) => group.id === selectedId) ?? null;
  });

  updateMemberCount = (newCount: number): void => {
    const currentGroup = this.selectedGroup();
    if (!currentGroup || currentGroup.memberCount === newCount) {
      return;
    }

    this.groups.update((groups) =>
      groups.map((group) =>
        group.id === currentGroup.id ? { ...group, memberCount: newCount } : group
      )
    );
  };

  constructor(
    private readonly authService: AuthService,
    private readonly groupService: GroupService,
    private readonly formBuilder: FormBuilder,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.initializeTheme();
    this.loadAppData();
  }

  loadAppData(): void {
    this.isLoading.set(true);
    this.loadingError.set(null);
    this.groupAccessError.set(null);
    this.groupActionNotice.set(null);

    forkJoin({
      user: this.authService.getCurrentUser(),
      groups: this.groupService.getGroups()
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ groups }) => {
          this.groups.set(groups);
          this.selectedGroupId.set(null);
        },
        error: () => {
          this.loadingError.set('Unable to load your dashboard. Please refresh and try again.');
        }
      });
  }

  selectGroup(groupId: number): void {
    this.selectedGroupId.set(groupId);
    this.groupAccessError.set(null);
    this.groupActionNotice.set(null);
  }

  toggleTheme(): void {
    const nextValue = !this.darkMode();
    this.darkMode.set(nextValue);
    this.applyTheme(nextValue);
    localStorage.setItem(this.themeStorageKey, nextValue ? 'dark' : 'light');
  }

  openCreateGroup(): void {
    this.createGroupOpen.set(true);
    this.createGroupError.set(null);
  }

  cancelCreateGroup(): void {
    this.createGroupOpen.set(false);
    this.createGroupError.set(null);
    this.createGroupForm.reset({ name: '', description: '' });
  }

  submitCreateGroup(): void {
    this.createGroupError.set(null);

    if (this.createGroupForm.invalid) {
      this.createGroupForm.markAllAsTouched();
      return;
    }

    this.isCreatingGroup.set(true);
    const formValue = this.createGroupForm.getRawValue();
    const payload = {
      name: formValue.name.trim(),
      description: formValue.description.trim() || undefined
    };

    this.groupService
      .createGroup(payload)
      .pipe(
        switchMap((createdGroup) =>
          this.groupService.getGroups().pipe(
            map((groups) => ({ groups, createdGroupId: createdGroup.id }))
          )
        ),
        finalize(() => this.isCreatingGroup.set(false))
      )
      .subscribe({
        next: ({ groups, createdGroupId }) => {
          this.groups.set(groups);
          this.selectedGroupId.set(createdGroupId);
          this.groupAccessError.set(null);
          this.cancelCreateGroup();
        },
        error: () => {
          this.createGroupError.set('Could not create group. Please review the details and try again.');
        }
      });
  }

  logout(): void {
    this.authService.clearSession();
    this.router.navigateByUrl('/auth/login');
  }

  hasCreateGroupError(controlName: 'name' | 'description'): boolean {
    const control = this.createGroupForm.controls[controlName];
    return control.invalid && control.touched;
  }

  onGroupAccessDenied(message: string): void {
    this.selectedGroupId.set(null);
    this.groupAccessError.set(message);
  }

  onGroupLeft(message: string): void {
    this.selectedGroupId.set(null);
    this.groupAccessError.set(null);
    this.groupActionNotice.set(message);

    this.groupService
      .getGroups()
      .pipe(take(1))
      .subscribe({
        next: (groups) => {
          this.groups.set(groups);
        },
        error: () => {
          this.groupActionNotice.set('You left the group, but we could not refresh your group list yet.');
        }
      });
  }

  private initializeTheme(): void {
    const savedTheme = localStorage.getItem(this.themeStorageKey);
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const useDark = savedTheme ? savedTheme === 'dark' : prefersDark;

    this.darkMode.set(useDark);
    this.applyTheme(useDark);
  }

  private applyTheme(isDark: boolean): void {
    document.body.classList.toggle('dark-theme', isDark);
  }
}
