package com.xsgrok.app.prompt

import com.xsgrok.app.data.model.*

/**
 * Prompt模板管理器
 * 提供不同强度等级和场景专用的Prompt模板
 */
object PromptTemplates {
    
    // ========== 基础模板系列（按强度分级） ==========
    
    /**
     * 获取指定等级的Prompt模板
     */
    fun getTemplate(level: TabooLevel): PromptTemplate {
        return when (level) {
            TabooLevel.LIGHT -> createLightTemplate()
            TabooLevel.MODERATE -> createModerateTemplate()
            TabooLevel.DEEP -> createDeepTemplate()
            TabooLevel.UNRESTRICTED -> createUnrestrictedTemplate()
        }
    }
    
    /**
     * 轻度模板 - 温馨日常向，含蓄暗示
     */
    private fun createLightTemplate(): PromptTemplate {
        return PromptTemplate(
            name = "轻度模板",
            level = TabooLevel.LIGHT,
            basePrompt = """
                你是一位专注于温馨浪漫的网文作家。
                
                【写作风格】
                - 情感细腻温柔，注重心灵交流
                - 含蓄暗示，不过分直白
                - 细节描写点到为止
                - 氛围营造为主
                
                【描写要求】
                - 注重眼神、表情的细腻变化
                - 强调情感和心理层面
                - 身体接触轻描淡写
                - 留白给读者想象空间
                
                【禁止内容】
                - 露骨的身体描写
                - 过于直白的亲密场景
            """.trimIndent(),
            scenePrompts = mutableMapOf(
                SceneType.DAILY to """
                    【日常场景】
                    描写温馨的日常互动，注重细节和氛围。
                    突出角色的性格特点和情感变化。
                """.trimIndent(),
                SceneType.AMBIGUOUS to """
                    【暧昧铺垫】
                    营造心动的氛围，但不点破。
                    描写若有若无的暧昧感。
                """.trimIndent(),
                SceneType.EMOTIONAL to """
                    【情感升温】
                    深化情感连接，注重内心独白。
                    情感表达含蓄而真挚。
                """.trimIndent(),
                SceneType.PHYSICAL to """
                    【身体接触】
                    轻描淡写，点到为止。
                    注重感觉而非具体动作。
                """.trimIndent(),
                SceneType.INTIMATE to """
                    【亲密场景】
                    含蓄表达，注重情感层面。
                    暗示为主，不直白描写。
                """.trimIndent(),
                SceneType.CLIMAX to """
                    【情感高潮】
                    情感爆发而非身体描写。
                    注重心灵的契合与共鸣。
                """.trimIndent(),
                SceneType.AFTERMATH to """
                    【后戏余韵】
                    温馨的后续互动。
                    注重情感余韵。
                """.trimIndent()
            ),
            instructionLibrary = InstructionLibrary(
                visualDescriptions = mutableListOf(
                    "眼神交汇时的温柔光芒",
                    "嘴角上扬的细微弧度",
                    "微微泛红的脸颊",
                    "低头时的羞涩",
                    "手指轻轻蜷缩的紧张"
                ),
                tactileDescriptions = mutableListOf(
                    "指尖相触的温度",
                    "轻轻的握手",
                    "隔着衣料的轻触"
                ),
                psychologicalDescriptions = mutableListOf(
                    "心中的小鹿乱撞",
                    "脸上一阵发烫",
                    "紧张得说不出话"
                ),
                auditoryDescriptions = mutableListOf(
                    "轻柔的呼吸声",
                    "微微加快的心跳",
                    "轻声细语的温柔"
                )
            )
        )
    }
    
