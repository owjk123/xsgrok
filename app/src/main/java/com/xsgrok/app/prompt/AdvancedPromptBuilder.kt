package com.xsgrok.app.prompt

import com.xsgrok.app.agent.AgentOrchestrator
import com.xsgrok.app.data.model.*

/**
 * 高级Prompt构建器 - 注入专业长篇小说写作技法
 */

object AdvancedPromptBuilder {
    
    /** 反AI味写作法则 */
    val ANTI_AI_RULES = """
【反AI味写作法则 - 铁律】
1. 禁止工整排比：不要连续3个以上相同句式
2. 禁止空泛比喻：不要"仿佛xxx一般"式的模糊描写
3. 禁止过度修饰：一个名词前最多1个形容词
4. 段落长短交替：每段3-8句，必须有1句段和1个长段（8句+）
5. 用动作替代心理：不要"他很愤怒"，要"把烟头摁进掌心"
6. 对话自然化：夹杂语气词、打断、省略，避免工整问答
7. 场景过渡要自然：不要突兀跳转，用感官过渡（"耳边的风声渐渐安静下来"）
8. 避免总结式结尾：不要"这就是xxx的一天"，用画面定格
9. 角色说话要有辨识度：不同角色的用词、语气、习惯应有区别
10. 信息要展示不要讲述：不要"他是个强者"，要让他在行动中展现
""".trimIndent()
    
    /** 节奏控制技法 */
    fun getRhythmTechniques(phase: RhythmPhase): String {
        return when (phase) {
            RhythmPhase.SLOW_BUILDUP -> """
【慢热阶段技法】
- 日常生活切入，建立角色常态
- 环境描写占比40%，营造氛围
- 小细节暗示大趋势（天气变化、人物微表情）
- 对话多于行动，通过对话推进信息
- 章节结尾设小悬念，牵引读者
""".trimIndent()
            
            RhythmPhase.PROGRESSION -> """
【推进阶段技法】
- 事件密度递增，节奏渐快
- 短句增多，段落缩短
- 对话减少，行动增多
- 时间压缩：大段时间一笔带过
- 冲突逐级升级
""".trimIndent()
            
            RhythmPhase.CLIMAX -> """
【高潮阶段技法】
- 短句密集，甚至单字成段
- 感官全面激活：视觉+听觉+触觉+嗅觉
- 内心独白极少，完全由行动驱动
- 节奏像鼓点：紧凑、有力、不留喘息
- 关键时刻用慢镜头效果：1秒的事写3段
""".trimIndent()
            
            RhythmPhase.AFTERMATH -> """
【余韵阶段技法】
- 长句为主，节奏放缓
- 内心独白回归，反思和沉淀
- 环境描写呼应情绪
- 留白：不说透，让读者自己感受
- 结尾留钩子：一个细节暗示新的危机
""".trimIndent()
        }
    }
    
    /** 张力管理技法 */
    fun getTensionTechniques(level: Int): String {
        return when {
            level < 3 -> """
【低张力场景技法】
- 长段落，慢节奏
- 详细的环境和感官描写
- 角色内心活动丰富
- 对话悠闲，有闲聊
""".trimIndent()
            level < 7 -> """
【中等张力场景技法】
- 段落中等长度
- 描写和行动交替
- 对话简洁，目的明确
- 微小冲突持续存在
""".trimIndent()
            else -> """
【高张力场景技法】
- 短句为主，段落极短
- 动作连续，不打断
- 五感全部激活
- 内心活动压缩到最少
- 时间感扭曲：紧张时放慢，危险时加速
""".trimIndent()
        }
    }
    
    /** 场景过渡技法 */
    fun getTransitionTechniques(): String {
        return """
【场景过渡技法】
1. 感官过渡：用一种感官收束旧场景，另一种感官开启新场景
2. 时间过渡：用环境变化暗示时间流逝（光线、温度、声音）
3. 情绪过渡：旧场景的情绪延续到新场景的开头
4. 物件过渡：一个道具连接两个场景
5. 对话过渡：在对话中自然切换话题和场景
6. 绝对禁止：不要用分隔符、时间戳等机械方式切换场景
""".trimIndent()
    }
    
    /** 角色对话技法 */
    fun getDialogueTechniques(): String {
        return """
【角色对话技法】
1. 每个角色有独特的说话方式（用词、句式、口头禅）
2. 真实对话特征：打断、省略、答非所问、言外之意
3. 对话中穿插动作描写，不要纯对话
4. 重要信息不要直接说出，通过暗示和误解传达
5. 对话节奏与场景张力匹配
6. 避免说教式对话
""".trimIndent()
    }
    
    /** 构建完整的写作技法Prompt */
    fun buildWritingTechniquesPrompt(
        novel: Novel, 
        chapterNum: Int,
        stage: AgentOrchestrator.GenerationStage
    ): String {
        val rhythmPhase = when (novel.generationConfig.rhythmPreference) {
            RhythmPreference.SLOW_BURN -> {
                val progress = if (novel.keyNodes.isNotEmpty()) 
                    chapterNum.toFloat() / (novel.keyNodes.size * 5) else 0f
                when {
                    progress < 0.4f -> RhythmPhase.SLOW_BUILDUP
                    progress < 0.7f -> RhythmPhase.PROGRESSION
                    progress < 0.85f -> RhythmPhase.CLIMAX
                    else -> RhythmPhase.AFTERMATH
                }
            }
            RhythmPreference.FAST_PACED -> {
                val progress = if (novel.keyNodes.isNotEmpty())
                    chapterNum.toFloat() / (novel.keyNodes.size * 3) else 0f
                when {
                    progress < 0.2f -> RhythmPhase.SLOW_BUILDUP
                    progress < 0.5f -> RhythmPhase.PROGRESSION
                    progress < 0.8f -> RhythmPhase.CLIMAX
                    else -> RhythmPhase.AFTERMATH
                }
            }
            else -> RhythmPhase.PROGRESSION
        }
        
        val tensionLevel = when (stage) {
            AgentOrchestrator.GenerationStage.PLANNING -> 3
            AgentOrchestrator.GenerationStage.EXECUTING -> novel.generationConfig.intensityLevel
            AgentOrchestrator.GenerationStage.REVIEWING -> 3
            AgentOrchestrator.GenerationStage.POLISHING -> 5
        }
        
        return buildString {
            appendLine(ANTI_AI_RULES)
            appendLine()
            appendLine(getRhythmTechniques(rhythmPhase))
            appendLine()
            appendLine(getTensionTechniques(tensionLevel))
            appendLine()
            
            if (stage == AgentOrchestrator.GenerationStage.EXECUTING || 
                stage == AgentOrchestrator.GenerationStage.POLISHING) {
                appendLine(getTransitionTechniques())
                appendLine()
                appendLine(getDialogueTechniques())
            }
        }
    }
}
