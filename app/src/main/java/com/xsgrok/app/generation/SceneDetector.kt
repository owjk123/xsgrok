package com.xsgrok.app.generation

import com.xsgrok.app.data.model.*

/**
 * 场景检测器
 * 检测内容中的场景类型并自动触发增强模式
 */
class SceneDetector {
    
    // 场景关键词映射
    private val sceneKeywords = mapOf(
        SceneType.INTIMATE to listOf(
            "亲吻", "拥抱", "缠绵", "亲密", "肌肤", "温度",
            "心跳", "呼吸", "目光", "嘴唇", "触碰", "相拥",
            "抚摸", "依偎", "缠绵", "交缠", "融合", "沉沦",
            "沦陷", "燃烧", "渴望", "欲望", "爱意", "激情"
        ),
        SceneType.CLIMAX to listOf(
            "高潮", "爆发", "临界", "顶峰", "巅峰", "极致",
            "顶点", "沸腾", "燃烧", "炸裂", "崩溃", "融化",
            "沦陷", "沉沦", "爆发", "倾泻", "释放", "解脱"
        ),
        SceneType.PHYSICAL to listOf(
            "抚摸", "触碰", "接触", "肌肤", "身体", "指尖",
            "掌心", "温度", "热度", "滚烫", "冰凉", "颤抖",
            "战栗", "酥麻", "电流", "敏感", "敏感点", "刺激"
        ),
        SceneType.EMOTIONAL to listOf(
            "心动", "暧昧", "情愫", "喜欢", "爱意", "喜欢",
            "心跳", "紧张", "害羞", "脸红", "羞涩", "甜蜜",
            "幸福", "温暖", "感动", "深情", "眷恋", "思念"
        ),
        SceneType.AMBIGUOUS to listOf(
            "靠近", "依偎", "肩膀", "手", "眼神", "注视",
            "对视", "凝望", "回避", "躲闪", "试探", "小心翼翼",
            "若即若离", "欲言又止", "欲拒还迎", "暧昧", "微妙"
        ),
        SceneType.TENSION to listOf(
            "紧张", "不安", "恐惧", "压力", "害怕", "担心",
            "焦虑", "犹豫", "挣扎", "纠结", "两难", "困境",
            "僵局", "对峙", "危险", "危机", "威胁", "压迫"
        ),
        SceneType.CONFLICT to listOf(
            "争吵", "冲突", "矛盾", "对峙", "争吵", "争执",
            "分歧", "对立", "对抗", "反对", "拒绝", "否定",
            "质疑", "误会", "隔阂", "疏远", "冷战", "僵持"
        ),
        SceneType.RESOLUTION to listOf(
            "和解", "解决", "释然", "放下", "原谅", "坦白",
            "坦诚", "沟通", "理解", "妥协", "让步", "接受",
            "认可", "肯定", "拥抱", "和好", "重归于好", "冰释"
        ),
        SceneType.AFTERMATH to listOf(
            "余韵", "回味", "余温", "温柔", "轻声", "呢喃",
            "低语", "依偎", "相拥", "静默", "沉默", "凝望",
            "微笑", "满足", "安心", "宁静", "平静", "平和"
        ),
        SceneType.DAILY to listOf(
            "日常", "平常", "普通", "正常", "说话", "聊天",
            "对话", "交流", "见面", "相遇", "认识", "相处"
        )
    )
    
    // 敏感动作关键词
    private val sensitiveActionKeywords = listOf(
        "吻", "拥抱", "抚摸", "触碰", "牵手", "搂", "抱",
        "贴近", "靠近", "相拥", "缠绵", "交缠", "融合",
        "依偎", "偎", "靠", "贴", "触碰", "触", "抚摸"
    )
    
    /**
     * 检测内容中的场景类型
     */
    fun detectSceneType(content: String): SceneType {
        val lowerContent = content.lowercase()
        val scores = mutableMapOf<SceneType, Int>()
        
        // 计算每种场景类型的匹配分数
        for ((sceneType, keywords) in sceneKeywords) {
            var score = 0
            for (keyword in keywords) {
                if (lowerContent.contains(keyword)) {
                    score += calculateKeywordScore(keyword, content)
                }
            }
            if (score > 0) {
                scores[sceneType] = score
            }
        }
        
        // 返回得分最高的场景类型
        return scores.maxByOrNull { it.value }?.key ?: SceneType.DAILY
    }
    
