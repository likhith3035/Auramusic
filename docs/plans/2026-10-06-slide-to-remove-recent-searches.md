# Slide Right to Remove Recent Searches Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Allow users in the Search screen to slide/swipe right on any recent search item to delete/remove it from search history, while preserving the existing 'X' remove button.

**Architecture:** 
- Leverage Compose Material 3's `SwipeToDismissBox` and `rememberSwipeToDismissBoxState` configured with `enableDismissFromStartToEnd = true` and `enableDismissFromEndToStart = false`.
- Underneath the sliding row, render a red/error-tinted reveal container with an animated delete icon (`Icons.Rounded.Delete`) and haptic feedback on completion.
- Keep the existing 'X' button (`Icons.Rounded.Close`) intact on the right of `RecentSearchEntityRow` so users have both swipe and tap-to-delete options.
- Animate list changes with `Modifier.animateItem()` so remaining items transition smoothly upon deletion.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.10.x, Compose Material 3 (`SwipeToDismissBox`).

---

### Task 1: Add Swipe-to-Dismiss Background & Imports in `SearchScreen.kt`

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/SearchScreen.kt`

**Details:**
1. Import `SwipeToDismissBox`, `SwipeToDismissBoxState`, `SwipeToDismissBoxValue`, `rememberSwipeToDismissBoxState`, `ExperimentalMaterial3Api`.
2. Import `Icons.Rounded.Delete` from `androidx.compose.material.icons.rounded.Delete`.
3. Import `rememberUpdatedState` from `androidx.compose.runtime`.
4. Import `drawWithContent` from `androidx.compose.ui.draw` and `clipRect` from `androidx.compose.ui.graphics.drawscope`.
5. Implement `RecentSearchSwipeBackground(swipeState: SwipeToDismissBoxState)`:
   - Reveals an error container background (`MaterialTheme.colorScheme.error.copy(alpha = 0.22f)` or `MaterialTheme.colorScheme.errorContainer`).
   - Clips the rendered background to the swipe offset (`clipRect(left = 0f, top = 0f, right = offset, bottom = size.height)`).
   - Displays `Icons.Rounded.Delete` with `Alignment.CenterStart` and `padding(start = PAGE_GUTTER)`.

---

### Task 2: Implement `DismissibleRecentSearchRow` and Update `RecentSearchEntityRow`

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/SearchScreen.kt:750-830`

**Details:**
1. Create `@OptIn(ExperimentalMaterial3Api::class) @Composable private fun DismissibleRecentSearchRow`:
   - State: `rememberSwipeToDismissBoxState` with `confirmValueChange`:
     - If `value == SwipeToDismissBoxValue.StartToEnd`: trigger `haptics.play(Haptic.Select)`, execute `onRemove()`, return `true`.
     - Otherwise return `false`.
   - Positional threshold: 35% of row width (`positionalThreshold = { distance -> distance * 0.35f }`).
   - `SwipeToDismissBox`:
     - `enableDismissFromStartToEnd = true` (slide right to delete)
     - `enableDismissFromEndToStart = false` (disable slide left)
     - `backgroundContent = { RecentSearchSwipeBackground(dismissState) }`
2. Update `RecentSearchEntityRow`:
   - Add `modifier: Modifier = Modifier` parameter.
   - Apply `modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)` so the row is opaque during drag.
   - **PRESERVE THE 'X' BUTTON**: The right-aligned `Box` with `Icon(Icons.Rounded.Close, ...)` and `.clickable(onClick = onRemove)` MUST remain untouched.

---

### Task 3: Hook `DismissibleRecentSearchRow` into `recentSearches` LazyList

**Files:**
- Modify: `sharedUi/src/jvmSharedMain/kotlin/com/music/bitchord/ui/screens/SearchScreen.kt:750-760`

**Details:**
1. In `LazyListScope.recentSearches`:
   - Replace the direct `RecentSearchEntityRow` call with `DismissibleRecentSearchRow`.
   - Pass `modifier = Modifier.animateItem()` to enable smooth collapse animations.
   - Ensure the item key remains `key = { "recent:${it.id}" }` for state retention and stable item animations.

---

### Task 4: Verify Compilation on Both Android and Desktop

**Commands:**
1. Android Build:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat compileDevDebugSources
   ```
   *Expected: BUILD SUCCESSFUL*

2. Desktop Build:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat :desktopApp:compileKotlin
   ```
   *Expected: BUILD SUCCESSFUL*
