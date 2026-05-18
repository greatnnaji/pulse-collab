import { Component, OnInit, signal, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MessageService } from './message.service';
import { MessageResponse } from './message.models';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-message-thread',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './message-thread.component.html',
  styleUrls: ['./message-thread.component.scss']
})
export class MessageThreadComponent implements OnInit, AfterViewInit {
  readonly messages = signal<MessageResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  private groupId = 0;
  readonly newMessage = signal('');
  readonly sending = signal(false);
  private currentUserId: number | null = null;

  @ViewChild('scrollContainer', { static: false }) scrollContainer?: ElementRef<HTMLDivElement>;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly messageService: MessageService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('groupId');
    this.groupId = param ? Number(param) : 0;
    this.loadMessages();
    const user = this.authService.currentUser();
    this.currentUserId = user ? user.id : null;
  }

  ngAfterViewInit(): void {
    // ensure we scroll after initial render
    setTimeout(() => this.scrollToBottom(), 0);
  }

  loadMessages(): void {
    if (!this.groupId) {
      this.error.set('Missing group id');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.messageService.getMessages(this.groupId, 0, 50).subscribe({
      next: (page) => {
        // normalize ordering (oldest -> newest)
        const sorted = page.content.slice().sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
        this.messages.set(sorted);
        this.loading.set(false);
        setTimeout(() => this.scrollToBottom(), 0);
      },
      error: () => {
        this.error.set('Unable to load messages');
        this.loading.set(false);
      }
    });
  }

  send(): void {
    const content = this.newMessage().trim();
    if (!content || this.sending()) {
      return;
    }

    this.sending.set(true);
    this.error.set(null);

    const tempId = -Date.now();
    const tempMessage: MessageResponse = {
      id: tempId,
      groupId: this.groupId,
      senderId: this.currentUserId ?? -1,
      senderUsername: 'You',
      content,
      createdAt: new Date().toISOString()
    };

    // optimistic append at end (oldest -> newest ordering)
    this.messages.update((list) => [...list, tempMessage]);
    this.newMessage.set('');

    setTimeout(() => this.scrollToBottom(), 0);

    this.messageService.createMessage(this.groupId, { content }).subscribe({
      next: (created) => {
        // replace temp message with server-provided message
        this.messages.update((list) => list.map((m) => (m.id === tempId ? created : m)));
        this.sending.set(false);
        setTimeout(() => this.scrollToBottom(), 0);
      },
      error: () => {
        // remove temp message and show error
        this.messages.update((list) => list.filter((m) => m.id !== tempId));
        this.error.set('Failed to send message');
        this.sending.set(false);
      }
    });
  }

  private scrollToBottom(): void {
    try {
      const el = this.scrollContainer?.nativeElement;
      if (!el) return;
      el.scrollTop = el.scrollHeight;
    } catch (e) {
      // ignore
    }
  }

  isMine(m: MessageResponse): boolean {
    return !!(this.currentUserId && m.senderId === this.currentUserId) || m.senderUsername === 'You';
  }

  formatTime(iso: string): string {
    try {
      const d = new Date(iso);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
    } catch (e) {
      return iso;
    }
  }

  nameColor(senderId: number | null | undefined): string {
    const colors = ['#E06C75', '#98C379', '#E5C07B', '#61AFEF', '#C678DD', '#56B6C2'];
    const idStr = String(senderId ?? '0');
    let hash = 0;
    for (let i = 0; i < idStr.length; i++) {
      hash = (hash * 31 + idStr.charCodeAt(i)) >>> 0;
    }
    return colors[hash % colors.length];
  }

  shouldShowSender(index: number, m: MessageResponse): boolean {
    if (index === 0) return true;
    const prev = this.messages()[index - 1];
    return !!prev && prev.senderId !== m.senderId;
  }

  isFirstInGroup(index: number, m: MessageResponse): boolean {
    return this.shouldShowSender(index, m);
  }

  gapFor(index: number, m: MessageResponse): number {
    return this.shouldShowSender(index, m) ? 8 : 2;
  }
}