    /**
     * 计算关键词得分（考虑位置和上下文）
     */
    private fun calculateKeywordScore(keyword: String, content: String): Int {
        var score = 1
        
        // 在标题中出现的关键词得分更高
        val firstLine = content.lines().firstOrNull() ?: ""
        if (firstLine.contains(keyword)) {
            score += 3
        }
        
        // 在对话中出现的关键词
        if (content.lines().any { line -> line.startsWith("「") && line.contains(keyword) }) {
            score += 2
        }
        
        // 多次出现
        val occurrences = content.windowed(keyword.length) { it == keyword }.count { it }
        if (occurrences > 1) {
            score += minOf(occurrences - 1, 3)
        }
        
        return score
    }
    
    /**
     * 检测是否需要增强模式
     */
    fun shouldEnhanceMode(content: String): Boolean {
        val sceneType = detectSceneType(content)
        
        return when (sceneType) {
            SceneType.INTIMATE, SceneType.CLIMAX, SceneType.PHYSICAL -> true
            else -> false
        }
    }
    
    /**
     * 获取场景强度
     */
    fun getSceneIntensity(content: String, profile: SensoryProfile): Int {
        val sceneType = detectSceneType(content)
        
        val baseIntensity = when (sceneType) {
            SceneType.DAILY -> 2
            SceneType.AMBIGUOUS -> 4
            SceneType.EMOTIONAL -> 5
            SceneType.PHYSICAL -> 6
            SceneType.INTIMATE -> 7
            SceneType.CLIMAX -> 9
            SceneType.AFTERMATH -> 3
            SceneType.TENSION, SceneType.CONFLICT -> 5
            SceneType.RESOLUTION -> 4
        }
        
        // 与配置强度结合
        return ((baseIntensity + profile.intimateSceneIntensity) / 2).coerceIn(1, 10)
    }
    
    /**
     * 检测亲密动作
     */
    fun detectIntimateActions(content: String): List<String> {
        val detectedActions = mutableListOf<String>()
        
        for (keyword in sensitiveActionKeywords) {
            if (content.contains(keyword)) {
                detectedActions.add(keyword)
            }
        }
        
        return detectedActions.distinct()
    }
    
    /**
     * 估算描写深度
     */
    fun estimateDescriptionDepth(content: String, profile: SensoryProfile): Int {
        var depth = profile.descriptionDensity
        
        // 根据场景类型调整
        val sceneType = detectSceneType(content)
        val sceneDepthModifier = when (sceneType) {
            SceneType.INTIMATE, SceneType.CLIMAX -> +2
            SceneType.PHYSICAL -> +1
            SceneType.EMOTIONAL, SceneType.AFTERMATH -> 0
            SceneType.AMBIGUOUS -> -1
            else -> -2
        }
        
        depth = (depth + sceneDepthModifier).coerceIn(1, 10)
        
        // 根据内容长度调整
        val wordCount = content.length
        if (wordCount < 1000) {
            depth = minOf(depth, 5)
        } else if (wordCount > 3000) {
            depth = (depth + 1).coerceAtMost(10)
        }
        
        return depth
    }
    
