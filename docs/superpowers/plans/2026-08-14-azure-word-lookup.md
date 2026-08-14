# Azure Word Lookup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve hover word details through Azure Translator when configured, while preserving the existing DeepSeek lookup as a safe fallback.

**Architecture:** Add a focused `AzureTranslatorService` that owns Azure HTTP calls and JSON normalization. `WordService` keeps the existing Redis → database flow, asks Azure first for missing/new words, and calls the existing AI method only when Azure is unavailable or returns no usable Chinese translation. Azure results are considered cacheable with translation as the required core field because Translator does not provide English IPA.

**Tech Stack:** Java 17, Spring Boot, Java HttpClient, Jackson, JUnit 5, Mockito, Maven.

**Spec:** User authorization in the current Codex task following the Azure Translator assessment.

## Global Constraints

- No Azure credentials may be committed.
- With no `AZURE_TRANSLATOR_KEY`, behavior must remain compatible with the current DeepSeek lookup.
- Azure output must preserve the existing response keys: `found`, `word`, `phonetic`, `translation`, `partOfSpeech`, `example`.
- Azure Translator does not supply IPA; `phonetic` remains optional for Azure-resolved words.
- Redis and database caching remain in front of external providers.

---

### Task 1: Azure response normalization

**Files:**
- Create: `src/main/java/com/typenglish/service/AzureTranslatorService.java`
- Test: `src/test/java/com/typenglish/service/AzureTranslatorServiceTest.java`

**Interfaces:**
- Produces: `boolean isConfigured()` and `Map<String, Object> lookup(String word, String language)`.

- [ ] Write failing parser tests for dictionary alternatives, POS tags, dictionary examples, and plain translation fallback.
- [ ] Implement deterministic Jackson parsing with the first highest-confidence dictionary translation.
- [ ] Add Azure Dictionary Lookup, Dictionary Examples, and Translate HTTP requests with timeouts.
- [ ] Return `null` for unconfigured, unsupported, non-2xx, or unusable responses so callers can fall back safely.

### Task 2: Provider ordering and cache completeness

**Files:**
- Modify: `src/main/java/com/typenglish/service/WordService.java`
- Test: `src/test/java/com/typenglish/service/WordServiceLookupTest.java`

**Interfaces:**
- Consumes: `AzureTranslatorService.lookup` and `AzureTranslatorService.isConfigured`.

- [ ] Inject Azure Translator into `WordService`.
- [ ] Add an Azure-first lookup method that falls back to the existing AI method only when Azure has no usable result.
- [ ] Preserve existing database values and fill only missing fields.
- [ ] Treat translation as sufficient for Azure cache hits so missing IPA does not trigger repeated external calls.
- [ ] Store new provider-resolved words with a neutral `词典查询` category.

### Task 3: Configuration and verification

**Files:**
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Consumes environment variables `AZURE_TRANSLATOR_KEY`, `AZURE_TRANSLATOR_REGION`, and optional `AZURE_TRANSLATOR_ENDPOINT`.

- [ ] Add empty-by-default Azure Translator configuration.
- [ ] Run focused lookup tests, then the complete Maven test suite.
- [ ] Run Maven package without tests to verify production compilation.
- [ ] Check Git diff for credentials, debug logs, generated files, and unrelated changes.
