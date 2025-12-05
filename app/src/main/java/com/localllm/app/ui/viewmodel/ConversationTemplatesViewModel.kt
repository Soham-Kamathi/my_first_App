package com.localllm.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Template categories
 */
enum class TemplateCategory(val displayName: String) {
    CREATIVE("Creative Writing"),
    CODING("Coding & Tech"),
    LEARNING("Learning"),
    PRODUCTIVITY("Productivity"),
    ANALYSIS("Analysis"),
    FUN("Fun & Games"),
    PERSONAL("Personal")
}

/**
 * Conversation template data
 */
data class ConversationTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: TemplateCategory,
    val prompt: String,
    val systemPrompt: String? = null
)

/**
 * UI State for Conversation Templates
 */
data class ConversationTemplatesUiState(
    val selectedCategory: TemplateCategory? = null,
    val searchQuery: String = "",
    val templates: List<ConversationTemplate> = emptyList()
)

/**
 * ViewModel for Conversation Templates feature
 * Provides pre-built conversation starters organized by category
 */
@HiltViewModel
class ConversationTemplatesViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationTemplatesUiState(templates = allTemplates))
    val uiState: StateFlow<ConversationTemplatesUiState> = _uiState.asStateFlow()

    /**
     * Select a category filter
     */
    fun selectCategory(category: TemplateCategory?) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            templates = filterTemplates(category, _uiState.value.searchQuery)
        )
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            templates = filterTemplates(_uiState.value.selectedCategory, query)
        )
    }

    private fun filterTemplates(category: TemplateCategory?, query: String): List<ConversationTemplate> {
        return allTemplates.filter { template ->
            val matchesCategory = category == null || template.category == category
            val matchesQuery = query.isEmpty() ||
                    template.title.contains(query, ignoreCase = true) ||
                    template.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    companion object {
        val allTemplates = listOf(
            // Creative Writing
            ConversationTemplate(
                id = "story_starter",
                title = "Story Starter",
                description = "Get creative story ideas and opening paragraphs",
                category = TemplateCategory.CREATIVE,
                prompt = "I want to write a story. Can you give me an interesting story premise and a compelling opening paragraph?",
                systemPrompt = "You are a creative writing assistant who specializes in generating unique and engaging story ideas."
            ),
            ConversationTemplate(
                id = "poem_generator",
                title = "Poetry Helper",
                description = "Create poems in various styles",
                category = TemplateCategory.CREATIVE,
                prompt = "Help me write a poem. What topic or style would you like to explore?",
                systemPrompt = "You are a skilled poet who can compose poems in various styles and forms."
            ),
            ConversationTemplate(
                id = "character_creator",
                title = "Character Creator",
                description = "Develop detailed fictional characters",
                category = TemplateCategory.CREATIVE,
                prompt = "Help me create a detailed character for my story. Let's start with their background and personality.",
                systemPrompt = "You are a character development expert who creates rich, multi-dimensional fictional characters."
            ),
            ConversationTemplate(
                id = "world_builder",
                title = "World Builder",
                description = "Build fictional worlds and settings",
                category = TemplateCategory.CREATIVE,
                prompt = "Help me build a fictional world. What kind of setting interests you - fantasy, sci-fi, or something else?",
                systemPrompt = "You are a world-building expert who creates immersive fictional settings with rich history and culture."
            ),

            // Coding & Tech
            ConversationTemplate(
                id = "code_explainer",
                title = "Code Explainer",
                description = "Get explanations for code snippets",
                category = TemplateCategory.CODING,
                prompt = "I have some code I'd like you to explain. What programming language is it in?",
                systemPrompt = "You are an expert programmer who explains code clearly and thoroughly."
            ),
            ConversationTemplate(
                id = "debug_assistant",
                title = "Debug Helper",
                description = "Find and fix bugs in your code",
                category = TemplateCategory.CODING,
                prompt = "I have a bug in my code. Can you help me debug it?",
                systemPrompt = "You are an expert debugger who systematically identifies and fixes code issues."
            ),
            ConversationTemplate(
                id = "architecture_advisor",
                title = "Architecture Advisor",
                description = "Get software architecture guidance",
                category = TemplateCategory.CODING,
                prompt = "I need help designing the architecture for my project. What type of application are you building?",
                systemPrompt = "You are a senior software architect who provides guidance on system design and architecture."
            ),
            ConversationTemplate(
                id = "tech_interview",
                title = "Tech Interview Prep",
                description = "Practice technical interview questions",
                category = TemplateCategory.CODING,
                prompt = "Let's practice technical interview questions. What role or company are you preparing for?",
                systemPrompt = "You are a tech interview coach who helps candidates prepare for coding interviews."
            ),

            // Learning & Education
            ConversationTemplate(
                id = "tutor",
                title = "Personal Tutor",
                description = "Learn any subject with a patient tutor",
                category = TemplateCategory.LEARNING,
                prompt = "I want to learn something new. What topic would you like to explore?",
                systemPrompt = "You are a patient and knowledgeable tutor who explains concepts clearly and adapts to the learner's level."
            ),
            ConversationTemplate(
                id = "quiz_master",
                title = "Quiz Master",
                description = "Test your knowledge with custom quizzes",
                category = TemplateCategory.LEARNING,
                prompt = "Let's test my knowledge! What subject would you like to quiz me on?",
                systemPrompt = "You are an engaging quiz master who creates educational and progressively challenging questions."
            ),
            ConversationTemplate(
                id = "language_partner",
                title = "Language Partner",
                description = "Practice a foreign language",
                category = TemplateCategory.LEARNING,
                prompt = "I want to practice a language. Which language would you like to practice?",
                systemPrompt = "You are a language learning partner who engages in conversation and gently corrects mistakes."
            ),
            ConversationTemplate(
                id = "eli5",
                title = "ELI5 Explainer",
                description = "Complex topics explained simply",
                category = TemplateCategory.LEARNING,
                prompt = "Explain a complex topic to me in simple terms. What would you like to understand?",
                systemPrompt = "You explain complex topics using simple language, analogies, and examples that anyone can understand."
            ),

            // Productivity
            ConversationTemplate(
                id = "email_writer",
                title = "Email Writer",
                description = "Craft professional emails",
                category = TemplateCategory.PRODUCTIVITY,
                prompt = "Help me write a professional email. What's the context and who is the recipient?",
                systemPrompt = "You are an expert business communicator who writes clear, professional emails."
            ),
            ConversationTemplate(
                id = "meeting_summarizer",
                title = "Meeting Summarizer",
                description = "Summarize meeting notes into action items",
                category = TemplateCategory.PRODUCTIVITY,
                prompt = "I have meeting notes to summarize. Please share them and I'll extract key points and action items.",
                systemPrompt = "You are an executive assistant who creates clear meeting summaries with actionable takeaways."
            ),
            ConversationTemplate(
                id = "brainstorm_partner",
                title = "Brainstorm Partner",
                description = "Generate ideas collaboratively",
                category = TemplateCategory.PRODUCTIVITY,
                prompt = "Let's brainstorm together! What topic or problem would you like to explore?",
                systemPrompt = "You are a creative brainstorming partner who builds on ideas and offers fresh perspectives."
            ),
            ConversationTemplate(
                id = "project_planner",
                title = "Project Planner",
                description = "Plan and organize projects",
                category = TemplateCategory.PRODUCTIVITY,
                prompt = "Help me plan a project. What are you trying to accomplish?",
                systemPrompt = "You are a project management expert who breaks down goals into actionable steps."
            ),

            // Analysis & Research
            ConversationTemplate(
                id = "research_assistant",
                title = "Research Assistant",
                description = "Help with research and analysis",
                category = TemplateCategory.ANALYSIS,
                prompt = "I need help researching a topic. What would you like to learn more about?",
                systemPrompt = "You are a research assistant who helps explore topics systematically and thoroughly."
            ),
            ConversationTemplate(
                id = "data_analyst",
                title = "Data Analyst",
                description = "Analyze data and find insights",
                category = TemplateCategory.ANALYSIS,
                prompt = "Help me analyze some data. What kind of data do you have?",
                systemPrompt = "You are a data analyst who finds patterns, insights, and actionable conclusions from data."
            ),
            ConversationTemplate(
                id = "pros_cons",
                title = "Pros & Cons Analysis",
                description = "Weigh options objectively",
                category = TemplateCategory.ANALYSIS,
                prompt = "Help me evaluate a decision by considering pros and cons. What are you deciding?",
                systemPrompt = "You are an objective analyst who helps evaluate decisions by considering all perspectives."
            ),
            ConversationTemplate(
                id = "fact_checker",
                title = "Fact Checker",
                description = "Verify claims and information",
                category = TemplateCategory.ANALYSIS,
                prompt = "Help me verify some information. What claim would you like to fact-check?",
                systemPrompt = "You are a careful fact-checker who evaluates claims and identifies reliable sources."
            ),

            // Fun & Games
            ConversationTemplate(
                id = "trivia_game",
                title = "Trivia Game",
                description = "Play a fun trivia game",
                category = TemplateCategory.FUN,
                prompt = "Let's play trivia! What category interests you?",
                systemPrompt = "You are an entertaining trivia host who makes learning fun with interesting questions."
            ),
            ConversationTemplate(
                id = "riddle_master",
                title = "Riddle Master",
                description = "Solve riddles and puzzles",
                category = TemplateCategory.FUN,
                prompt = "Give me a riddle to solve! How difficult should it be?",
                systemPrompt = "You are a riddle master who presents clever puzzles and provides helpful hints when needed."
            ),
            ConversationTemplate(
                id = "roleplay",
                title = "Roleplay Adventure",
                description = "Interactive storytelling adventure",
                category = TemplateCategory.FUN,
                prompt = "Let's play a text adventure game! What kind of adventure would you like?",
                systemPrompt = "You are a game master running an interactive text adventure with choices and consequences."
            ),
            ConversationTemplate(
                id = "would_you_rather",
                title = "Would You Rather",
                description = "Play Would You Rather",
                category = TemplateCategory.FUN,
                prompt = "Let's play Would You Rather! I'll give you interesting dilemmas to choose from.",
                systemPrompt = "You create creative and thought-provoking 'Would You Rather' scenarios."
            ),

            // Personal
            ConversationTemplate(
                id = "journal_prompt",
                title = "Journal Prompts",
                description = "Reflective journaling questions",
                category = TemplateCategory.PERSONAL,
                prompt = "Give me a thoughtful journal prompt to reflect on today.",
                systemPrompt = "You provide meaningful journal prompts that encourage self-reflection and personal growth."
            ),
            ConversationTemplate(
                id = "decision_helper",
                title = "Decision Helper",
                description = "Help making personal decisions",
                category = TemplateCategory.PERSONAL,
                prompt = "I need help making a decision. What are you trying to decide?",
                systemPrompt = "You are a thoughtful advisor who helps people work through decisions by asking good questions."
            ),
            ConversationTemplate(
                id = "motivator",
                title = "Daily Motivation",
                description = "Get inspired and motivated",
                category = TemplateCategory.PERSONAL,
                prompt = "I need some motivation today. Share something inspiring!",
                systemPrompt = "You are an uplifting motivator who provides genuine encouragement and practical wisdom."
            ),
            ConversationTemplate(
                id = "mindfulness_guide",
                title = "Mindfulness Guide",
                description = "Guided relaxation and mindfulness",
                category = TemplateCategory.PERSONAL,
                prompt = "Guide me through a short mindfulness or relaxation exercise.",
                systemPrompt = "You are a calm mindfulness guide who leads peaceful relaxation and meditation exercises."
            )
        )
    }
}