    /**
     * 中度模板 - 情感细腻，适度感官
     */
    private fun createModerateTemplate(): PromptTemplate {
        return PromptTemplate(
            name = "中度模板",
            level = TabooLevel.MODERATE,
            basePrompt = """
                你是一位擅长细腻情感描写的网文作家。
                
                【写作风格】
                - 情感与感官并重
                - 描写适度，不过分露骨
                - 注重氛围营造
                - 细节丰富但不冗余
                
                【描写要求】
                - 感官描写注重层次感
                - 情感表达真挚自然
                - 身体接触有度有感
                - 节奏把控张弛有度
                
                【描写密度】
                - 视觉：重点突出
                - 触觉：细腻有层次
                - 听觉：辅助氛围
                - 嗅觉：点缀使用
            """.trimIndent(),
            scenePrompts = mutableMapOf(
                SceneType.DAILY to """
                    【日常场景】
                    描写自然的日常互动。
                    通过细节展现角色关系和性格。
                """.trimIndent(),
                SceneType.AMBIGUOUS to """
                    【暧昧铺垫】
                    营造心照不宣的暧昧感。
                    通过细节描写暗示情感。
                """.trimIndent(),
                SceneType.EMOTIONAL to """
                    【情感升温】
                    情感层层递进。
                    注重心理变化和外在表现的一致性。
                """.trimIndent(),
                SceneType.PHYSICAL to """
                    【身体接触】
                    描写有一定深度但不露骨。
                    注重感觉和情感的双重表达。
                """.trimIndent(),
                SceneType.INTIMATE to """
                    【亲密场景】
                    适度的感官描写。
                    情感和身体描写并重。
                    注重氛围和感觉。
                """.trimIndent(),
                SceneType.CLIMAX to """
                    【情感高潮】
                    情感和感官双重爆发。
                    注重身心合一的描写。
                """.trimIndent(),
                SceneType.AFTERMATH to """
                    【后戏余韵】
                    温情脉脉的后续。
                    注重情感回味和交流。
                """.trimIndent()
            ),
            instructionLibrary = InstructionLibrary(
                visualDescriptions = mutableListOf(
                    "微光下皮肤泛着柔和的光泽",
                    "睫毛轻颤投下的阴影",
                    "汗珠顺着脖颈滑落",
                    "微微开启的唇",
                    "眼神中的渴望与克制",
                    "手指扣在一起的力度"
                ),
                tactileDescriptions = mutableListOf(
                    "掌心传来的滚烫温度",
                    "指尖划过肌肤的酥麻",
                    "呼吸喷洒在颈侧的热度",
                    "手指穿过发丝的轻柔",
                    "皮肤相贴时的微微颤抖"
                ),
                psychologicalDescriptions = mutableListOf(
                    "理智在崩塌边缘挣扎",
                    "身体的本能超越思维",
                    "渴望与紧张交织",
                    "全身的感官都在放大"
                ),
                auditoryDescriptions = mutableListOf(
                    "压抑的喘息",
                    "心跳如擂鼓",
                    "声音沙哑的低语",
                    "呼吸交缠的节奏",
                    "衣物摩擦的细微声响"
                ),
                olfactoryDescriptions = mutableListOf(
                    "混合的体香",
                    "发丝间的气息",
                    "空气中弥漫的暧昧味道"
                )
            )
        )
    }
    
