import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MessageService } from './message.service';
import { MessageResponse } from './message.models';

@Component({
  selector: 'app-message-thread',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './message-thread.component.html',
  styleUrls: ['./message-thread.component.scss']
})
export class MessageThreadComponent implements OnInit {
  readonly messages = signal<MessageResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  private groupId = 0;

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
}
