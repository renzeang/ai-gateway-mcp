package org.veronicadavichi.config.mcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpSessionManager 单元测试
 */
class McpSessionManagerTest {

    private McpSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new McpSessionManager();
    }

    @AfterEach
    void tearDown() {
        if (sessionManager != null) {
            sessionManager.shutdown();
        }
    }

    @Test
    void testCreateSession() {
        McpSession session = sessionManager.createSession();

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertTrue(sessionManager.hasSession(session.getSessionId()));
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    void testGetSession() {
        McpSession created = sessionManager.createSession();
        String sessionId = created.getSessionId();

        McpSession retrieved = sessionManager.getSession(sessionId);

        assertNotNull(retrieved);
        assertEquals(sessionId, retrieved.getSessionId());
        assertSame(created, retrieved);
    }

    @Test
    void testGetSession_NullId() {
        McpSession retrieved = sessionManager.getSession(null);
        assertNull(retrieved);
    }

    @Test
    void testGetSession_NonExistent() {
        McpSession retrieved = sessionManager.getSession("non-existent-id");
        assertNull(retrieved);
    }

    @Test
    void testGetOrCreateSession_WithValidId() {
        McpSession created = sessionManager.createSession();
        String sessionId = created.getSessionId();

        McpSession retrieved = sessionManager.getOrCreateSession(sessionId);

        assertNotNull(retrieved);
        assertEquals(sessionId, retrieved.getSessionId());
        assertSame(created, retrieved);
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    void testGetOrCreateSession_WithNullId() {
        McpSession session = sessionManager.getOrCreateSession(null);

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertTrue(sessionManager.hasSession(session.getSessionId()));
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    void testGetOrCreateSession_WithEmptyId() {
        McpSession session = sessionManager.getOrCreateSession("");

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    void testGetOrCreateSession_WithNonExistentId() {
        McpSession session = sessionManager.getOrCreateSession("non-existent");

        assertNotNull(session);
        assertNotEquals("non-existent", session.getSessionId());
        assertTrue(sessionManager.hasSession(session.getSessionId()));
    }

    @Test
    void testRemoveSession() {
        McpSession created = sessionManager.createSession();
        String sessionId = created.getSessionId();

        McpSession removed = sessionManager.removeSession(sessionId);

        assertNotNull(removed);
        assertEquals(sessionId, removed.getSessionId());
        assertFalse(sessionManager.hasSession(sessionId));
        assertEquals(0, sessionManager.getActiveSessionCount());
    }

    @Test
    void testRemoveSession_NonExistent() {
        McpSession removed = sessionManager.removeSession("non-existent");
        assertNull(removed);
    }

    @Test
    void testHasSession() {
        McpSession created = sessionManager.createSession();
        String sessionId = created.getSessionId();

        assertTrue(sessionManager.hasSession(sessionId));

        sessionManager.removeSession(sessionId);

        assertFalse(sessionManager.hasSession(sessionId));
    }

    @Test
    void testGetActiveSessionCount() {
        assertEquals(0, sessionManager.getActiveSessionCount());

        sessionManager.createSession();
        assertEquals(1, sessionManager.getActiveSessionCount());

        sessionManager.createSession();
        assertEquals(2, sessionManager.getActiveSessionCount());

        sessionManager.removeSession(sessionManager.createSession().getSessionId());
        assertEquals(2, sessionManager.getActiveSessionCount());
    }

    @Test
    void testSessionTouch() throws InterruptedException {
        McpSession session = sessionManager.createSession();
        Instant beforeTouch = session.getLastActivityAt();

        Thread.sleep(10);
        sessionManager.getSession(session.getSessionId());

        assertTrue(session.getLastActivityAt().isAfter(beforeTouch));
    }

    @Test
    void testCleanupExpiredSessions() throws Exception {
        McpSession session1 = sessionManager.createSession();
        McpSession session2 = sessionManager.createSession();

        // 模拟 session2 过期
        Field field = McpSession.class.getDeclaredField("lastActivityAt");
        field.setAccessible(true);
        field.set(session2, Instant.now().minus(Duration.ofHours(1)));

        // 等待一点时间确保清理任务可以执行
        Thread.sleep(100);

        // 手动触发清理（通过访问私有方法）
        Field cleanupMethod = McpSessionManager.class.getDeclaredField("sessions");
        cleanupMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, McpSession> sessionsMap = (Map<String, McpSession>) cleanupMethod.get(sessionManager);

        // 移除过期的会话
        sessionsMap.entrySet().removeIf(entry -> entry.getValue().isExpired(Duration.ofMinutes(30)));

        assertFalse(sessionManager.hasSession(session2.getSessionId()));
        assertTrue(sessionManager.hasSession(session1.getSessionId()));
    }

    @Test
    void testShutdown() {
        sessionManager.createSession();
        sessionManager.createSession();

        assertEquals(2, sessionManager.getActiveSessionCount());

        sessionManager.shutdown();

        assertEquals(0, sessionManager.getActiveSessionCount());
    }
}