    /**
     * 生成场景增强提示
     */
    fun generateEnhancementHint(
        content: String,
        profile: SensoryProfile
    ): String {
        val sceneType = detectSceneType(content)
        val intensity = getSceneIntensity(content, profile)
        val depth = estimateDescriptionDepth(content, profile)
        
        return buildString {
            appendLine("【场景检测结果】")
            appendLine("- 场景类型：${sceneType.name}")
            appendLine("- 强度等级：$intensity/10")
            appendLine("- 描写深度：$depth/10")
            appendLine()
            
            // 场景特定指导
            appendLine("【场景指导】")
            when (sceneType) {
                SceneType.INTIMATE -> {
                    appendLine("1. 增强感官描写层次")
                    appendLine("2. 深化心理活动")
                    appendLine("3. 注重氛围营造")
                    appendLine("4. 保持情感真实性")
                    appendLine()
                    appendLine("【注入描写】")
                    appendLine("- 触觉：${profile.sensoryFocus.contains(SensoryType.TACTILE)}")
                    appendLine("- 视觉：${profile.sensoryFocus.contains(SensoryType.VISUAL)}")
                    appendLine("- 听觉：${profile.sensoryFocus.contains(SensoryType.AUDITORY)}")
                }
                
                SceneType.CLIMAX -> {
                    appendLine("1. 情感爆发描写")
                    appendLine("2. 感官极致体验")
                    appendLine("3. 节奏加快")
                    appendLine("4. 张力最大化")
                }
                
                SceneType.PHYSICAL -> {
                    appendLine("1. 身体感觉描写")
                    appendLine("2. 细节刻画")
                    appendLine("3. 感受层次递进")
                }
                
                SceneType.EMOTIONAL -> {
                    appendLine("1. 心理活动深化")
                    appendLine("2. 情感细节描写")
                    appendLine("3. 内心变化刻画")
                }
                
                else -> {
                    appendLine("保持当前描写风格")
                }
            }
            
            // 根据强度注入描写指令
            if (intensity >= 7) {
                appendLine()
                appendLine("【高级指令】")
                appendLine("- 使用多感官协同描写")
                appendLine("- 注重内心独白的层次")
                appendLine("- 营造沉浸式氛围")
            }
        }
    }
    
    /**
     * 检测场景变化
     */
    fun detectSceneTransition(
        previousContent: String?,
        currentContent: String
    ): SceneTransition? {
        if (previousContent == null) return null
        
        val previousType = detectSceneType(previousContent)
        val currentType = detectSceneType(currentContent)
        
        return if (previousType != currentType) {
            SceneTransition(
                fromType = previousType,
                toType = currentType,
                isSmooth = isSmoothTransition(previousType, currentType)
            )
        } else {
            null
        }
    }
    
    /**
     * 判断过渡是否平滑
     */
    private fun isSmoothTransition(from: SceneType, to: SceneType): Boolean {
        val smoothTransitions = mapOf(
            SceneType.DAILY to listOf(SceneType.AMBIGUOUS, SceneType.EMOTIONAL),
            SceneType.AMBIGUOUS to listOf(SceneType.EMOTIONAL, SceneType.PHYSICAL),
            SceneType.EMOTIONAL to listOf(SceneType.PHYSICAL, SceneType.INTIMATE),
            SceneType.PHYSICAL to listOf(SceneType.INTIMATE, SceneType.CLIMAX),
            SceneType.INTIMATE to listOf(SceneType.CLIMAX, SceneType.AFTERMATH),
            SceneType.CLIMAX to listOf(SceneType.AFTERMATH),
            SceneType.AFTERMATH to listOf(SceneType.DAILY, SceneType.EMOTIONAL)
        )
        
        return to in (smoothTransitions[from] ?: emptyList())
    }
    
    /**
     * 分析情感曲线
     */
    fun analyzeEmotionalArc(content: String): EmotionalArcAnalysis {
        val paragraphs = content.split("\n\n")
        val emotionalLevels = paragraphs.mapIndexed { index, para ->
            val intensity = getSceneIntensity(para, SensoryProfile())
            index to intensity
        }
        
        val startLevel = emotionalLevels.firstOrNull()?.second ?: 0
        val peakLevel = emotionalLevels.maxOfOrNull { it.second } ?: 0
        val endLevel = emotionalLevels.lastOrNull()?.second ?: 0
        
        val trend = when {
            endLevel > startLevel -> "上升"
            endLevel < startLevel -> "下降"
            else -> "平稳"
        }
        
        return EmotionalArcAnalysis(
            startLevel = startLevel,
            peakLevel = peakLevel,
            endLevel = endLevel,
            trend = trend,
            paragraphCount = paragraphs.size
        )
    }
    
    /**
     * 场景过渡数据类
     */
    data class SceneTransition(
        val fromType: SceneType,
        val toType: SceneType,
        val isSmooth: Boolean
    )
    
    /**
     * 情感曲线分析结果
     */
    data class EmotionalArcAnalysis(
        val startLevel: Int,
        val peakLevel: Int,
        val endLevel: Int,
        val trend: String,
        val paragraphCount: Int
    )
}
