package chat.rocket.android.widget.autocompletion.ui

import android.support.v7.widget.RecyclerView
import chat.rocket.android.widget.autocompletion.model.SuggestionModel
import chat.rocket.android.widget.autocompletion.strategy.CompletionStrategy
import chat.rocket.android.widget.autocompletion.strategy.regex.StringMatchingCompletionStrategy
import java.lang.reflect.Type
import kotlin.properties.Delegates

abstract class SuggestionsAdapter<VH : BaseSuggestionViewHolder>(
        val token: String,
        val constraint: Int = CONSTRAINT_UNBOUND,
        threshold: Int  = MAX_RESULT_COUNT) : RecyclerView.Adapter<VH>() {
    companion object {
        // Any number of results.
        const val UNLIMITED_RESULT_COUNT = -1
        // Trigger suggestions only if on the line start.
        const val CONSTRAINT_BOUND_TO_START = 0
        // Trigger suggestions from anywhere.
        const val CONSTRAINT_UNBOUND = 1
        // Maximum number of results to display by default.
        private const val MAX_RESULT_COUNT = 5
    }
    private var itemType: Type? = null
    private var itemClickListener: ItemClickListener? = null
    // Called to gather results when no results have previously matched.
    private var providerExternal: ((query: String) -> Unit)? = null
    // Maximum number of results/suggestions to display.
    private var resultsThreshold: Int = if (threshold > 0) threshold else UNLIMITED_RESULT_COUNT
    // The strategy used for suggesting completions.
    private val strategy: CompletionStrategy = StringMatchingCompletionStrategy(resultsThreshold)
    // Current input term to look up for suggestions.
    private var currentTerm: String by Delegates.observable("", { _, _, newTerm ->
        val items = strategy.autocompleteItems(newTerm)
        notifyDataSetChanged()
    })

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).text.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), itemClickListener)
    }

    override fun getItemCount() = strategy.autocompleteItems(currentTerm).size

    private fun getItem(position: Int): SuggestionModel {
        return strategy.autocompleteItems(currentTerm)[position]
    }

    fun autocomplete(newTerm: String) {
        this.currentTerm = newTerm.toLowerCase().trim()
    }

    fun addItems(list: List<SuggestionModel>) {
        strategy.addAll(list)
        // Since we've just added new items we should check for possible new completion suggestions.
        strategy.autocompleteItems(currentTerm)
        notifyDataSetChanged()
    }

    fun setOnClickListener(clickListener: ItemClickListener) {
        this.itemClickListener = clickListener
    }

    fun hasItemClickListener() = itemClickListener != null

    /**
     * Return the current searched term.
     */
    fun term() = this.currentTerm

    /**
     * Set the maximum number of results to show.
     *
     * @param threshold The maximum number of suggestions to display.
     */
    fun setResultsThreshold(threshold: Int) {
        check(threshold > 0)
        resultsThreshold = threshold
    }

    fun cancel() {
        strategy.addAll(emptyList())
        strategy.autocompleteItems(currentTerm)
        notifyDataSetChanged()
    }

    interface ItemClickListener {
        fun onClick(item: SuggestionModel)
    }
}