package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.ui.theme.CarcBorder
import com.carcassonne.companion.ui.theme.CarcCard
import com.carcassonne.companion.ui.theme.CarcText
import com.carcassonne.companion.ui.theme.CarcText3

@Composable
fun MatchNotesSection(notes: String?) {
    if (!notes.isNullOrBlank()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard),
            border = BorderStroke(1.dp, CarcBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("📝", fontSize = 13.sp, color = CarcText3)
                Spacer(Modifier.height(4.dp))
                Text(notes, fontSize = 14.sp, color = CarcText, lineHeight = 20.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
