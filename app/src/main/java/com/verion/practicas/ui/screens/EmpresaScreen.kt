package com.verion.practicas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verion.practicas.R
import com.verion.practicas.ui.components.GlassCard
import com.verion.practicas.ui.theme.*

private data class CompanyItem(val initial: String, val name: String, val detail: String, val convoc: String)
private val mockEmpresas = listOf(
    CompanyItem("T", "TechPyme SAC",  "Empresa de software · Lima",  "5 convoc."),
    CompanyItem("I", "InnoMed EIRL",  "Clínica y salud · Arequipa",  "3 convoc."),
    CompanyItem("A", "AgriTech Perú", "Tecnología agrícola · Cusco", "2 convoc."),
)

@Composable
fun EmpresaScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Empresas & MiPymes", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Empresas que buscan talento técnico", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
        }

        items(mockEmpresas.size) { i ->
            val co = mockEmpresas[i]
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(BrandBlue, BrandPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(co.initial, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(co.name,   color = TextPrimary,   fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(co.detail, color = TextSecondary, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(50.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(co.convoc, color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Spacer(Modifier.height(16.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "CONVOCATORIAS ACTIVAS",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Próximamente",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Publica y gestiona convocatorias\npara encontrar técnicos calificados",
                        color = Color.White.copy(alpha = 0.22f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
