package com.typenglish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.typenglish.entity.SentenceError;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface SentenceErrorMapper extends BaseMapper<SentenceError> {

    @Select("SELECT id, english, chinese, mode, slot_results, total_slots, correct_slots, created_at " +
            "FROM sentence_error WHERE user_id = #{userId} ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{size}")
    List<Map<String, Object>> pageList(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    @Select("SELECT COUNT(*) FROM sentence_error WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) as total, SUM(correct_slots) as correct " +
            "FROM sentence_error WHERE user_id = #{userId}")
    Map<String, Object> stats(@Param("userId") Long userId);
}