    /**
     * 深度模板 - 沉浸式体验，丰富细节
     */
    private fun createDeepTemplate(): PromptTemplate {
        return PromptTemplate(
            name = "深度模板",
            level = TabooLevel.DEEP,
            basePrompt = """
                你是一位擅长沉浸式情感描写的网文作家。
                
                【写作风格】
                - 沉浸式体验，强调代入感
                - 感官描写丰富立体
                - 注重身心合一
                - 细节真实有质感
                
                【描写要求】
                - 多感官协同描写
                - 心理和身体同步刻画
                - 节奏感强烈
                - 注重层次和递进
                
                【描写重点】
                - 身体感受的细微变化
                - 心理活动的层次递进
                - 氛围的层层渲染
                - 情绪的积累和爆发
            """.trimIndent(),
            scenePrompts = mutableMapOf(
                SceneType.DAILY to """
                    【日常场景】
                    通过日常细节建立亲密感。
                    为后续情感发展铺垫。
                """.trimIndent(),
                SceneType.AMBIGUOUS to """
                    【暧昧铺垫】
                    细腻的暧昧互动。
                    通过肢体语言传递情感。
                """.trimIndent(),
                SceneType.EMOTIONAL to """
                    【情感升温】
                    深入的情感剖析。
                    情感积累和递进。
                """.trimIndent(),
                SceneType.PHYSICAL to """
                    【身体接触】
                    丰富细腻的身体描写。
                    感觉层次分明。
                """.trimIndent(),
                SceneType.INTIMATE to """
                    【亲密场景】
                    沉浸式感官描写。
                    身心合一的感觉刻画。
                    氛围渲染强烈。
                """.trimIndent(),
                SceneType.CLIMAX to """
                    【情感高潮】
                    全感官的高潮描写。
                    情感和身体的极致体验。
                """.trimIndent(),
                SceneType.AFTERMATH to """
                    【后戏余韵】
                    细腻的后续描写。
                    余韵悠长，回味无穷。
                """.trimIndent()
            ),
            instructionLibrary = InstructionLibrary(
                visualDescriptions = mutableListOf(
                    "皮肤因充血而泛红",
                    "汗珠在肌肉线条上滑落",
                    "眼神涣散又聚焦",
                    "唇瓣因亲吻而微微红肿",
                    "身体因刺激而不自觉弓起",
                    "指节因用力而泛白",
                    "颈侧跳动的脉搏"
                ),
                tactileDescriptions = mutableListOf(
                    "指尖传来的脉搏跳动",
                    "掌心贴着的滚烫体温",
                    "嘴唇触碰时的电流感",
                    "肌肤相贴的每一寸都在燃烧",
                    "呼吸带来的灼热感",
                    "指尖划过时的战栗",
                    "身体不由自主的颤抖"
                ),
                psychologicalDescriptions = mutableListOf(
                    "理智在燃烧中化为灰烬",
                    "感官被无限放大",
                    "身体比意识更诚实",
                    "在欲望中沉沦的感觉",
                    "突破临界点时的心悸"
                ),
                auditoryDescriptions = mutableListOf(
                    "压抑不住的轻吟",
                    "急促的喘息",
                    "身体碰撞的闷响",
                    "沙哑的嗓音",
                    "从喉咙深处溢出的声音"
                ),
                olfactoryDescriptions = mutableListOf(
                    "汗水和体香混合的气息",
                    "空气中弥漫的荷尔蒙味道",
                    "发丝间残留的洗发水香"
                ),
                tasteDescriptions = mutableListOf(
                    "舌尖相触的甜蜜",
                    "唾液交换的温度",
                    "唇齿间的缠绵"
                )
            )
        )
    }
    
