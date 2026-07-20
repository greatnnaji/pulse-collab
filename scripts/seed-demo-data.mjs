#!/usr/bin/env node
// Populates a running Pulse Collab backend with demo users, a group, and a message
// history, via the real REST API (register/login, create group, invite + accept,
// send messages) - no direct DB access, so it works the same locally or against a
// deployed backend.
//
// Usage:
//   node scripts/seed-demo-data.mjs                                        # local, http://localhost:8080/api
//   API_BASE_URL=https://<render-url>/api node scripts/seed-demo-data.mjs  # deployed backend
//
// Safe to re-run: existing users log in instead of re-registering, and users
// already in the group are skipped instead of re-invited.

const API_BASE_URL = process.env.API_BASE_URL ?? 'http://localhost:8080/api';

const USERS = [
  { username: 'alice', email: 'alice@example.com', password: 'Demo1234', displayName: 'Alice' },
  { username: 'bob', email: 'bob@example.com', password: 'Demo1234', displayName: 'Bob' },
  { username: 'carol', email: 'carol@example.com', password: 'Demo1234', displayName: 'Carol' },
];

const GROUP = { name: 'Product Team', description: 'Demo workspace for the walkthrough' };

const CONVERSATION = [
  ['alice', 'Hey team, pushed the deploy config fixes'],
  ['bob', 'Nice, pulling now'],
  ['carol', 'Real-time messages are working on my end'],
  ['alice', 'STOMP handshake looking solid'],
  ['bob', "This is the feature I'm most proud of tbh"],
];

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`${options.method ?? 'GET'} ${path} -> ${res.status}: ${body}`);
  }
  return res.status === 204 ? null : res.json();
}

async function registerOrLogin(user) {
  try {
    const auth = await request('/auth/register', { method: 'POST', body: JSON.stringify(user) });
    console.log(`registered ${user.username}`);
    return auth;
  } catch {
    const auth = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ usernameOrEmail: user.username, password: user.password }),
    });
    console.log(`${user.username} already existed, logged in`);
    return auth;
  }
}

async function getOrCreateGroup(ownerToken) {
  const groups = await request('/groups', { headers: { Authorization: `Bearer ${ownerToken}` } });
  const existing = groups.find((g) => g.name === GROUP.name);
  if (existing) {
    console.log(`reusing existing group "${existing.name}" (id ${existing.id})`);
    return existing;
  }
  const created = await request('/groups', {
    method: 'POST',
    headers: { Authorization: `Bearer ${ownerToken}` },
    body: JSON.stringify(GROUP),
  });
  console.log(`created group "${created.name}" (id ${created.id})`);
  return created;
}

async function ensureMember(group, ownerToken, member, memberToken) {
  const members = await request(`/groups/${group.id}/members`, {
    headers: { Authorization: `Bearer ${ownerToken}` },
  });
  if (members.some((m) => m.email === member.email)) {
    console.log(`${member.username} already in "${group.name}"`);
    return;
  }
  const invite = await request(`/groups/${group.id}/members`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${ownerToken}` },
    body: JSON.stringify({ email: member.email }),
  });
  await request(`/invites/${invite.id}/accept`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${memberToken}` },
  });
  console.log(`${member.username} joined "${group.name}"`);
}

async function main() {
  console.log(`seeding ${API_BASE_URL}`);

  const tokens = {};
  for (const user of USERS) {
    const auth = await registerOrLogin(user);
    tokens[user.username] = auth.token;
  }

  const owner = USERS[0];
  const group = await getOrCreateGroup(tokens[owner.username]);

  for (const member of USERS.slice(1)) {
    await ensureMember(group, tokens[owner.username], member, tokens[member.username]);
  }

  for (const [username, content] of CONVERSATION) {
    await request(`/groups/${group.id}/messages`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${tokens[username]}` },
      body: JSON.stringify({ content }),
    });
    console.log(`${username}: ${content}`);
  }

  console.log('\nseed complete, demo logins:');
  for (const user of USERS) {
    console.log(`  ${user.username} / ${user.password}`);
  }
}

main().catch((err) => {
  console.error(err.message);
  process.exit(1);
});
