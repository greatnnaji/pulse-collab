import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { Client,StompSubscription } from '@stomp/stompjs';
import { AuthService } from '../auth/auth.service';

export type ConnectionState = 'connecting' | 'connected' | 'disconnected';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private client: Client | null = null;
  private reconnectAttempt = 0;
  private reconnectTimeouts: Array<ReturnType<typeof setTimeout>> = [];
  private activeSubscriptions = new Map<string, StompSubscription>();
  private desiredDestinations = new Set<string>();

  readonly connectionState$ = new BehaviorSubject<ConnectionState>('disconnected');
  readonly messages$ = new Subject<any>();

  constructor(private readonly authService: AuthService) {
    this.client = new Client();
  }

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.connectionState$.value === 'connected') {
        resolve();
        return;
      }

      if (this.connectionState$.value === 'connecting') {
        // Already connecting, wait
        const subscription = this.connectionState$.subscribe((state) => {
          if (state === 'connected') {
            subscription.unsubscribe();
            resolve();
          } else if (state === 'disconnected') {
            subscription.unsubscribe();
            reject(new Error('Connection failed'));
          }
        });
        return;
      }

      const token = this.authService.getJwtToken();
      if (!token) {
        reject(new Error('No auth token available'));
        return;
      }

      this.connectionState$.next('connecting');

      if (!this.client) {
        this.client = new Client();
      }

      this.client.brokerURL = `ws://localhost:8080/ws?token=${token}`;
      this.client.reconnectDelay = 0; // We handle reconnect manually
      this.client.heartbeatIncoming = 4000;
      this.client.heartbeatOutgoing = 4000;

      this.client.onConnect = () => {
        this.reconnectAttempt = 0;
        this.connectionState$.next('connected');
        this.restoreSubscriptions();
        resolve();
      };

      this.client.onDisconnect = () => {
        this.connectionState$.next('disconnected');
        this.scheduleReconnect();
      };

      this.client.onStompError = (error) => {
        console.error('STOMP error:', error);
        this.connectionState$.next('disconnected');
        this.scheduleReconnect();
        reject(error);
      };

      try {
        this.client.activate(); // triggers beforeHandshake request
      } catch (err) {
        this.connectionState$.next('disconnected');
        reject(err);
      }
    });
  }

  disconnect(): void {
    this.clearReconnectTimeouts();
    this.activeSubscriptions.forEach((sub) => {
      try {
        sub.unsubscribe();
      } catch (e) {
        // Ignore unsubscribe errors
      }
    });
    this.activeSubscriptions.clear();

    if (this.client && this.client.connected) {
      this.client.deactivate();
    }
    this.connectionState$.next('disconnected');
  }

  subscribeToGroupMessages(groupId: number): void {
    const destination = `/topic/groups/${groupId}`;
    this.desiredDestinations.add(destination);

    // Unsubscribe from old subscription for this group if it exists
    const existingKey = destination;
    if (this.activeSubscriptions.has(existingKey)) {
      try {
        this.activeSubscriptions.get(existingKey)?.unsubscribe();
      } catch (e) {
        // Ignore
      }
    }

    if (!this.client || !this.client.connected) {
      console.warn('WebSocket not connected; cannot subscribe');
      return;
    }

    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const payload = JSON.parse(message.body);
        this.messages$.next(payload);
      } catch (err) {
        console.error('Failed to parse message:', err);
      }
    });

    this.activeSubscriptions.set(existingKey, subscription);
  }

  unsubscribeFromGroupMessages(groupId: number): void {
    const destination = `/topic/groups/${groupId}`;
    this.desiredDestinations.delete(destination);
    const subscription = this.activeSubscriptions.get(destination);
    if (subscription) {
      try {
        subscription.unsubscribe();
      } catch (e) {
        // Ignore
      }
      this.activeSubscriptions.delete(destination);
    }
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimeouts();

    const delays = [1000, 2000, 4000, 8000, 8000, 8000]; // cap at 8s
    const delay = delays[Math.min(this.reconnectAttempt, delays.length - 1)];

    const timeout = setTimeout(() => {
      this.reconnectAttempt++;
      this.connect().catch((err) => {
        console.error('Reconnect failed:', err);
      });
    }, delay);

    this.reconnectTimeouts.push(timeout);
  }

  private clearReconnectTimeouts(): void {
    this.reconnectTimeouts.forEach(clearTimeout);
    this.reconnectTimeouts = [];
  }

  private restoreSubscriptions(): void {
    if (!this.client || !this.client.connected) {
      return;
    }

    for (const destination of this.desiredDestinations) {
      if (this.activeSubscriptions.has(destination)) {
        continue;
      }

      const subscription = this.client.subscribe(destination, (message) => {
        try {
          const payload = JSON.parse(message.body);
          this.messages$.next(payload);
        } catch (err) {
          console.error('Failed to parse message:', err);
        }
      });

      this.activeSubscriptions.set(destination, subscription);
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
