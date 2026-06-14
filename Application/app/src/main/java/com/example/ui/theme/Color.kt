package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

val Slate950 = Color(0xFF020617)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

val Blue500 = Color(0xFF3B82F6) // Bright blue accent
val Blue600 = Color(0xFF2563EB)
val Blue100 = Color(0xFFDBEAFE)
val Blue400 = Color(0xFF60A5FA)
val Blue700 = Color(0xFF1D4ED8)
val Blue900 = Color(0xFF1E3A8A)
val Emerald500 = Color(0xFF10B981)
val Slate600 = Color(0xFF475569)

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// Status & Priority Colors
val StatusPending = Color(0xFFF59E0B) // Yellow/Amber
val StatusInProgress = Color(0xFF3B82F6) // Blue
val StatusCompleted = Color(0xFF10B981) // Green
val StatusPendingContainer = Color(0x1AF59E0B) // 10% opacity
val StatusInProgressContainer = Color(0x1A3B82F6)
val StatusCompletedContainer = Color(0x1A10B981)

val PriorityLow = Color(0xFF9CA3AF) // Gray
val PriorityMedium = Color(0xFFF97316) // Orange
val PriorityHigh = Color(0xFFEF4444) // Red
val PriorityLowContainer = Color(0x1A9CA3AF)
val PriorityMediumContainer = Color(0x1AF97316)
val PriorityHighContainer = Color(0x1AEF4444)

// Card styling colors
val CardBackgroundUser: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

val CardBorderUser: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

val StatCardBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

val StatCardBorder: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

// Aliases used by new screens
val AccentBlue = Blue500
val AppBackground = Slate900
val AppSurface = Slate800