    /**
     * 无限制模板 - 完整表达
     */
    private fun createUnrestrictedTemplate(): PromptTemplate {
        return PromptTemplate(
            name = "无限制模板",
            level = TabooLevel.UNRESTRICTED,
            basePrompt = """
                你是一位擅长成人向情感描写的网文作家。
                
                【写作风格】
                - 完整表达，无限制
                - 感官描写极致细腻
                - 注重身心真实感受
                - 氛围营造沉浸式
                
                【描写要求】
                - 全感官调动
                - 细节真实有质感
                - 心理刻画深入
                - 节奏把控精准
                
                【描写广度】
                - 身体的每一处感受
                - 心理的每一个波动
                - 情绪的每一次变化
                - 环境的每一丝渲染
            """.trimIndent(),
            scenePrompts = mutableMapOf(
                SceneType.DAILY to """
                    【日常场景】
                    通过亲密的日常互动建立关系。
                """.trimIndent(),
                SceneType.AMBIGUOUS to """
                    【暧昧铺垫】
                    充满张力的暧昧互动。
                """.trimIndent(),
                SceneType.EMOTIONAL to """
                    【情感升温】
                    深入剖析情感变化。
                """.trimIndent(),
                SceneType.PHYSICAL to """
                    【身体接触】
                    全面丰富的身体描写。
                """.trimIndent(),
                SceneType.INTIMATE to """
                    【亲密场景】
                    完整沉浸式描写。
                    极致的身心体验。
                """.trimIndent(),
                SceneType.CLIMAX to """
                    【情感高潮】
                    完整的高潮体验。
                """.trimIndent(),
                SceneType.AFTERMATH to """
                    【后戏余韵】
                    完整的后续描写。
                """.trimIndent()
            ),
            instructionLibrary = InstructionLibrary(
                visualDescriptions = mutableListOf(
                    "皮肤泛起的潮红",
                    "身体线条的起伏",
                    "汗珠滑落的轨迹",
                    "表情变化的所有细节",
                    "身体反应的每一个瞬间"
                ),
                tactileDescriptions = mutableListOf(
                    "每一寸肌肤的感觉",
                    "触碰的所有细节",
                    "温度传递的全程",
                    "压力和力度的变化"
                ),
                psychologicalDescriptions = mutableListOf(
                    "心理活动的全部过程",
                    "欲望的层层递进",
                    "本能与理智的对抗"
                ),
                auditoryDescriptions = mutableListOf(
                    "所有的声音细节",
                    "呼吸的每一次变化",
                    "环境的所有声响"
                ),
                olfactoryDescriptions = mutableListOf(
                    "所有的气味细节"
                ),
                tasteDescriptions = mutableListOf(
                    "所有的味觉体验"
                )
            )
        )
    }
    
    // ========== 场景检测关键词 ==========
    
    /**
     * 场景类型检测
     */
    fun detectSceneType(content: String): SceneType {
        val lowerContent = content.lowercase()
        
        return when {
            containsAny(lowerContent, listOf("亲吻", "拥抱", "缠绵", "亲密", "做", "结合")) -> SceneType.INTIMATE
            containsAny(lowerContent, listOf("高潮", "爆发", "临界", "顶峰")) -> SceneType.CLIMAX
            containsAny(lowerContent, listOf("抚摸", "触碰", "接触", "肌肤", "身体")) -> SceneType.PHYSICAL
            containsAny(lowerContent, listOf("心动", "暧昧", "情愫", "喜欢", "爱意")) -> SceneType.EMOTIONAL
            containsAny(lowerContent, listOf("温柔", "依靠", "依偎", "靠近")) -> SceneType.AMBIGUOUS
            containsAny(lowerContent, listOf("争吵", "冲突", "矛盾", "对峙")) -> SceneType.CONFLICT
            containsAny(lowerContent, listOf("和解", "解决", "释然", "放下")) -> SceneType.RESOLUTION
            containsAny(lowerContent, listOf("紧张", "不安", "恐惧", "压力")) -> SceneType.TENSION
            containsAny(lowerContent, listOf("余韵", "回味", "后", "温柔")) -> SceneType.AFTERMATH
            else -> SceneType.DAILY
        }
    }
    
    private fun containsAny(content: String, keywords: List<String>): Boolean {
        return keywords.any { content.contains(it) }
    }
    
    // ========== 描写指令生成 ==========
    
