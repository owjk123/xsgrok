package com.xsgrok.app.generation

import com.xsgrok.app.data.model.*

/**
 * 节奏控制器
 * 管理章节内的情感节奏和描写强度
 */
class RhythmController {
    
    /**
     * 确定当前章节的节奏阶段
     */
    fun determinePhase(novel: Novel, chapterNum: Int): RhythmPhase {
        val progress = calculateProgress(novel, chapterNum)
        
        return when {
            progress < 0.3f -> RhythmPhase.SLOW_BUILDUP
            progress < 0.7f -> RhythmPhase.PROGRESSION
            progress < 0.9f -> RhythmPhase.CLIMAX
            else -> RhythmPhase.AFTERMATH
        }
    }
    
    /**
     * 计算章节进度
     */
    private fun calculateProgress(novel: Novel, chapterNum: Int): Float {
        return if (novel.keyNodes.isNotEmpty()) {
            val nodeProgress = novel.currentNodeIndex.toFloat() / novel.keyNodes.size
            val chapterProgress = chapterNum.toFloat() / (novel.keyNodes.lastOrNull()?.targetChapter ?: 10)
            (nodeProgress + chapterProgress) / 2
        } else {
            // 如果没有节点信息，使用章节数量估算
            minOf(1f, chapterNum.toFloat() / 20)
        }
    }
    
    /**
     * 获取节奏配置
     */
    fun getPacingConfig(novel: Novel): PacingConfig {
        return novel.generationConfig.pacingConfig
    }
    
    /**
     * 生成节奏提示
     */
    fun generateRhythmHint(novel: Novel, chapterNum: Int): String {
        val phase = determinePhase(novel, chapterNum)
        val config = getPacingConfig(novel)
        
        return buildString {
            appendLine("【节奏控制】")
            appendLine("- 当前阶段：${phase.displayName}")
            appendLine("- 阶段描述：${phase.description}")
            appendLine("- 目标字数比例：${(phase.targetWordRatio * 100).toInt()}%")
            appendLine()
            
            // 根据节奏偏好调整
            appendLine("【节奏偏好】")
            appendLine("- ${novel.generationConfig.rhythmPreference.description}")
            
            // 根据阶段提供具体指导
            appendLine()
            appendLine("【阶段指导】")
            when (phase) {
                RhythmPhase.SLOW_BUILDUP -> {
                    appendLine("1. 缓慢建立场景氛围")
                    appendLine("2. 通过细节铺垫情感")
                    appendLine("3. 创造微妙的张力")
                    appendLine("4. 为后续高潮积蓄能量")
                    appendLine()
                    appendLine("【字数分配】")
                    appendLine("- 铺垫：40%")
                    appendLine("- 积累：40%")
                    appendLine("- 悬念：20%")
                }
                
                RhythmPhase.PROGRESSION -> {
                    appendLine("1. 逐步升温情感")
                    appendLine("2. 增加互动频率")
                    appendLine("3. 深化关系层次")
                    appendLine("4. 引入新的张力点")
                    appendLine()
                    appendLine("【字数分配】")
                    appendLine("- 延续：30%")
                    appendLine("- 升温：40%")
                    appendLine("- 新发展：30%")
                }
                
                RhythmPhase.CLIMAX -> {
                    appendLine("1. 情感达到顶峰")
                    appendLine("2. 感官描写达到最强")
                    appendLine("3. 关系突破或转折")
                    appendLine("4. 保持紧张感直到顶点")
                    appendLine()
                    appendLine("【字数分配】")
                    appendLine("- 攀升：30%")
                    appendLine("- 爆发：50%")
                    appendLine("- 转折：20%")
                }
                
                RhythmPhase.AFTERMATH -> {
                    appendLine("1. 情感平复期")
                    appendLine("2. 余韵回味")
                    appendLine("3. 关系确认或新动态")
                    appendLine("4. 为下一章节铺垫")
                    appendLine()
                    appendLine("【字数分配】")
                    appendLine("- 余韵：40%")
                    appendLine("- 沉淀：30%")
                    appendLine("- 铺垫：30%")
                }
            }
            
            // 紧张点和释放点
            appendLine()
            appendLine("【节奏标记】")
            appendLine("- 紧张点数量：${config.tensionPoints}")
            appendLine("- 释放点数量：${config.releasePoints}")
        }
    }
    
    /**
     * 计算目标字数
     */
    fun calculateTargetWordCount(novel: Novel, chapterNum: Int): Int {
        val baseWordCount = 4000 // 基础字数
        val phase = determinePhase(novel, chapterNum)
        
        // 根据阶段调整
        val phaseModifier = when (phase) {
            RhythmPhase.SLOW_BUILDUP -> 0.9f  // 略少
            RhythmPhase.PROGRESSION -> 1.0f    // 标准
            RhythmPhase.CLIMAX -> 1.2f         // 略多
            RhythmPhase.AFTERMATH -> 0.85f     // 略少
        }
        
        // 根据节奏偏好调整
        val rhythmModifier = when (novel.generationConfig.rhythmPreference) {
            RhythmPreference.SLOW_BURN -> 1.1f
            RhythmPreference.BALANCED -> 1.0f
            RhythmPreference.FAST_PACED -> 0.9f
        }
        
        // 根据关系进度调整
        val intimacyModifier = 1f + (novel.intimacyProgress * 0.2f)
        
        return (baseWordCount * phaseModifier * rhythmModifier * intimacyModifier).toInt()
    }
    
