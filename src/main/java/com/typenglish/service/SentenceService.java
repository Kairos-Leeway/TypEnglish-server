package com.typenglish.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.typenglish.entity.SentenceBank;
import com.typenglish.entity.WordBank;
import com.typenglish.mapper.SentenceBankMapper;
import com.typenglish.mapper.SentenceErrorMapper;
import com.typenglish.mapper.WordBankMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SentenceService {

    private final SentenceBankMapper sentenceBankMapper;
    private final WordBankMapper wordBankMapper;
    private final SentenceErrorMapper sentenceErrorMapper;

    public SentenceService(SentenceBankMapper sentenceBankMapper, WordBankMapper wordBankMapper,
                           SentenceErrorMapper sentenceErrorMapper) {
        this.sentenceBankMapper = sentenceBankMapper;
        this.wordBankMapper = wordBankMapper;
        this.sentenceErrorMapper = sentenceErrorMapper;
    }

    /** 随机抽取句子，tokenize 为单词+标点列表 */
    public List<Map<String, Object>> getSentences(String language, int count) {
        var sentences = sentenceBankMapper.randomPick(language, Math.min(count, 15));
        List<Map<String, Object>> result = new ArrayList<>();
        for (var s : sentences) {
            String english = s.getEnglish();
            List<Map<String, Object>> wordList = new ArrayList<>();
            if (english != null) {
                StringBuilder buf = new StringBuilder();
                int idx = 0;
                for (int i = 0; i < english.length(); i++) {
                    char ch = english.charAt(i);
                    if (Character.isWhitespace(ch)) {
                        if (buf.length() > 0) { wordList.add(buildToken(buf.toString(), language, idx++)); buf.setLength(0); }
                        continue;
                    }
                    if (Character.isLetter(ch) || ch == '\'' || ch == '-') {
                        buf.append(ch);
                    } else {
                        if (buf.length() > 0) { wordList.add(buildToken(buf.toString(), language, idx++)); buf.setLength(0); }
                        wordList.add(punctToken(String.valueOf(ch), idx++));
                    }
                }
                if (buf.length() > 0) { wordList.add(buildToken(buf.toString(), language, idx++)); }
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", s.getId());
            entry.put("chinese", s.getChinese());
            entry.put("english", s.getEnglish());
            entry.put("words", wordList);
            entry.put("difficulty", s.getDifficulty());
            result.add(entry);
        }
        return result;
    }

    /** 删除句子，同时清理关联的错题记录 */
    public void deleteById(Long id) {
        sentenceBankMapper.deleteById(id);
        sentenceErrorMapper.delete(new LambdaQueryWrapper<com.typenglish.entity.SentenceError>()
                .eq(com.typenglish.entity.SentenceError::getSentenceId, id));
    }

    private Map<String, Object> buildToken(String word, String language, int index) {
        Long wordId = null;
        String translation = null;
        String phonetic = null;
        WordBank wb = wordBankMapper.selectOne(new LambdaQueryWrapper<WordBank>()
                .eq(WordBank::getWord, word).eq(WordBank::getLanguage, language));
        if (wb != null) {
            wordId = wb.getId();
            translation = wb.getTranslation();
            phonetic = wb.getPhonetic();
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("index", index);
        entry.put("word", word);
        entry.put("wordId", wordId);
        entry.put("translation", translation);
        entry.put("phonetic", phonetic);
        entry.put("punctuation", false);
        return entry;
    }

    private Map<String, Object> punctToken(String punct, int index) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("index", index);
        entry.put("word", punct);
        entry.put("wordId", null);
        entry.put("translation", null);
        entry.put("phonetic", null);
        entry.put("punctuation", true);
        return entry;
    }
}
