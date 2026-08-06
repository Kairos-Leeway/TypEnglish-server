package com.typenglish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.typenglish.entity.SentenceBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SentenceBankMapper extends BaseMapper<SentenceBank> {

    @Select("SELECT * FROM sentence_bank WHERE language = #{language} ORDER BY RAND() LIMIT #{limit}")
    List<SentenceBank> randomPick(@Param("language") String language, @Param("limit") int limit);
}
