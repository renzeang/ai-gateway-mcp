package org.veronicadavichi.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.veronicadavichi.entity.McpServer;

import java.util.List;

/**
 * MCP Server Mapper 接口
 */
@Mapper
public interface McpServerMapper extends BaseMapper<McpServer> {

    /**
     * 分页查询服务器列表（带筛选）
     */
    @Select("<script>" +
            "SELECT * FROM mcp_server " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='enabled != null'>" +
            "    AND enabled = #{enabled}" +
            "  </if>" +
            "</where>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    @Results(id = "serverResultMap", value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "name", column = "name"),
        @Result(property = "baseUrl", column = "base_url"),
        @Result(property = "description", column = "description"),
        @Result(property = "enabled", column = "enabled"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
    })
    List<McpServer> selectByCondition(@Param("name") String name,
                                      @Param("enabled") Boolean enabled,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /**
     * 统计符合条件的记录数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM mcp_server " +
            "<where>" +
            "  <if test='name != null and name != \"\"'>" +
            "    AND name LIKE CONCAT('%', #{name}, '%')" +
            "  </if>" +
            "  <if test='enabled != null'>" +
            "    AND enabled = #{enabled}" +
            "  </if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("name") String name,
                          @Param("enabled") Boolean enabled);
}
