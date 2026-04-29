package com.xsgrok.app.agent

import com.xsgrok.app.data.model.*

/**
 * 生成模式预设系统
 * 提供多种叙事风格和节奏的预设配置
 */

data class GenerationPreset(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val sensoryProfile: SensoryProfile,
    val generationConfig: GenerationConfig,
    val systemPromptAddition: String
)

object GenerationPresets {
    
    val PRESETS = listOf(
        // 1. 快节奏热血
        GenerationPreset(
            id = "fast_hotblood",
            name = "快节奏热血",
            description = "情节紧凑，战斗密集，燃点频出",
            icon = "🔥",
            sensoryProfile = SensoryProfile(
                descriptionDensity = 4,
                sensoryFocus = listOf(SensoryType.VISUAL, SensoryType.AUDITORY, SensoryType.TACTILE),
                tabooLevel = TabooLevel.LIGHT,
                intimateSceneIntensity = 2,
                emotionExpressionStyle = EmotionStyle.EXPLICIT
            ),
            generationConfig = GenerationConfig(
                rhythmPreference = RhythmPreference.FAST_PACED,
                perspectiveMode = PerspectiveMode.THIRD_PERSON,
                intensityLevel = 8,
                pacingConfig = PacingConfig(
                    slowBuildupRatio = 0.2f,
                    progressionRatio = 0.3f,
                    climaxRatio = 0.35f,
                    aftermathRatio = 0.15f,
                    tensionPoints = 5,
                    releasePoints = 2
                )
            ),
            systemPromptAddition = """
                【热血风格强化】
                - 战斗场景占40%以上篇幅
                - 每章至少一个燃点（突破、觉醒、逆转）
                - 对话简洁有力，多感叹句
                - 节奏紧凑，短句为主
                - 逆境翻盘是核心节奏
            """.trimIndent()
        ),
        
        // 2. 慢热细腻
        GenerationPreset(
            id = "slow_delicate",
            name = "慢热细腻",
            description = "情感铺垫充足，描写细腻入微",
            icon = "🌙",
            sensoryProfile = SensoryProfile(
                descriptionDensity = 8,
                sensoryFocus = listOf(SensoryType.VISUAL, SensoryType.TACTILE, SensoryType.OLFACTORY, SensoryType.AUDITORY),
                tabooLevel = TabooLevel.MODERATE,
                intimateSceneIntensity = 7,
                emotionExpressionStyle = EmotionStyle.IMPLICIT
            ),
            generationConfig = GenerationConfig(
                rhythmPreference = RhythmPreference.SLOW_BURN,
                perspectiveMode = PerspectiveMode.THIRD_PERSON,
                intensityLevel = 6,
                pacingConfig = PacingConfig(
                    slowBuildupRatio = 0.5f,
                    progressionRatio = 0.25f,
                    climaxRatio = 0.1f,
                    aftermathRatio = 0.15f,
                    tensionPoints = 2,
                    releasePoints = 1
                )
            ),
            systemPromptAddition = """
                【慢热细腻风格强化】
                - 情感铺垫占50%以上篇幅
                - 感官描写密度高，注重细节
                - 情感含蓄表达，用动作替代直白描述
                - 长短段落交替，营造节奏感
                - 微表情和微小动作是情感传递的核心
            """.trimIndent()
        ),
        
        // 3. 悬疑推理
        GenerationPreset(
            id = "mystery_detective",
            name = "悬疑推理",
            description = "线索铺垫，逻辑严密，反转不断",
            icon = "🔍",
            sensoryProfile = SensoryProfile(
                descriptionDensity = 6,
                sensoryFocus = listOf(SensoryType.VISUAL, SensoryType.AUDITORY),
                tabooLevel = TabooLevel.LIGHT,
                intimateSceneIntensity = 3,
                emotionExpressionStyle = EmotionStyle.MODERATE
            ),
            generationConfig = GenerationConfig(
                rhythmPreference = RhythmPreference.BALANCED,
                perspectiveMode = PerspectiveMode.THIRD_PERSON,
                intensityLevel = 7,
                pacingConfig = PacingConfig(
                    slowBuildupRatio = 0.3f,
                    progressionRatio = 0.35f,
                    climaxRatio = 0.2f,
                    aftermathRatio = 0.15f,
                    tensionPoints = 4,
                    releasePoints = 3
                )
            ),
            systemPromptAddition = """
                【悬疑推理风格强化】
                - 每章至少埋下1条新线索
                - 线索呈碎片化分布，不集中揭露
                - 对话中隐藏关键信息
                - 角色各怀鬼胎，言行不一
                - 每3-5章设计一个小反转
                - 关键信息必须前文有伏笔（禁止天降信息）
                - 读者应能根据线索提前推理出部分真相
            """.trimIndent()
        ),
        
        // 4. 都市言情
        GenerationPreset(
            id = "urban_romance",
            name = "都市言情",
            description = "现代都市，情感驱动，关系纠葛",
            icon = "💕",
            sensoryProfile = SensoryProfile(
                descriptionDensity = 7,
                sensoryFocus = listOf(SensoryType.VISUAL, SensoryType.TACTILE, SensoryType.OLFACTORY),
                tabooLevel = TabooLevel.MODERATE,
                intimateSceneIntensity = 6,
                emotionExpressionStyle = EmotionStyle.IMPLICIT
            ),
            generationConfig = GenerationConfig(
                rhythmPreference = RhythmPreference.SLOW_BURN,
                perspectiveMode = PerspectiveMode.THIRD_PERSON,
                intensityLevel = 6,
                pacingConfig = PacingConfig(
                    slowBuildupRatio = 0.45f,
                    progressionRatio = 0.3f,
                    climaxRatio = 0.1f,
                    aftermathRatio = 0.15f,
                    tensionPoints = 3,
                    releasePoints = 2
                )
            ),
            systemPromptAddition = """
                【都市言情风格强化】
                - 人物关系是核心驱动，情感变化占40%以上
                - 社会背景真实：职场、家庭、社交圈
                - 对话要有潜台词，言外之意比直说更重要
                - 微表情和肢体语言传递情感
                - 冲突来源于误解、选择、现实压力
                - 感情发展要有合理的节奏，不突兀
                - 生活细节增加代入感：食物、穿着、天气
            """.trimIndent()
        ),
        
        // 5. 暗黑悬疑
        GenerationPreset(
            id = "dark_thriller",
            name = "暗黑悬疑",
            description = "心理惊悚，氛围压抑，反转密集",
            icon = "🌑",
            sensoryProfile = SensoryProfile(
                descriptionDensity = 7,
                sensoryFocus = listOf(SensoryType.VISUAL, SensoryType.AUDITORY, SensoryType.PROPRIOCEPTION),
                tabooLevel = TabooLevel.MODERATE,
                intimateSceneIntensity = 3,
                emotionExpressionStyle = EmotionStyle.RAW
            ),
            generationConfig = GenerationConfig(
                rhythmPreference = RhythmPreference.BALANCED,
                perspectiveMode = PerspectiveMode.FIRST_PERSON,
                intensityLevel = 8,
                pacingConfig = PacingConfig(
                    slowBuildupRatio = 0.35f,
                    progressionRatio = 0.3f,
                    climaxRatio = 0.2f,
                    aftermathRatio = 0.15f,
                    tensionPoints = 5,
                    releasePoints = 1
                )
            ),
            systemPromptAddition = """
                【暗黑悬疑风格强化】
                - 氛围压抑，每个场景都透着不安
                - 不可靠叙事：主角的认知可能有偏差
                - 每章至少一个疑点，线索碎片化分布
                - 角色各怀鬼胎，言行不一
                - 心理描写深入阴暗面
                - 反转要有伏笔支撑，禁止天降信息
                - 节奏在平静和惊悚间反复切换
            """.trimIndent()
        ),
        
        // 6. 轻松搞笑
        GenerationPreset(
            id = "comedy_fun",
            name = "轻松搞笑",
            description = "幽默为主，节奏轻快，反转逗趣",
            icon = "😄",
            sensoryProfile = SensoryProfile(
                descriptionDensity = 5,
                sensoryFocus = listOf(SensoryType.VISUAL, SensoryType.AUDITORY),
                tabooLevel = TabooLevel.LIGHT,
                intimateSceneIntensity = 2,
                emotionExpressionStyle = EmotionStyle.EXPLICIT
            ),
            generationConfig = GenerationConfig(
                rhythmPreference = RhythmPreference.FAST_PACED,
                perspectiveMode = PerspectiveMode.FIRST_PERSON,
                intensityLevel = 4,
                pacingConfig = PacingConfig(
                    slowBuildupRatio = 0.3f,
                    progressionRatio = 0.35f,
                    climaxRatio = 0.15f,
                    aftermathRatio = 0.2f,
                    tensionPoints = 2,
                    releasePoints = 4
                )
            ),
            systemPromptAddition = """
                【轻松搞笑风格强化】
                - 对话是核心，吐槽和金句频出
                - 反转要出人意料但逻辑自洽
                - 角色要有明显的性格标签，碰撞出笑点
                - 严肃场景突然打破，制造反差萌
                - 节奏明快，不要拖沓铺垫
                - 生活化场景为主，接地气
                - 禁止说教，用荒诞表达真理
            """.trimIndent()
        ),
        
        // 7. 日常治愈
        GenerationPreset(
            id = "daily_healing",
            name = "日常治愈",
            description = "温馨日常，小确幸，暖心治愈",
            icon = "☀️",
            sensoryProfile = SensoryProfile(
                descriptionDensity = 6,
                sensoryFocus = listOf(SensoryType.VISUAL, SensoryType.OLFACTORY, SensoryType.TASTE),
                tabooLevel = TabooLevel.LIGHT,
                intimateSceneIntensity = 3,
                emotionExpressionStyle = EmotionStyle.IMPLICIT
            ),
            generationConfig = GenerationConfig(
                rhythmPreference = RhythmPreference.SLOW_BURN,
                perspectiveMode = PerspectiveMode.FIRST_PERSON,
                intensityLevel = 3,
                pacingConfig = PacingConfig(
                    slowBuildupRatio = 0.5f,
                    progressionRatio = 0.3f,
                    climaxRatio = 0.05f,
                    aftermathRatio = 0.15f,
                    tensionPoints = 1,
                    releasePoints = 3
                )
            ),
            systemPromptAddition = """
                【日常治愈风格强化】
                - 日常细节是核心，食物、天气、季节变化
                - 情节轻缓，冲突温和
                - 对话温暖有趣，有生活气息
                - 感官描写偏向温暖舒适的方向
                - 小确幸是情感高潮
                - 章节结尾留有余韵
            """.trimIndent()
        ),
        
        // 8. 深度沉浸
        GenerationPreset(
            id = "deep_immersive",
            name = "深度沉浸",
            description = "全方位感官沉浸，深度情感体验",
            icon = "🌊",
            sensoryProfile = SensoryProfile(
                descriptionDensity = 9,
                sensoryFocus = listOf(SensoryType.VISUAL, SensoryType.TACTILE, SensoryType.AUDITORY, SensoryType.OLFACTORY, SensoryType.TASTE, SensoryType.PROPRIOCEPTION),
                tabooLevel = TabooLevel.DEEP,
                intimateSceneIntensity = 8,
                emotionExpressionStyle = EmotionStyle.RAW
            ),
            generationConfig = GenerationConfig(
                rhythmPreference = RhythmPreference.BALANCED,
                perspectiveMode = PerspectiveMode.FIRST_PERSON,
                intensityLevel = 9,
                pacingConfig = PacingConfig(
                    slowBuildupRatio = 0.4f,
                    progressionRatio = 0.3f,
                    climaxRatio = 0.15f,
                    aftermathRatio = 0.15f,
                    tensionPoints = 3,
                    releasePoints = 2
                )
            ),
            systemPromptAddition = """
                【深度沉浸风格强化】
                - 全感官描写，每个场景至少覆盖3种感官
                - 心理活动深度挖掘，不回避阴暗面
                - 身体感受精确描写
                - 情绪表达原生态，不加修饰
                - 沉浸式节奏，让读者身临其境
                - 场景切换需要过渡，不突兀跳转
            """.trimIndent()
        )
    )
    
    fun getPreset(id: String): GenerationPreset? {
        return PRESETS.find { it.id == id }
    }
    
    fun applyPreset(novel: Novel, presetId: String): Novel {
        val preset = getPreset(presetId) ?: return novel
        return novel.copy(
            sensoryProfile = preset.sensoryProfile,
            generationConfig = preset.generationConfig
        )
    }
}