    /**
     * 根据感官类型和强度生成描写指令
     */
    fun generateSensoryInstructions(
        sensoryType: SensoryType,
        intensity: Int,
        tabooLevel: TabooLevel
    ): String {
        val intensityModifier = when {
            intensity <= 3 -> "轻微"
            intensity <= 6 -> "适中"
            intensity <= 8 -> "强烈"
            else -> "极致"
        }
        
        val instruction = when (sensoryType) {
            SensoryType.VISUAL -> "${intensityModifier}的视觉描写，注重光影和细节"
            SensoryType.TACTILE -> "${intensityModifier}的触觉描写，注重温度和质感"
            SensoryType.AUDITORY -> "${intensityModifier}的听觉描写，注重节奏和层次"
            SensoryType.OLFACTORY -> "${intensityModifier}的嗅觉描写，注重氛围"
            SensoryType.TASTE -> "${intensityModifier}的味觉描写，注重细节"
            SensoryType.PROPRIOCEPTION -> "${intensityModifier}的本体感觉描写，注重内在感受"
        }
        
        // 根据禁忌等级调整
        return when (tabooLevel) {
            TabooLevel.LIGHT -> instruction.replace(intensityModifier, "轻微")
            TabooLevel.MODERATE -> instruction
            TabooLevel.DEEP -> instruction.replace(intensityModifier, "细腻丰富的")
            TabooLevel.UNRESTRICTED -> instruction.replace(intensityModifier, "极致细腻全面的")
        }
    }
    
    /**
     * 生成完整场景Prompt
     */
    fun generateScenePrompt(
        sceneType: SceneType,
        profile: SensoryProfile,
        characterInfo: CharacterBodyProfile? = null,
        relationshipInfo: RelationshipState? = null
    ): String {
        val template = getTemplate(profile.tabooLevel)
        val scenePrompt = template.scenePrompts[sceneType] ?: template.scenePrompts[SceneType.DAILY] ?: ""
        
        val instructions = buildString {
            appendLine("【场景类型】${sceneType.name}")
            appendLine()
            
            // 描写密度
            appendLine("【描写密度】${profile.descriptionDensity}/10")
            appendLine()
            
            // 感官侧重
            appendLine("【感官侧重】")
            profile.sensoryFocus.forEach { sensory ->
                appendLine("- ${generateSensoryInstructions(sensory, profile.descriptionDensity, profile.tabooLevel)}")
            }
            appendLine()
            
            // 角色信息
            characterInfo?.let { char ->
                appendLine("【角色特征】")
                if (char.bodyFeatures.height.isNotBlank()) {
                    appendLine("- 身高：${char.bodyFeatures.height}")
                }
                if (char.bodyFeatures.build.isNotBlank()) {
                    appendLine("- 体型：${char.bodyFeatures.build}")
                }
                if (char.bodyFeatures.skinTone.isNotBlank()) {
                    appendLine("- 肤色：${char.bodyFeatures.skinTone}")
                }
                if (char.bodyFeatures.voiceDescription.isNotBlank()) {
                    appendLine("- 声音：${char.bodyFeatures.voiceDescription}")
                }
                if (char.bodyFeatures.scent.isNotBlank()) {
                    appendLine("- 体味：${char.bodyFeatures.scent}")
                }
                
                // 敏感点
                if (char.sensitivePoints.isNotEmpty()) {
                    appendLine("【敏感点】")
                    char.sensitivePoints.take(3).forEach { point ->
                        appendLine("- ${point.name}（${point.location}）：${point.responseDescription}")
                    }
                }
                appendLine()
            }
            
            // 关系信息
            relationshipInfo?.let { rel ->
                appendLine("【关系状态】")
                appendLine("- 亲密度：${rel.intimacyLevel}%")
                appendLine("- 信任度：${rel.trustLevel}%")
                appendLine("- 当前阶段：${rel.currentStage.displayName}")
                appendLine("- 权力动态：${rel.powerDynamic.displayName}")
                appendLine()
            }
            
            // 场景Prompt
            appendLine("【场景要求】")
            appendLine(scenePrompt)
        }
        
        return instructions
    }
    
    // ========== 动态Prompt构建 ==========
    
