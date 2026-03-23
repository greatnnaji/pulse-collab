import { Component, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './auth-layout.component.html',
  styleUrl: './auth-layout.component.scss'
})
export class AuthLayoutComponent implements OnInit {
  private readonly storageKey = 'pulse-theme';
  readonly darkMode = signal(false);

  ngOnInit(): void {
    const savedTheme = localStorage.getItem(this.storageKey);
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const useDark = savedTheme ? savedTheme === 'dark' : prefersDark;

    this.darkMode.set(useDark);
    this.applyTheme(useDark);
  }

  toggleTheme(): void {
    const nextValue = !this.darkMode();
    this.darkMode.set(nextValue);
    this.applyTheme(nextValue);
    localStorage.setItem(this.storageKey, nextValue ? 'dark' : 'light');
  }

  private applyTheme(isDark: boolean): void {
    document.body.classList.toggle('dark-theme', isDark);
  }
}
