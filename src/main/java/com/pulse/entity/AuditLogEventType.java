package com.pulse.entity;

public enum AuditLogEventType {
    USER_REGISTERED,
    USER_LOGIN_SUCCESS,
    USER_LOGIN_FAILED,
    GROUP_CREATED,
    GROUP_MEMBER_ADDED,
    GROUP_MEMBER_REMOVED,
    GROUP_LEFT
}