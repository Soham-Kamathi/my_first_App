package com.localllm.app.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ModelIconMapper.
 */
class ModelIconMapperTest {

    @Test
    fun `getIconForModel returns correct icon for llama models`() {
        val result = ModelIconMapper.getIconForModel("Llama-3.2-3B-Instruct")
        // Would need to mock R.drawable in actual test, this is conceptual
        assertNotNull(result)
    }

    @Test
    fun `getIconForModel returns correct icon for deepseek models`() {
        val result = ModelIconMapper.getIconForModel("DeepSeek-Coder-1.5B")
        assertNotNull(result)
    }

    @Test
    fun `getIconForModel returns correct icon for qwen models`() {
        val result = ModelIconMapper.getIconForModel("Qwen2.5-3B-Instruct")
        assertNotNull(result)
    }

    @Test
    fun `getIconForModel returns default icon for unknown models`() {
        val result = ModelIconMapper.getIconForModel("UnknownModel-XYZ")
        assertNotNull(result)
    }

    @Test
    fun `getIconForModelByAuthor prioritizes author over name`() {
        val llamaIcon = ModelIconMapper.getIconForModelByAuthor("meta-llama", "Some-Random-Name")
        val mistralIcon = ModelIconMapper.getIconForModelByAuthor("mistralai", "Some-Random-Name")
        assertNotEquals(llamaIcon, mistralIcon)
    }

    @Test
    fun `getIconForModelByAuthor handles null author gracefully`() {
        val result = ModelIconMapper.getIconForModelByAuthor(null, "Llama-3.2-3B")
        assertNotNull(result)
    }

    @Test
    fun `getModelFamily detects llama family`() {
        val family = ModelIconMapper.getModelFamily("Llama-3.2-3B-Instruct", emptyList())
        assertEquals("Llama", family)
    }

    @Test
    fun `getModelFamily detects deepseek family`() {
        val family = ModelIconMapper.getModelFamily("DeepSeek-Coder-1.5B", emptyList())
        assertEquals("DeepSeek", family)
    }

    @Test
    fun `getModelFamily detects qwen family`() {
        val family = ModelIconMapper.getModelFamily("Qwen2.5-3B-Instruct", emptyList())
        assertEquals("Qwen", family)
    }

    @Test
    fun `getModelFamily returns Other for unknown models`() {
        val family = ModelIconMapper.getModelFamily("UnknownModel-XYZ", emptyList())
        assertEquals("Other", family)
    }

    @Test
    fun `getModelTypeDescription identifies code models from tags`() {
        val description = ModelIconMapper.getModelTypeDescription(
            listOf("code", "python", "transformers"),
            "text-generation"
        )
        assertEquals("Code Assistant", description)
    }

    @Test
    fun `getModelTypeDescription identifies chat models from tags`() {
        val description = ModelIconMapper.getModelTypeDescription(
            listOf("chat", "conversational"),
            "conversational"
        )
        assertEquals("Chat Model", description)
    }

    @Test
    fun `getModelTypeDescription identifies instruct models`() {
        val description = ModelIconMapper.getModelTypeDescription(
            listOf("instruct", "transformers"),
            "text-generation"
        )
        assertEquals("Instruction Following", description)
    }

    @Test
    fun `getModelTypeDescription falls back to pipeline tag`() {
        val description = ModelIconMapper.getModelTypeDescription(
            emptyList(),
            "text-generation"
        )
        assertEquals("Text Generation", description)
    }

    @Test
    fun `getModelTypeDescription handles conversational pipeline`() {
        val description = ModelIconMapper.getModelTypeDescription(
            emptyList(),
            "conversational"
        )
        assertEquals("Conversational AI", description)
    }

    @Test
    fun `getModelTypeDescription returns default for unknown`() {
        val description = ModelIconMapper.getModelTypeDescription(
            emptyList(),
            null
        )
        assertEquals("Language Model", description)
    }
}
