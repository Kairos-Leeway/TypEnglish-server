package com.typenglish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.typenglish.entity.PracticeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface PracticeRecordMapper extends BaseMapper<PracticeRecord> {

    /** 按模式统计用户练习次数 */
    @Select("SELECT mode, COUNT(*) as count FROM practice_record " +
            "WHERE user_id = #{userId} GROUP BY mode")
    List<Map<String, Object>> countByMode(@Param("userId") Long userId);

    /** 用户总练习统计 */
    @Select("SELECT COUNT(*) as total, SUM(CASE WHEN correct = 1 THEN 1 ELSE 0 END) as correct " +
            "FROM practice_record WHERE user_id = #{userId}")
    Map<String, Object> stats(@Param("userId") Long userId);
}
