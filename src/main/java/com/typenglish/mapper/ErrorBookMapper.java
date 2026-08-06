package com.typenglish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.typenglish.entity.ErrorBook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ErrorBookMapper extends BaseMapper<ErrorBook> {

    /** 用户待复习的错题 */
    @Select("SELECT eb.*, wb.word, wb.translation, wb.phonetic FROM error_book eb " +
            "JOIN word_bank wb ON eb.word_id = wb.id " +
            "WHERE eb.user_id = #{userId} AND eb.mastered = 0 " +
            "AND eb.next_review_at <= NOW() " +
            "ORDER BY eb.error_count DESC, eb.next_review_at ASC " +
            "LIMIT #{limit}")
    List<ErrorBook> findDueReviews(@Param("userId") Long userId, @Param("limit") int limit);

    /** 用户错题统计(按语言) */
    @Select("SELECT wb.language, COUNT(*) as error_count, " +
            "SUM(eb.error_count) as total_errors " +
            "FROM error_book eb JOIN word_bank wb ON eb.word_id = wb.id " +
            "WHERE eb.user_id = #{userId} AND eb.mastered = 0 " +
            "GROUP BY wb.language")
    List<java.util.Map<String, Object>> errorStatsByLanguage(@Param("userId") Long userId);
}
