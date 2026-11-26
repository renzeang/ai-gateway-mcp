package org.veronicadavichi.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.veronicadavichi.entity.McpResource;

import java.util.List;

/**
 * MCP Resources Mapper 接口
 */
@Mapper
public interface McpResourceMapper extends BaseMapper<McpResource> {

    /**
     * 分页查询资源列表（带筛选）
     */
    @Select("<script>" +
            "SELECT r.*, s.name as serverName " +
            "FROM mcp_resources r " +
            "LEFT JOIN mcp_server s ON r.server_id = s.id " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND r.name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='method != null and method != \"\"'>" +
            "    AND r.method = #{method}" +
            "  </if>" +
            "  <if test='serverId != null'>" +
            "    AND r.server_id = #{serverId}" +
            "  </if>" +
            "  <if test='enabled != null'>" +
            "    AND r.enabled = #{enabled}" +
            "  </if>" +
            "</where>" +
            "ORDER BY r.name ASC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<McpResource> selectByCondition(@Param("name") String name,
                                        @Param("method") String method,
                                        @Param("serverId") Long serverId,
                                        @Param("enabled") Boolean enabled,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 统计符合条件的记录数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM mcp_resources r " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND r.name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='method != null and method != \"\"'>" +
            "    AND r.method = #{method}" +
            "  </if>" +
            "  <if test='serverId != null'>" +
            "    AND r.server_id = #{serverId}" +
            "  </if>" +
            "  <if test='enabled != null'>" +
            "    AND r.enabled = #{enabled}" +
            "  </if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("name") String name,
                          @Param("method") String method,
                          @Param("serverId") Long serverId,
                          @Param("enabled") Boolean enabled);
}
