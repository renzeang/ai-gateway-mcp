package org.veronicadavichi.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.veronicadavichi.entity.McpPrompt;

import java.util.List;

/**
 * MCP Prompts Mapper 接口
 */
@Mapper
public interface McpPromptMapper extends BaseMapper<McpPrompt> {

    /**
     * 分页查询提示词列表（带筛选）
     */
    @Select("<script>" +
            "SELECT p.*, s.name as serverName " +
            "FROM mcp_prompts p " +
            "LEFT JOIN mcp_server s ON p.server_id = s.id " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND p.name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='serverId != null'>" +
            "    AND p.server_id = #{serverId}" +
            "  </if>" +
            "  <if test='enabled != null'>" +
            "    AND p.enabled = #{enabled}" +
            "  </if>" +
            "</where>" +
            "ORDER BY p.name ASC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<McpPrompt> selectByCondition(@Param("name") String name,
                                      @Param("serverId") Long serverId,
                                      @Param("enabled") Boolean enabled,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /**
     * 统计符合条件的记录数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM mcp_prompts p " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND p.name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='serverId != null'>" +
            "    AND p.server_id = #{serverId}" +
            "  </if>" +
            "  <if test='enabled != null'>" +
            "    AND p.enabled = #{enabled}" +
            "  </if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("name") String name,
                          @Param("serverId") Long serverId,
                          @Param("enabled") Boolean enabled);
}
