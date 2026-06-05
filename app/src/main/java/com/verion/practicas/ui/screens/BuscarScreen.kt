package com.verion.practicas.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verion.practicas.R
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*

private val categories = listOf("Todos", "Informática", "Diseño", "Mecánica", "Electricidad", "Admin.")
private val mockTecnicos = listOf(
    Triple("C", "Carlos M.",    "Técnico en Informática · ★ 5.0 · Lima"),
    Triple("A", "Ana R.",       "Diseñadora Gráfica · ★ 4.2 · Arequipa"),
    Triple("L", "Luis P.",      "Técnico Electricista · ★ 4.8 · Cusco"),
    Triple("M", "María T.",     "Administración · ★ 4.5 · Lima"),
)

@Composable
fun BuscarScreen() {
    var search by remember { mutableStateOf("") }
    var selectedChip by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Explorar técnicos", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Encuentra el talento que necesitas", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
        }

        item {
            GlassCard(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_search),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = search,
                        onValueChange = { search = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (search.isEmpty()) Text("Buscar por habilidad...", color = Color.White.copy(alpha = 0.38f), fontSize = 14.sp)
                            inner()
                        }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(categories) { i, cat ->
                    val selected = i == selectedChip
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (selected)
                                    Brush.linearGradient(listOf(BrandBlue, BrandIndigo))
                                else
                                    Brush.linearGradient(listOf(GlassBg, GlassBg))
                            )
                            .border(
                                1.dp,
                                if (selected) Color.Transparent else GlassBorder,
                                RoundedCornerShape(50.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { selectedChip = i }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cat,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Text(
                "Técnicos destacados",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(12.dp))
        }

        itemsIndexed(mockTecnicos) { _, (initial, name, detail) ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(OrbPurple, Color(0xFF9B6EFF)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name,   color = TextPrimary,   fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(detail, color = BrandBlue,     fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
