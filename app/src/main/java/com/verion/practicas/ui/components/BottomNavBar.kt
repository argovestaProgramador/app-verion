package com.verion.practicas.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Task
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.verion.practicas.ui.theme.BrandBlue
import com.verion.practicas.ui.theme.NavBg
import com.verion.practicas.ui.theme.NavBorder
import com.verion.practicas.ui.theme.PrimaryContainer
import com.verion.practicas.ui.theme.TextSecondary

private data class NavEntry(val icon: ImageVector, val label: String)

private fun navItemsFor(rol: String) = when (rol) {
    "TECNICO" -> listOf(
        NavEntry(Icons.Rounded.Search,          "Buscar"),
        NavEntry(Icons.Rounded.Task,            "Postulaciones"),
        NavEntry(Icons.Rounded.Person,          "Perfil"),
    )
    "EMPRESA" -> listOf(
        NavEntry(Icons.Rounded.Search,          "Buscar"),
        NavEntry(Icons.Rounded.BusinessCenter,  "Convocatorias"),
        NavEntry(Icons.Rounded.Person,          "Perfil"),
    )
    else -> listOf(
        NavEntry(Icons.Rounded.Search,  "Buscar"),
        NavEntry(Icons.Rounded.Person,  "Perfil"),
    )
}

@Composable
fun BottomNavBar(
    selectedIndex: Int,
    onNavSelected: (Int) -> Unit,
    rol: String = "GUEST",
    modifier: Modifier = Modifier
) {
    val navItems   = remember(rol) { navItemsFor(rol) }
    val borderColor = NavBorder

    NavigationBar(
        modifier       = modifier.drawBehind {
            drawLine(
                color       = borderColor,
                start       = Offset(0f, 0f),
                end         = Offset(size.width, 0f),
                strokeWidth = 1f
            )
        },
        containerColor = NavBg,
        tonalElevation = 0.dp,
        windowInsets   = WindowInsets.navigationBars,
    ) {
        navItems.forEachIndexed { i, item ->
            NavigationBarItem(
                selected        = i == selectedIndex,
                onClick         = { onNavSelected(i) },
                icon            = {
                    Icon(imageVector = item.icon, contentDescription = item.label)
                },
                label           = {
                    Text(item.label, style = MaterialTheme.typography.labelSmall)
                },
                alwaysShowLabel = false,
                colors          = NavigationBarItemDefaults.colors(
                    selectedIconColor   = BrandBlue,
                    selectedTextColor   = BrandBlue,
                    indicatorColor      = PrimaryContainer,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                )
            )
        }
    }
}
