package org.veronicadavichi.config.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpSession 单元测试
 */
class McpSessionTest {

    @Test
    void testSessionCreation() {
        String sessionId = "test-session-123";
        McpSession session = new McpSession(sessionId);

        assertEquals(sessionId, session.getSessionId());
        assertFalse(session.isInitialized());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getLastActivityAt());
        assertTrue(session.getState().isEmpty());
    }

    @Test
    void testTouch() throws InterruptedException {
        McpSession session = new McpSession("test-session");
        Instant beforeTouch = session.getLastActivityAt();

        Thread.sleep(10);
        session.touch();

        assertTrue(session.getLastActivityAt().isAfter(beforeTouch));
    }

    @Test
    void testIsExpired() {
        McpSession session = new McpSession("test-session");

        // 刚创建的会话不应过期
        assertFalse(session.isExpired(Duration.ofMinutes(30)));

        // 模拟过期会话
        session = new McpSession("test-session-2") {
            {
                // 通过反射或直接设置来模拟过期
                try {
                    var field = McpSession.class.getDeclaredField("lastActivityAt");
                    field.setAccessible(true);
                    field.set(this, Instant.now().minus(Duration.ofHours(1)));
                } catch (Exception e) {
                    fail("Failed to set lastActivityAt: " + e.getMessage());
                }
            }
        };

        assertTrue(session.isExpired(Duration.ofMinutes(30)));
    }

    @Test
    void testInitializedState() {
        McpSession session = new McpSession("test-session");

        assertFalse(session.isInitialized());

        session.setInitialized(true);
        assertTrue(session.isInitialized());

        session.setInitialized(false);
        assertFalse(session.isInitialized());
    }

    @Test
    void testSessionState() {
        McpSession session = new McpSession("test-session");

        session.setState("key1", "value1");
        session.setState("key2", 123);
        session.setState("key3", true);

        assertEquals("value1", session.getStateValue("key1"));
        assertEquals(123, session.getStateValue("key2"));
        assertTrue((Boolean) session.getStateValue("key3"));

        assertNull(session.getStateValue("nonexistent"));
    }

    @Test
    void testConnectionId() {
        McpSession session = new McpSession("test-session");

        assertNull(session.getConnectionId());

        session.setConnectionId("conn-123");
        assertEquals("conn-123", session.getConnectionId());
    }
}
