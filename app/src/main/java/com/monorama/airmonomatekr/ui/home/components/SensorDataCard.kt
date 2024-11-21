package com.monorama.airmonomatekr.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.monorama.airmonomatekr.ui.theme.BlueLevel
import com.monorama.airmonomatekr.ui.theme.GreenLevel
import com.monorama.airmonomatekr.ui.theme.OrangeLevel
import com.monorama.airmonomatekr.ui.theme.RedLevel

@Composable
fun SensorDataCard(
    title: String,
    value: String,
    unit: String,
    level: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Level Indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (level) {
                            0 -> BlueLevel
                            1 -> GreenLevel
                            2 -> OrangeLevel
                            3 -> RedLevel
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    color = when (level) {
                        0 -> BlueLevel
                        1 -> GreenLevel
                        2 -> OrangeLevel
                        3 -> RedLevel
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
} 