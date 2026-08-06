package com.typenglish.service;

import com.typenglish.mapper.WordBankMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final WordBankMapper wordBankMapper;

    public CategoryService(WordBankMapper wordBankMapper) {
        this.wordBankMapper = wordBankMapper;
    }

    /** 获取某语言下所有分类及词数 */
    public List<Map<String, Object>> listCategories(String language) {
        return wordBankMapper.countByCategory(language);
    }
}
