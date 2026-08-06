package com.typenglish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.typenglish.entity.WordBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface WordBankMapper extends BaseMapper<WordBank> {

    @Select("SELECT * FROM word_bank WHERE language = #{language} ORDER BY RAND() LIMIT #{limit}")
    List<WordBank> randomPick(@Param("language") String language, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT * FROM word_bank WHERE language = #{language} " +
            "<if test='excludeIds != null and excludeIds.size() > 0'>" +
            "AND id NOT IN <foreach collection='excludeIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "</if>" +
            "ORDER BY RAND() LIMIT #{limit}" +
            "</script>")
    List<WordBank> randomPickExcluding(@Param("language") String language,
                                       @Param("excludeIds") List<Long> excludeIds,
                                       @Param("limit") int limit);

    /** 按分类随机抽取 */
    @Select("SELECT * FROM word_bank WHERE language = #{language} AND category = #{category} ORDER BY RAND() LIMIT #{limit}")
    List<WordBank> randomPickByCategory(@Param("language") String language,
                                        @Param("category") String category,
                                        @Param("limit") int limit);

    /** 获取所有分类及单词数量 */
    @Select("SELECT category, COUNT(*) as wordCount FROM word_bank WHERE language = #{language} GROUP BY category ORDER BY category")
    List<Map<String, Object>> countByCategory(@Param("language") String language);
}
