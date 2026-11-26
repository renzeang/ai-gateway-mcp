package org.veronicadavichi.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.veronicadavichi.entity.McpTool;

import java.util.List;

/**
 * MCP Tools Mapper 接口
 */
@Mapper
public interface McpToolMapper extends BaseMapper<McpTool> {

    /**
     * 分页查询工具列表（带筛选）
     */
    @Select("<script>" +
            "SELECT t.*, s.name as serverName " +
            "FROM mcp_tools t " +
            "LEFT JOIN mcp_server s ON t.server_id = s.id " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND t.name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='method != null and method != \"\"'>" +
            "    AND t.method = #{method}" +
            "  </if>" +
            "  <if test='serverId != null'>" +
            "    AND t.server_id = #{serverId}" +
            "  </if>" +
            "  <if test='enabled != null'>" +
            "    AND t.enabled = #{enabled}" +
            "  </if>" +
            "</where>" +
            "ORDER BY t.name ASC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<McpTool> selectByCondition(@Param("name") String name,
                                    @Param("method") String method,
                                    @Param("serverId") Long serverId,
                                    @Param("enabled") Boolean enabled,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /**
     * 统计符合条件的记录数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM mcp_tools t " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND t.name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='method != null and method != \"\"'>" +
            "    AND t.method = #{method}" +
            "  </if>" +
            "  <if test='serverId != null'>" +
            "    AND t.server_id = #{serverId}" +
            "  </if>" +
            "  <if test='enabled != null'>" +
            "    AND t.enabled = #{enabled}" +
            "  </if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("name") String name,
                          @Param("method") String method,
                          @Param("serverId") Long serverId,
                          @Param("enabled") Boolean enabled);
}