    /**
     * 生成节奏过渡提示
     */
    fun generateTransitionHint(
        fromPhase: RhythmPhase,
        toPhase: RhythmPhase
    ): String {
        return buildString {
            appendLine("【节奏过渡】")
            appendLine("从「${fromPhase.displayName}」过渡到「${toPhase.displayName}」")
            appendLine()
            
            // 过渡指导
            when {
                fromPhase == RhythmPhase.SLOW_BUILDUP && toPhase == RhythmPhase.PROGRESSION -> {
                    appendLine("过渡方式：")
                    appendLine("1. 从细节描写转向互动描写")
                    appendLine("2. 增加对话和交流")
                    appendLine("3. 情感开始升温")
                    appendLine("4. 保持张力但逐渐增强")
                }
                
                fromPhase == RhythmPhase.PROGRESSION && toPhase == RhythmPhase.CLIMAX -> {
                    appendLine("过渡方式：")
                    appendLine("1. 加快节奏")
                    appendLine("2. 增强感官描写")
                    appendLine("3. 制造紧迫感")
                    appendLine("4. 积累的情感即将爆发")
                }
                
                fromPhase == RhythmPhase.CLIMAX && toPhase == RhythmPhase.AFTERMATH -> {
                    appendLine("过渡方式：")
                    appendLine("1. 从激烈转向平缓")
                    appendLine("2. 注重余韵描写")
                    appendLine("3. 情感交流和确认")
                    appendLine("4. 为后续发展铺垫")
                }
                
                else -> {
                    appendLine("自然过渡，保持流畅")
                }
            }
        }
    }
    
    /**
     * 生成强度曲线
     */
    fun generateIntensityCurve(
        novel: Novel,
        chapterNum: Int,
        sceneCount: Int
    ): List<Pair<String, Int>> {
        val phase = determinePhase(novel, chapterNum)
        val baseIntensity = novel.sensoryProfile.intimateSceneIntensity
        
        return when (phase) {
            RhythmPhase.SLOW_BUILDUP -> {
                // 缓慢上升曲线
                listOf(
                    "scene_1" to (baseIntensity * 0.3).toInt(),
                    "scene_2" to (baseIntensity * 0.5).toInt(),
                    "scene_3" to (baseIntensity * 0.7).toInt(),
                    "scene_4" to (baseIntensity * 0.6).toInt() // 最后稍微下降，留悬念
                )
            }
            
            RhythmPhase.PROGRESSION -> {
                // 波浪式上升
                listOf(
                    "scene_1" to (baseIntensity * 0.5).toInt(),
                    "scene_2" to (baseIntensity * 0.7).toInt(),
                    "scene_3" to (baseIntensity * 0.6).toInt(),
                    "scene_4" to (baseIntensity * 0.8).toInt(),
                    "scene_5" to (baseIntensity * 0.7).toInt()
                )
            }
            
            RhythmPhase.CLIMAX -> {
                // 快速上升然后保持在高点
                listOf(
                    "scene_1" to (baseIntensity * 0.8).toInt(),
                    "scene_2" to (baseIntensity * 1.0).toInt() // 顶峰
                )
            }
            
            RhythmPhase.AFTERMATH -> {
                // 从高点缓慢下降
                listOf(
                    "scene_1" to (baseIntensity * 0.7).toInt(),
                    "scene_2" to (baseIntensity * 0.5).toInt(),
                    "scene_3" to (baseIntensity * 0.4).toInt()
                )
            }
        }.take(sceneCount)
    }
    
    /**
     * 获取节奏阶段对应的描写深度
     */
    fun getDepthForPhase(phase: RhythmPhase, tabooLevel: TabooLevel): Int {
        val baseDepth = when (phase) {
            RhythmPhase.SLOW_BUILDUP -> 3
            RhythmPhase.PROGRESSION -> 5
            RhythmPhase.CLIMAX -> 8
            RhythmPhase.AFTERMATH -> 4
        }
        
        // 根据禁忌等级调整
        val levelModifier = when (tabooLevel) {
            TabooLevel.LIGHT -> 0.5f
            TabooLevel.MODERATE -> 0.75f
            TabooLevel.DEEP -> 1.0f
            TabooLevel.UNRESTRICTED -> 1.25f
        }
        
        return (baseDepth * levelModifier).toInt().coerceIn(1, 10)
    }
}
