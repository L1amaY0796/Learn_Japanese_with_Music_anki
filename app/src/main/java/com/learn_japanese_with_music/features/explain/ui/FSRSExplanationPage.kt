package com.learn_japanese_with_music.features.explain.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learn_japanese_with_music.core.components.MenuButton

@Composable
fun FSRSExplanationPage(
    onMenuClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BackHandler() {
            onMenuClick()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuButton(onClick = onMenuClick)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "複習演算法說明",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ExplanationSection(
                    title = "核心理論：間隔重複 (SRS)",
                    content = "根據艾賓浩斯遺忘曲線，人類在學習後會迅速遺忘資訊。間隔重複技術透過在「快要忘記」的關鍵時刻進行複習，將短期記憶轉化為長期記憶，能顯著提升記憶效率。"
                )

                ExplanationSection(
                    title = "FSRS-6 演算法",
                    content = "本系統採用先進的 FSRS (Free Spaced Repetition Scheduler) 演算法。與傳統演算法不同，FSRS 基於機器學習與大數據擬合，能更精準地預測您的大腦記憶狀態。"
                )

                ExplanationSection(
                    title = "DSR 三元模型",
                    content = "• 熟悉度 (Retrievability): \n您當下能想起該單字的機率。\n• 穩定度 (Stability): \n記憶能維持多久。穩定度越高，複習間隔越長。\n• 難度 (Difficulty): \n單字對您而言的複雜程度。"
                )

                ExplanationSection(
                    title = "四種評分與回饋",
                    content = "• Again (忘記): \n重置記憶，進入重新學習步幅。\n• Hard (困難): \n熟悉度不足，系統會稍微縮短下次複習間隔。\n• Good (良好): 標準間隔成長。\n• Easy (簡單): \n秒答，系統會大幅推遲下次複習日期。"
                )

                ExplanationSection(
                    title = "頑固單字 (Leech) 隔離",
                    content = "若一個單字被遺忘超過 6 次，系統會將其判定為「頑固單字」並自動停用 (Suspend)。這能防止您陷入無效的死記硬背，建議您為該單字建立新的聯想或例句後再解除停用。"
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun ExplanationSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 33.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
