package com.shrey.prankspot.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shrey.prankspot.ui.AppViewModel

@Composable
fun LibraryScreen(vm: AppViewModel, onBack: () -> Unit, onPreview: (String) -> Unit, onChosen: () -> Unit) {
    val favs by vm.favoritePranks.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }

    val shown = vm.pranks.filter { p ->
        (filter == "all" || (filter == "fav" && p.id in favs) || p.categoryId == filter) &&
            (query.isBlank() || listOf(p.name, p.title, p.message).any { it.contains(query, ignoreCase = true) })
    }

    Screen(
        "Prank pages", onBack,
        actions = {
            IconButton(onClick = { vm.choosePrank(vm.pranks.random()); onChosen() }) { Icon(Icons.Default.Casino, "Pick a random page") }
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search pranks") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(filter == "all", { filter = "all" }, { Text("All") }) }
                    item { FilterChip(filter == "fav", { filter = "fav" }, { Text("⭐ Favorites") }) }
                    items(vm.categories, key = { it.id }) { c ->
                        FilterChip(filter == c.id, { filter = c.id }, { Text("${c.emoji} ${c.name}") })
                    }
                }
            }
            if (shown.isEmpty()) item { Text("No pranks match. Try another search.", Modifier.padding(16.dp)) }
            items(shown, key = { it.id }) { p ->
                val selected = !draft.randomPrank && draft.prankId == p.id
                val bg = runCatching { Color(android.graphics.Color.parseColor(p.bgStart)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                val fg = runCatching { Color(android.graphics.Color.parseColor(p.textColor)) }.getOrDefault(Color.White)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { vm.choosePrank(p); onChosen() },
                    colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
                    border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.secondary) else null,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p.emoji, fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name + if (selected) "  ✓" else "", fontWeight = FontWeight.Bold)
                                Text(vm.category(p.categoryId).name, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { vm.toggleFavoritePrank(p.id) }) {
                                Icon(if (p.id in favs) Icons.Default.Star else Icons.Outlined.StarOutline, "Favorite", tint = fg)
                            }
                        }
                        Text(p.title, fontWeight = FontWeight.SemiBold)
                        Text(p.message, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { onPreview(p.id) }) { Text("Preview", color = fg) }
                            TextButton(onClick = { vm.choosePrank(p); onChosen() }) { Text("Use this", color = fg, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
