package com.monorama.airmonomatekr.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.monorama.airmonomatekr.ui.home.components.SensorDataCard

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "실시간 공기질 데이터",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        SensorDataCard(
            title = "미세먼지 (PM2.5)",
            value = "25",
            unit = "μg/m³"
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SensorDataCard(
            title = "이산화탄소 (CO2)",
            value = "450",
            unit = "ppm"
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SensorDataCard(
            title = "온도",
            value = "24.5",
            unit = "°C"
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SensorDataCard(
            title = "습도",
            value = "45",
            unit = "%"
        )
    }
} 