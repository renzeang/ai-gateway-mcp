package org.veronicadavichi.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.veronicadavichi.entity.McpAuditLog;

import java.util.List;

/**
 * MCP 审计日志 Mapper 接口
 */
@Mapper
public interface McpAuditLogMapper extends BaseMapper<McpAuditLog> {

    /**
     * 分页查询审计日志列表（带筛选）
     */
    @Select("<script>" +
            "SELECT * FROM mcp_audit_log " +
            "<where>" +
            "  <if test='targetName != null and targetName != \"\"'>" +
            "    AND target_name LIKE CONCAT('%', #{targetName}, '%')" +
            "  </if>" +
            "  <if test='requestType != null and requestType != \"\"'>" +
            "    AND request_type LIKE CONCAT('%', #{requestType}, '%')" +
            "  </if>" +
            "  <if test='isError != null'>" +
            "    AND is_error = #{isError}" +
            "  </if>" +
            "  <if test='startDate != null and startDate != \"\"'>" +
            "    AND DATE(created_at) &gt;= #{startDate}" +
            "  </if>" +
            "  <if test='endDate != null and endDate != \"\"'>" +
            "    AND DATE(created_at) &lt;= #{endDate}" +
            "  </if>" +
            "</where>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<McpAuditLog> selectByCondition(@Param("targetName") String targetName,
                                        @Param("requestType") String requestType,
                                        @Param("isError") Boolean isError,
                                        @Param("startDate") String startDate,
                                        @Param("endDate") String endDate,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 统计符合条件的记录数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM mcp_audit_log " +
            "<where>" +
            "  <if test='targetName != null and targetName != \"\"'>" +
            "    AND target_name LIKE CONCAT('%', #{targetName}, '%')" +
            "  </if>" +
            "  <if test='requestType != null and requestType != \"\"'>" +
            "    AND request_type LIKE CONCAT('%', #{requestType}, '%')" +
            "  </if>" +
            "  <if test='isError != null'>" +
            "    AND is_error = #{isError}" +
            "  </if>" +
            "  <if test='startDate != null and startDate != \"\"'>" +
            "    AND created_at &gt;= #{startDate}" +
            "  </if>" +
            "  <if test='endDate != null and endDate != \"\"'>" +
            "    AND created_at &lt;= #{endDate} 23:59:59" +
            "  </if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("targetName") String targetName,
                          @Param("requestType") String requestType,
                          @Param("isError") Boolean isError,
                          @Param("startDate") String startDate,
                          @Param("endDate") String endDate);
}
