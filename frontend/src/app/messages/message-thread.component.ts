import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MessageService } from './message.service';
import { MessageResponse } from './message.models';

@Component({
  selector: 'app-message-thread',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './message-thread.component.html',
  styleUrls: ['./message-thread.component.scss']
})
export class MessageThreadComponent implements OnInit {
  readonly messages = signal<MessageResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  private groupId = 0;
  readonly newMessage = signal('');
  readonly sending = signal(false);

  constructor(private readonly route: ActivatedRoute, private readonly messageService: MessageService) {}

  ngOnInit(): void {
    const param = this.route.snapshot.paramMap.get('groupId');
    this.groupId = param ? Number(param) : 0;
    this.loadMessages();
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
        this.messages.set(page.content);
        this.loading.set(false);
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
      senderId: -1,
      senderUsername: 'You',
      content,
      createdAt: new Date().toISOString()
    };

    // optimistic insert at top
    this.messages.update((list) => [tempMessage, ...list]);
    this.newMessage.set('');

    this.messageService.createMessage(this.groupId, { content }).subscribe({
      next: (created) => {
        // replace temp message with server-provided message
        this.messages.update((list) => list.map((m) => (m.id === tempId ? created : m)));
        this.sending.set(false);
      },
      error: () => {
        // remove temp message and show error
        this.messages.update((list) => list.filter((m) => m.id !== tempId));
        this.error.set('Failed to send message');
        this.sending.set(false);
      }
    });
  }
}
