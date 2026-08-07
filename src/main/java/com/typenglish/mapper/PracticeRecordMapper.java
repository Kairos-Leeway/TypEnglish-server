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

    /** 分页查询练习记录（关联 word_bank 取单词信息），最近优先 */
    @Select("SELECT pr.id, pr.mode, pr.correct, pr.answer, pr.created_at, " +
            "wb.word, wb.translation " +
            "FROM practice_record pr " +
            "LEFT JOIN word_bank wb ON pr.word_id = wb.id " +
            "WHERE pr.user_id = #{userId} " +
            "ORDER BY pr.created_at DESC " +
            "LIMIT #{offset}, #{size}")
    List<Map<String, Object>> pageWithWord(@Param("userId") Long userId,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    /** 按日期聚合统计（含句子练习） */
    @Select("SELECT date, SUM(count) as count, SUM(correct) as correct FROM (" +
            "SELECT DATE(created_at) as date, COUNT(*) as count, " +
            "SUM(CASE WHEN correct = 1 THEN 1 ELSE 0 END) as correct " +
            "FROM practice_record WHERE user_id = #{userId} GROUP BY DATE(created_at) " +
            "UNION ALL " +
            "SELECT DATE(created_at) as date, COUNT(*) as count, " +
            "SUM(correct_slots) as correct " +
            "FROM sentence_error WHERE user_id = #{userId} GROUP BY DATE(created_at)" +
            ") t GROUP BY date ORDER BY date DESC LIMIT #{days}")
    List<Map<String, Object>> dailyStats(@Param("userId") Long userId,
                                         @Param("days") int days);

    /** 总记录数 */
    @Select("SELECT COUNT(*) FROM practice_record WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Long userId);
}