    /**
     * 构建增强版System Prompt
     */
    fun buildEnhancedSystemPrompt(
        novel: Novel,
        chapterNum: Int,
        progressInfo: ProgressInfo
    ): String {
        val template = getTemplate(novel.sensoryProfile.tabooLevel)
        val sceneType = SceneType.EMOTIONAL
        
        return buildString {
            // 基础模板
            appendLine(template.basePrompt)
            appendLine()
            
            // 小说信息
            appendLine("【小说信息】")
            appendLine("- 标题：《${novel.title}》")
            appendLine("- 类型：${novel.type}")
            appendLine("- 风格：${novel.style}")
            appendLine("- 视角：${novel.generationConfig.perspectiveMode.displayName}")
            appendLine()
            
            // 进度信息
            appendLine("【当前进度】")
            appendLine(progressInfo.toModelHint())
            appendLine(progressInfo.toSensoryHint())
            appendLine()
            
            // 节奏偏好
            appendLine("【节奏偏好】")
            appendLine("- ${novel.generationConfig.rhythmPreference.description}")
            appendLine("- 描写密度：${novel.sensoryProfile.descriptionDensity}/10")
            appendLine("- 情感表达：${novel.sensoryProfile.emotionExpressionStyle}")
            appendLine()
            
            // 当前场景要求
            appendLine("【章节要求】")
            appendLine(generateScenePrompt(
                sceneType = sceneType,
                profile = novel.sensoryProfile,
                relationshipInfo = novel.relationshipStates.lastOrNull()
            ))
            appendLine()
            
            // 去AI味写作提示
            appendLine(getAntiAIWritingHints())
        }
    }
    
    /**
     * 获取去AI味写作提示
     */
    fun getAntiAIWritingHints(): String {
        return """
【写作要求（严格遵守）】
1. 段落长度必须有变化：每章至少一个长段落（8句以上），多个极短段落（1-2句）
2. 禁止句式重复：同一段落内相同句式结构不超过2次
3. 禁止心理总结：用具体动作、细节代替
   - 错误：「他很愤怒」
   - 正确：「把烟头摁进掌心，烟灰簌簌落下」
4. 对话要自然：夹杂语气词、打断、省略，不要工整的一问一答
5. 场景描写要粗糙：不要面面俱到，留白给读者想象
6. 感官描写要真实：避免过度美化，保持质感
        """.trimIndent()
    }
    
    // ========== 关系动态Prompt ==========
    
    /**
     * 根据关系状态生成Prompt
     */
    fun generateRelationshipPrompt(
        relationship: RelationshipState,
        triggerEvent: RelationshipEventType
    ): String {
        return buildString {
            appendLine("【关系事件】${triggerEvent.name}")
            appendLine("- 事件描述：${triggerEvent.name}")
            appendLine("- 亲密度变化：${if (triggerEvent.intimacyImpact >= 0) "+" else ""}${triggerEvent.intimacyImpact}")
            appendLine("- 信任度变化：${if (triggerEvent.trustImpact >= 0) "+" else ""}${triggerEvent.trustImpact}")
            appendLine()
            
            // 根据关系阶段调整描写
            val stageModifier = when (relationship.currentStage) {
                RelationshipStage.INITIAL, RelationshipStage.ACQUAINTANCE -> 
                    "刚刚认识，描写应该更加克制和含蓄"
                RelationshipStage.FAMILIAR, RelationshipStage.CLOSE -> 
                    "已经熟悉，可以有一些亲密互动"
                RelationshipStage.DEEP, RelationshipStage.INTIMATE -> 
                    "关系深入，可以有较多亲密描写"
                RelationshipStage.BONDED -> 
                    "关系稳定，可以有深度的亲密描写"
            }
            appendLine("【描写指导】$stageModifier")
            appendLine()
            
            // 权力动态
            val powerModifier = when (relationship.powerDynamic) {
                PowerDynamic.DOMINANT -> "一方处于主导地位，描写侧重于主导方的主动"
                PowerDynamic.SUBMISSIVE -> "一方处于顺从地位，描写侧重于顺从方的反应"
                PowerDynamic.EQUAL -> "双方平等，描写侧重于互动和交流"
                PowerDynamic.FLUID -> "权力关系流动，描写侧重于角色的转换"
            }
            appendLine("【权力动态】$powerModifier")
        }
    }
}
