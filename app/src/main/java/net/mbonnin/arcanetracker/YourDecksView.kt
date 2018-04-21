package net.mbonnin.arcanetracker

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics


class YourDecksView(context: Context, val onDeckClicked: (Deck) -> Unit): RecyclerView(context) {
    val yourDecksAdapter = YourDecksAdapter()

    init {
        layoutManager = GridLayoutManager(context, 3)
        yourDecksAdapter.setOnClickListener{deckClickedId ->
            val deck = yourDecksAdapter.list.filter { it.id == deckClickedId }.firstOrNull()
            if (deck != null) {
                onDeckClicked(deck)
            }
        }
        adapter = yourDecksAdapter

        val spacing = 8.toPixel(context.resources.displayMetrics)

        setPadding(spacing, spacing, spacing, spacing)
        clipToPadding = false

        addItemDecoration(SpacesItemDecoration(spacing))
    }
}

fun Int.toPixel(displayMetrics: DisplayMetrics): Int {
    return (this.toFloat() * displayMetrics.density).toInt()
}