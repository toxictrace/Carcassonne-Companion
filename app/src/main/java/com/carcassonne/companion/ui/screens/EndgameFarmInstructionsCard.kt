package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.CarcBg3
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.CarcText3

@Composable
fun EndgameFarmInstructionsCard() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(CarcBg3).padding(14.dp)) {
        Text(stringResource(R.string.farm_scoring), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.farm_instructions),
            fontSize = 13.sp, color = CarcText3)
        Text(stringResource(R.string.farm_formula), fontSize = 13.sp, color = CarcGreen,
            fontWeight = FontWeight.SemiBold)
    }
}
