package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.CarcBorder
import com.carcassonne.companion.ui.theme.CarcCard2
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.CarcText3

@Composable
fun MatchHeaderSection(
    title: String,
    date: String,
    onShare: () -> Unit,
    onEdit: () -> Unit
) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "📅 $date",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 12.sp,
                    color = CarcText3,
                    maxLines = 1,
                    softWrap = false
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarcCard2)
                            .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                            .clickable(onClick = onShare)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.share_game),
                            fontSize = 13.sp,
                            color = CarcGreen,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarcCard2)
                            .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                            .clickable { onEdit() }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.edit_btn),
                            fontSize = 13.sp,
                            color = CarcGreen,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

}
