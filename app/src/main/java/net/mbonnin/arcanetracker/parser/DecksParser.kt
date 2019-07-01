package net.mbonnin.arcanetracker.parser

import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import net.hearthsim.kotlin.hslog.LogLine
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.helper.DeckStringHelper
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RDeck
import net.mbonnin.hsmodel.CardJson
import timber.log.Timber

class DecksParser: LogReader.LineConsumer {
    enum class State {
        DEFAULT,
        ARENA,
        GAME
    }

    val deckStringHelper = DeckStringHelper()
    var state = State.DEFAULT


    override fun onLine(rawLine: String) {
        Timber.d(rawLine)
        if (rawLine.contains("Deck Contents Received:")) {
            state = State.DEFAULT
        } else if (rawLine.contains("Finished Editing Deck:")) {
            state = State.DEFAULT
        } else if (rawLine.contains("Finding Game With Deck:")) {
            state = State.GAME
        } else if (rawLine.contains("Starting Arena Game With Deck")) {
            state = State.ARENA
        } else {
            val logLine = LogLine.parseLine(rawLine)
            if (logLine != null) {
                val result = deckStringHelper.parseLine(logLine.line)

                if (result?.id != null) {
                    val deck = DeckStringHelper.parse(result.deckString)
                    if (deck != null) {
                        deck.id = result.id
                        if (state == State.ARENA) {
                            deck.name = ArcaneTrackerApplication.get().getString(R.string.arenaDeck)
                        } else {
                            deck.name = result.name ?: "?"
                        }


                        val nonCollectible = deck.cards.keys.firstOrNull { CardJson.getCard(it)?.collectible != true }
                        if (nonCollectible != null) {
                            Utils.reportNonFatal(Exception("InvalidDeck ? ($nonCollectible) (${logLine.line})"))
                        }

                        if (state == State.ARENA || state == State.GAME) {
                            Completable.fromAction {
                                MainViewCompanion.playerCompanion.deck = deck
                            }
                                    .subscribeOn(AndroidSchedulers.mainThread())
                                    .subscribe()
                        }

                        val rdeck = RDeck(id = deck.id,
                                name = deck.name,
                                deck_string = result.deckString,
                                arena = state == State.ARENA)

                        try {
                            RDatabaseSingleton.instance.deckDao().insert(rdeck)
                        } catch (e: Exception) {
                            RDatabaseSingleton.instance.deckDao().updateNameAndContents(rdeck.id, rdeck.name, rdeck.deck_string, rdeck.accessMillis)
                        }
                    }
                }

            }
        }
    }

    override fun onPreviousDataRead() {
    }

    companion object {
        var instance: DecksParser? = null
        fun get(): DecksParser {
            if (instance == null) {
                instance = DecksParser()
            }

            return instance!!
        }
    }
}