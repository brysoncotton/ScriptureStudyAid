package com.example.scripturestudyaid

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

class VerseAdapter(
    private var verses: List<Verse>,
    private var volumeName: String = "",
    private var bookName: String = "",
    private var chapterNum: Int = 0
) : RecyclerView.Adapter<VerseAdapter.ViewHolder>() {

    private var allVerses: List<Verse> = verses
    private var filteredVerses: List<Verse> = verses

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardVerse: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardVerse)
        val tvVerseText: TextView = view.findViewById(R.id.tvVerseText)
        val flNoteContainer: FrameLayout = view.findViewById(R.id.flNoteContainer)
        var actionPopup: PopupWindow? = null // Track popup to prevent orphaned instances
    }

    fun filterVerses(query: String) {
        filteredVerses = if (query.isEmpty()) {
            allVerses
        } else {
            allVerses.filter { verse ->
                verse.text.contains(query, ignoreCase = true) ||
                verse.verse.toString().contains(query)
            }
        }
        notifyDataSetChanged()
    }

    private var selectionMode: Boolean = false
    private var selectedVerseRef: Int? = null
    private var onVerseSelected: ((Verse) -> Unit)? = null

    fun setSelectionMode(enabled: Boolean, onSelected: (Verse) -> Unit) {
        this.selectionMode = enabled
        this.onVerseSelected = onSelected
        notifyDataSetChanged()
    }

    fun setSelectedVerse(verseNum: Int?) {
        this.selectedVerseRef = verseNum
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = filteredVerses.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val verse = filteredVerses[position]
        val context = holder.itemView.context

        if (selectionMode) {
            // --- SELECTION MODE ---
            
            // 1. Bubble Highlight (Stroke Change on Card)
            if (verse.verse == selectedVerseRef) {
                holder.cardVerse.strokeColor = Color.parseColor("#00BCD4") // Cyan
                holder.cardVerse.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
                holder.cardVerse.setCardBackgroundColor(Color.parseColor("#E0F7FA")) // Light Cyan tint
            } else {
                holder.cardVerse.strokeColor = Color.parseColor("#E0E0E0")
                holder.cardVerse.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
                holder.cardVerse.setCardBackgroundColor(Color.WHITE)
            }

            // 2. Shared Click Logic
            val onVerseClick = View.OnClickListener {
                if (selectedVerseRef == verse.verse) {
                    selectedVerseRef = null
                    onVerseSelected?.invoke(verse.copy(verse = -1))
                } else {
                    selectedVerseRef = verse.verse
                    onVerseSelected?.invoke(verse)
                }
                notifyDataSetChanged()
            }

            // Apply to EVERYTHING
            holder.cardVerse.setOnClickListener(onVerseClick)
            holder.itemView.setOnClickListener(onVerseClick)
            holder.tvVerseText.setOnClickListener(onVerseClick)
            holder.flNoteContainer.setOnClickListener(onVerseClick) // Even the empty container


            // 3. Simple Text Display (No Spans, No Selectable)
            val fullString = "${verse.verse} ${verse.text}"
            val spannable = SpannableString(fullString)
            val endOfNum = verse.verse.toString().length
            spannable.setSpan(SuperscriptSpan(), 0, endOfNum, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(RelativeSizeSpan(0.7f), 0, endOfNum, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            
            holder.tvVerseText.text = spannable
            holder.tvVerseText.movementMethod = null // Disable clickable spans
            holder.tvVerseText.setTextIsSelectable(false) // Disable text selection
            holder.tvVerseText.setOnLongClickListener(null) // Disable long press menu
            holder.tvVerseText.isClickable = false
            holder.tvVerseText.isFocusable = false
            
            // 4. Hide Icons
            holder.flNoteContainer.removeAllViews()
            holder.flNoteContainer.visibility = View.GONE

        } else {
            // --- NORMAL MODE ---
            holder.cardVerse.strokeColor = Color.parseColor("#E0E0E0")
            holder.cardVerse.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
            holder.cardVerse.setCardBackgroundColor(Color.WHITE)
            holder.cardVerse.setOnClickListener(null)
            holder.flNoteContainer.visibility = View.VISIBLE

            val fullString = "${verse.verse} ${verse.text}"
            val spannable = SpannableString(fullString)

            // Set the verse number to superscript
            val endOfNum = verse.verse.toString().length
            spannable.setSpan(SuperscriptSpan(), 0, endOfNum, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(RelativeSizeSpan(0.7f), 0, endOfNum, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Load notes first (they take priority for clicks)
            val notes = NotesRepository.getAllNotes(context).filter {
                it.volume == volumeName && it.book == bookName && 
                it.chapter == chapterNum && it.verse == verse.verse
            }
            
            // Load highlights and apply them with clickable spans
            val highlights = AnnotationRepository.getHighlightsForVerse(context, volumeName, bookName, chapterNum, verse.verse)
            for (highlight in highlights) {
                val start = highlight.selectionStart.coerceIn(0, fullString.length)
                val end = highlight.selectionEnd.coerceIn(0, fullString.length)
                if (start < end) {
                    // Add background color
                    spannable.setSpan(BackgroundColorSpan(highlight.color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    
                    // Add clickable span to show highlight dialog
                    val clickableSpan = object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            showViewHighlightDialog(context, highlight, verse) {
                                val pos = holder.adapterPosition
                                if (pos != RecyclerView.NO_POSITION) {
                                    notifyItemChanged(pos)
                                }
                            }
                        }
                        
                        override fun updateDrawState(ds: android.text.TextPaint) {
                            // Don't change text appearance for clickable highlights
                            ds.isUnderlineText = false
                        }
                    }
                    spannable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            
            // Apply notes with clickable spans (these override highlights if overlapping)
            for (note in notes) {
                val start = note.selectionStart.coerceIn(0, fullString.length)
                val end = note.selectionEnd.coerceIn(0, fullString.length)
                if (start < end) {
                    // Add background color
                    spannable.setSpan(BackgroundColorSpan(note.color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    
                    // Add clickable span to show note dialog
                    val clickableSpan = object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            showViewNoteDialog(context, note, verse) {
                                val pos = holder.adapterPosition
                                if (pos != RecyclerView.NO_POSITION) {
                                    notifyItemChanged(pos)
                                }
                            }
                        }
                        
                        override fun updateDrawState(ds: android.text.TextPaint) {
                            // Don't change text appearance for clickable notes
                            ds.isUnderlineText = false
                        }
                    }
                    spannable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            holder.tvVerseText.movementMethod = android.text.method.LinkMovementMethod.getInstance()
            holder.tvVerseText.setTextIsSelectable(true)
            holder.tvVerseText.setText(spannable, TextView.BufferType.SPANNABLE)
            holder.tvVerseText.isClickable = true
            holder.tvVerseText.isFocusable = true

            // Clear previous annotation icons
            holder.flNoteContainer.removeAllViews()

            // Post to ensure layout is ready for line calculation
            holder.tvVerseText.post {
                if (holder.tvVerseText.layout != null) {
                    renderAnnotationIcons(context, holder, verse)
                }
            }

            // Dismiss any previous popup when rebinding
            holder.actionPopup?.dismiss()
            holder.actionPopup = null

            // Show action bar on long press, independent of text selection system
            holder.tvVerseText.setOnLongClickListener {
                // Dismiss any existing popup first
                holder.actionPopup?.dismiss()
                
                // Show the action bar immediately
                holder.tvVerseText.postDelayed({
                    if (holder.tvVerseText.hasSelection()) {
                        val selStart = holder.tvVerseText.selectionStart
                        val selEnd = holder.tvVerseText.selectionEnd
                        if (selStart != -1 && selEnd != -1 && selStart != selEnd) {
                            holder.actionPopup = showCustomActionPopup(context, holder, verse, selStart, selEnd, null)
                        }
                    }
                }, 100) // Small delay to ensure selection is established
                
                false // Don't consume the event - let default selection happen
            }
            
            // Also allow default text selection behavior
            holder.tvVerseText.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
                override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                    // Hide the default menu
                    menu?.clear()
                    return true
                }

                override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                    menu?.clear()
                    return true
                }

                override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean {
                    return false
                }

                override fun onDestroyActionMode(mode: android.view.ActionMode?) {
                    // Don't dismiss popup here - keep it independent
                }
            }
        }
    }

    private fun showCustomActionPopup(
        context: Context,
        holder: ViewHolder,
        verse: Verse,
        selStart: Int,
        selEnd: Int,
        actionMode: android.view.ActionMode?
    ): PopupWindow {
        val popupView = LayoutInflater.from(context).inflate(R.layout.popup_selection_actions, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val selectedText = holder.tvVerseText.text.subSequence(selStart, selEnd).toString()

        // Close button
        popupView.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            popupWindow.dismiss()
            actionMode?.finish()
        }

        // Highlight button
        popupView.findViewById<ImageButton>(R.id.btnHighlight).setOnClickListener {
            // Get CURRENT selection at click time
            val currentStart = holder.tvVerseText.selectionStart
            val currentEnd = holder.tvVerseText.selectionEnd
            if (currentStart != -1 && currentEnd != -1 && currentStart != currentEnd) {
                val currentSelectedText = holder.tvVerseText.text.subSequence(currentStart, currentEnd).toString()
                popupWindow.dismiss()
                showHighlightDialog(context, verse, currentStart, currentEnd, currentSelectedText) {
                    val pos = holder.adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        notifyItemChanged(pos)
                    }
                }
                actionMode?.finish()
            }
        }

        // Note button
        popupView.findViewById<ImageButton>(R.id.btnNote).setOnClickListener {
            // Get CURRENT selection at click time
            val currentStart = holder.tvVerseText.selectionStart
            val currentEnd = holder.tvVerseText.selectionEnd
            if (currentStart != -1 && currentEnd != -1 && currentStart != currentEnd) {
                val currentSelectedText = holder.tvVerseText.text.subSequence(currentStart, currentEnd).toString()
                popupWindow.dismiss()
                showAddNoteDialog(context, verse, currentStart, currentEnd, currentSelectedText) {
                    val pos = holder.adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        notifyItemChanged(pos)
                    }
                }
                actionMode?.finish()
            }
        }

        // Tag button
        popupView.findViewById<ImageButton>(R.id.btnTag).setOnClickListener {
            popupWindow.dismiss()
            showAddTagDialog(context, verse) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos)
                }
            }
            actionMode?.finish()
        }

        // Bookmark button
        popupView.findViewById<ImageButton>(R.id.btnBookmark).setOnClickListener {
            popupWindow.dismiss()
            val isBookmarked = AnnotationRepository.toggleBookmark(context, volumeName, bookName, chapterNum, verse.verse)
            Toast.makeText(
                context,
                if (isBookmarked) "Bookmark added" else "Bookmark removed",
                Toast.LENGTH_SHORT
            ).show()
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos)
            }
            actionMode?.finish()
        }

        // Link button
        popupView.findViewById<ImageButton>(R.id.btnLink).setOnClickListener {
            popupWindow.dismiss()
            showLinkDialog(context, verse) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos)
                }
            }
            actionMode?.finish()
        }

        // Define button
        popupView.findViewById<ImageButton>(R.id.btnDefine).setOnClickListener {
            val currentStart = holder.tvVerseText.selectionStart
            val currentEnd = holder.tvVerseText.selectionEnd
            if (currentStart != -1 && currentEnd != -1 && currentStart != currentEnd) {
                val word = holder.tvVerseText.text.subSequence(currentStart, currentEnd).toString().trim()
                popupWindow.dismiss()
                showDefinition(context, word)
                actionMode?.finish()
            }
        }

        // Delete button
        popupView.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            popupWindow.dismiss()
            showDeleteAnnotationDialog(context, verse) {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos)
                }
            }
            actionMode?.finish()
        }

        // Show popup near the selection
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val location = IntArray(2)
        holder.tvVerseText.getLocationOnScreen(location)
        
        popupWindow.showAtLocation(
            holder.tvVerseText,
            Gravity.NO_GRAVITY,
            location[0] + holder.tvVerseText.width - popupView.measuredWidth - 20,
            location[1] + 100
        )
        
        return popupWindow
    }

    private fun renderAnnotationIcons(context: Context, holder: ViewHolder, verse: Verse) {
        holder.flNoteContainer.removeAllViews()
        val layout = holder.tvVerseText.layout ?: return

        // Get all annotations for this verse
        val notes = NotesRepository.getNotesForVerse(context, volumeName, bookName, chapterNum, verse.verse)
        val tags = AnnotationRepository.getTagsForVerse(context, volumeName, bookName, chapterNum, verse.verse)
        val bookmark = AnnotationRepository.getBookmarkForVerse(context, volumeName, bookName, chapterNum, verse.verse)
        val links = AnnotationRepository.getLinksForVerse(context, volumeName, bookName, chapterNum, verse.verse)

        var topOffset = 0

        // Render note icons
        for (note in notes) {
            val index = note.selectionStart.coerceIn(0, holder.tvVerseText.text.length)
            val line = layout.getLineForOffset(index)
            val top = layout.getLineTop(line)

            val icon = ImageView(context)
            icon.setImageResource(R.drawable.ic_note)
            icon.setColorFilter(note.color, PorterDuff.Mode.SRC_IN)
            val density = context.resources.displayMetrics.density
            val params = FrameLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt())
            params.topMargin = top
            icon.layoutParams = params

            icon.setOnClickListener {
                showViewNoteDialog(context, note) {
                    val pos = holder.adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        notifyItemChanged(pos)
                    }
                }
            }

            holder.flNoteContainer.addView(icon)
        }

        // Render tag icon if tags exist
        if (tags.isNotEmpty()) {
            val icon = ImageView(context)
            icon.setImageResource(R.drawable.ic_tag)
            val density = context.resources.displayMetrics.density
            val params = FrameLayout.LayoutParams((20 * density).toInt(), (20 * density).toInt())
            params.topMargin = topOffset
            icon.layoutParams = params

            icon.setOnClickListener {
                showViewTagsDialog(context, verse, tags) {
                    val pos = holder.adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        notifyItemChanged(pos)
                    }
                }
            }

            holder.flNoteContainer.addView(icon)
            topOffset += (25 * density).toInt()
        }

        // Render bookmark icon if exists
        if (bookmark != null) {
            val icon = ImageView(context)
            icon.setImageResource(R.drawable.ic_bookmark)
            val density = context.resources.displayMetrics.density
            val params = FrameLayout.LayoutParams((20 * density).toInt(), (20 * density).toInt())
            params.topMargin = topOffset
            icon.layoutParams = params

            icon.setOnClickListener {
                AnnotationRepository.toggleBookmark(context, volumeName, bookName, chapterNum, verse.verse)
                Toast.makeText(context, "Bookmark removed", Toast.LENGTH_SHORT).show()
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos)
                }
            }

            holder.flNoteContainer.addView(icon)
            topOffset += (25 * density).toInt()
        }

        // Render link icon if links exist
        if (links.isNotEmpty()) {
            val icon = ImageView(context)
            icon.setImageResource(R.drawable.ic_link)
            val density = context.resources.displayMetrics.density
            val params = FrameLayout.LayoutParams((20 * density).toInt(), (20 * density).toInt())
            params.topMargin = topOffset
            icon.layoutParams = params

            icon.setOnClickListener {
                showViewLinksDialog(context, verse, links)
            }

            holder.flNoteContainer.addView(icon)
        }
    }

    private fun showAddNoteDialog(context: Context, verse: Verse, start: Int, end: Int, selectedText: String, onNoteAdded: () -> Unit) {
        val rootLayout = LinearLayout(context)
        rootLayout.orientation = LinearLayout.VERTICAL
        val sideMargin = (22 * context.resources.displayMetrics.density).toInt()
        val topMargin = (16 * context.resources.displayMetrics.density).toInt()
        rootLayout.setPadding(sideMargin, topMargin, sideMargin, 0)

        val input = EditText(context)
        input.hint = "Enter note..."
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.minLines = 3
        input.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        input.isVerticalScrollBarEnabled = true
        rootLayout.addView(input)

        val colorPickerTitle = TextView(context)
        colorPickerTitle.text = "Highlight Color"
        colorPickerTitle.setTextColor(Color.BLACK)
        val titleTopPadding = (16 * context.resources.displayMetrics.density).toInt()
        val titleBottomPadding = (8 * context.resources.displayMetrics.density).toInt()
        colorPickerTitle.setPadding(0, titleTopPadding, 0, titleBottomPadding)
        rootLayout.addView(colorPickerTitle)

        val colors = arrayOf(
            Color.parseColor("#FFFACD"), // LemonChiffon
            Color.parseColor("#98FB98"), // PaleGreen
            Color.parseColor("#B0E0E6"), // PowderBlue
            Color.parseColor("#F08080"), // LightCoral
            Color.parseColor("#DDA0DD")  // Plum
        )
        var selectedColor = colors[0]

        val colorSwatchesLayout = LinearLayout(context)
        colorSwatchesLayout.orientation = LinearLayout.HORIZONTAL
        colorSwatchesLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL
        rootLayout.addView(colorSwatchesLayout)

        val swatchViews = mutableListOf<View>()
        val swatchSize = (40 * context.resources.displayMetrics.density).toInt()
        val swatchMargin = (6 * context.resources.displayMetrics.density).toInt()

        for (color in colors) {
            val swatch = View(context)
            swatch.setBackgroundColor(color)
            val params = LinearLayout.LayoutParams(swatchSize, swatchSize)
            params.setMargins(swatchMargin, 0, swatchMargin, 0)
            swatch.layoutParams = params

            swatch.setOnClickListener {
                selectedColor = color
                for (v in swatchViews) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }
                swatch.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
            }

            colorSwatchesLayout.addView(swatch)
            swatchViews.add(swatch)
        }

        swatchViews[0].animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()

        AlertDialog.Builder(context)
            .setTitle("Add Note")
            .setView(rootLayout)
            .setPositiveButton("Save") { _, _ ->
                val content = input.text.toString()
                if (content.isNotEmpty()) {
                    val note = Note(
                        id = UUID.randomUUID().toString(),
                        volume = volumeName,
                        book = bookName,
                        chapter = chapterNum,
                        verse = verse.verse,
                        selectionStart = start,
                        selectionEnd = end,
                        selectedText = selectedText,
                        noteContent = content,
                        timestamp = System.currentTimeMillis(),
                        color = selectedColor
                    )
                    NotesRepository.saveNote(context, note)
                    onNoteAdded()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showViewNoteDialog(context: Context, note: Note, onNoteAction: () -> Unit) {
        val rootLayout = LinearLayout(context)
        rootLayout.orientation = LinearLayout.VERTICAL

        val message = TextView(context)
        message.text = "Selected Text: \"${note.selectedText}\"\n\n${note.noteContent}"
        message.textSize = 16f
        message.setTextColor(android.graphics.Color.BLACK)
        val textPadding = (24 * context.resources.displayMetrics.density).toInt()
        message.setPadding(textPadding, textPadding, textPadding, 0)

        val scrollView = android.widget.ScrollView(context)
        scrollView.addView(message)
        rootLayout.addView(scrollView)

        val colorPickerTitle = TextView(context)
        colorPickerTitle.text = "Change Color"
        colorPickerTitle.setTextColor(android.graphics.Color.BLACK)
        colorPickerTitle.setPadding(textPadding, (16 * context.resources.displayMetrics.density).toInt(), textPadding, (8 * context.resources.displayMetrics.density).toInt())
        rootLayout.addView(colorPickerTitle)

        val colors = arrayOf(
            Color.parseColor("#FFFACD"), // LemonChiffon
            Color.parseColor("#98FB98"), // PaleGreen
            Color.parseColor("#B0E0E6"), // PowderBlue
            Color.parseColor("#F08080"), // LightCoral
            Color.parseColor("#DDA0DD")  // Plum
        )

        val colorSwatchesLayout = LinearLayout(context)
        colorSwatchesLayout.orientation = LinearLayout.HORIZONTAL
        colorSwatchesLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL
        colorSwatchesLayout.setPadding(0, 0, 0, (8 * context.resources.displayMetrics.density).toInt())
        rootLayout.addView(colorSwatchesLayout)

        lateinit var dialog: AlertDialog

        val swatchSize = (40 * context.resources.displayMetrics.density).toInt()
        val swatchMargin = (6 * context.resources.displayMetrics.density).toInt()
        val swatchParams = LinearLayout.LayoutParams(swatchSize, swatchSize)
        swatchParams.setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin)

        for (color in colors) {
            val swatch = View(context)
            swatch.setBackgroundColor(color)
            swatch.layoutParams = swatchParams
            swatch.setOnClickListener {
                val updatedNote = note.copy(color = color)
                NotesRepository.saveNote(context, updatedNote)
                onNoteAction()
                dialog.dismiss()
            }
            colorSwatchesLayout.addView(swatch)
        }

        val builder = AlertDialog.Builder(context)
            .setTitle("Note")
            .setView(rootLayout)
            .setPositiveButton("Close", null)
            .setNegativeButton("Delete") { _, _ ->
                NotesRepository.deleteNote(context, note.id)
                onNoteAction()
            }
        dialog = builder.create()
        dialog.show()
    }


    fun updateVerses(newVerses: List<Verse>, volume: String, book: String, chapter: Int) {
        this.verses = newVerses
        this.allVerses = newVerses
        this.filteredVerses = newVerses
        this.volumeName = volume
        this.bookName = book
        this.chapterNum = chapter
        notifyDataSetChanged()
    }

    // ===================== NEW DIALOG METHODS =====================

    private fun showHighlightDialog(context: Context, verse: Verse, start: Int, end: Int, selectedText: String, onHighlightAdded: () -> Unit) {
        val rootLayout = LinearLayout(context)
        rootLayout.orientation = LinearLayout.VERTICAL
        val sideMargin = (22 * context.resources.displayMetrics.density).toInt()
        val topMargin = (16 * context.resources.displayMetrics.density).toInt()
        rootLayout.setPadding(sideMargin, topMargin, sideMargin, 0)

        val colorPickerTitle = TextView(context)
        colorPickerTitle.text = "Highlight Color"
        colorPickerTitle.setTextColor(Color.BLACK)
        val titleBottomPadding = (8 * context.resources.displayMetrics.density).toInt()
        colorPickerTitle.setPadding(0, 0, 0, titleBottomPadding)
        rootLayout.addView(colorPickerTitle)

        val colors = arrayOf(
            Color.parseColor("#FFFACD"), // LemonChiffon
            Color.parseColor("#98FB98"), // PaleGreen
            Color.parseColor("#B0E0E6"), // PowderBlue
            Color.parseColor("#F08080"), // LightCoral
            Color.parseColor("#DDA0DD")  // Plum
        )
        var selectedColor = colors[0]

        val colorSwatchesLayout = LinearLayout(context)
        colorSwatchesLayout.orientation = LinearLayout.HORIZONTAL
        colorSwatchesLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL
        rootLayout.addView(colorSwatchesLayout)

        val swatchViews = mutableListOf<View>()
        val swatchSize = (40 * context.resources.displayMetrics.density).toInt()
        val swatchMargin = (6 * context.resources.displayMetrics.density).toInt()

        for (color in colors) {
            val swatch = View(context)
            swatch.setBackgroundColor(color)
            val params = LinearLayout.LayoutParams(swatchSize, swatchSize)
            params.setMargins(swatchMargin, 0, swatchMargin, 0)
            swatch.layoutParams = params

            swatch.setOnClickListener {
                selectedColor = color
                for (v in swatchViews) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }
                swatch.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
            }

            colorSwatchesLayout.addView(swatch)
            swatchViews.add(swatch)
        }

        swatchViews[0].animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()

        AlertDialog.Builder(context)
            .setTitle("Highlight Text")
            .setView(rootLayout)
            .setPositiveButton("Save") { _, _ ->
                val highlight = Highlight(
                    id = UUID.randomUUID().toString(),
                    volume = volumeName,
                    book = bookName,
                    chapter = chapterNum,
                    verse = verse.verse,
                    selectionStart = start,
                    selectionEnd = end,
                    selectedText = selectedText,
                    color = selectedColor,
                    timestamp = System.currentTimeMillis()
                )
                AnnotationRepository.saveHighlight(context, highlight)
                onHighlightAdded()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddTagDialog(context: Context, verse: Verse, onTagAdded: () -> Unit) {
        val input = EditText(context)
        input.hint = "Enter tag name..."
        val padding = (24 * context.resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(context)
            .setTitle("Add Tag")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val tagName = input.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    val tag = Tag(
                        id = UUID.randomUUID().toString(),
                        tagName = tagName,
                        volume = volumeName,
                        book = bookName,
                        chapter = chapterNum,
                        verse = verse.verse,
                        timestamp = System.currentTimeMillis()
                    )
                    AnnotationRepository.saveTag(context, tag)
                    Toast.makeText(context, "Tag added", Toast.LENGTH_SHORT).show()
                    onTagAdded()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showViewTagsDialog(context: Context, verse: Verse, tags: List<Tag>, onTagAction: () -> Unit) {
        val tagNames = tags.map { it.tagName }
        val tagIds = tags.map { it.id }

        AlertDialog.Builder(context)
            .setTitle("Tags")
            .setItems(tagNames.toTypedArray()) { _, which ->
                // Show delete confirmation
                AlertDialog.Builder(context)
                    .setTitle("Delete Tag?")
                    .setMessage("Delete tag \"${tagNames[which]}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        AnnotationRepository.deleteTag(context, tagIds[which])
                        Toast.makeText(context, "Tag deleted", Toast.LENGTH_SHORT).show()
                        onTagAction()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showLinkDialog(context: Context, verse: Verse, onLinkAdded: () -> Unit) {
        // Create a dialog with search functionality
        val dialogView = LinearLayout(context)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(40, 20, 40, 20)

        // Add title
        val title = TextView(context)
        title.text = "Link Scripture"
        title.textSize = 18f
        title.setTextColor(Color.BLACK)
        title.setPadding(0, 0, 0, 16)
        dialogView.addView(title)

        // Add search input
        val searchInput = EditText(context)
        searchInput.hint = "Search by book, chapter, or verse..."
        searchInput.setTextColor(Color.BLACK)
        searchInput.setHintTextColor(Color.GRAY)
        dialogView.addView(searchInput)

        // Create ListView for results
        val resultsListView = android.widget.ListView(context)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (300 * context.resources.displayMetrics.density).toInt()
        )
        layoutParams.setMargins(0, 16, 0, 0)
        resultsListView.layoutParams = layoutParams
        dialogView.addView(resultsListView)

        // Load ALL scriptures from all volumes for searching
        val allScriptureRefs = mutableListOf<ScriptureReference>()
        val volumes = mapOf(
            "Book of Mormon" to "book-of-mormon.json",
            "Old Testament" to "old-testament.json",
            "New Testament" to "new-testament.json",
            "Doctrine and Covenants" to "doctrine-and-covenants.json",
            "Pearl of Great Price" to "pearl-of-great-price.json"
        )

        // Load all scripture references
        for ((volumeName, fileName) in volumes) {
            val scriptureData = JsonUtils.getScriptures(context, fileName)
            scriptureData?.books?.forEach { book ->
                book.chapters.forEach { chapter ->
                    chapter.verses.forEach { verseData ->
                        allScriptureRefs.add(
                            ScriptureReference(
                                volume = volumeName,
                                book = book.book,
                                chapter = chapter.chapter,
                                verse = verseData.verse,
                                text = verseData.text
                            )
                        )
                    }
                }
            }
        }

        // Initial display (show first 50 results)
        var filteredRefs = allScriptureRefs.take(50)
        val adapter = android.widget.ArrayAdapter(
            context,
            android.R.layout.simple_list_item_1,
            filteredRefs.map { "${it.volume} - ${it.book} ${it.chapter}:${it.verse}" }
        )
        resultsListView.adapter = adapter

        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        // Handle search filtering
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().trim().lowercase()
                filteredRefs = if (query.isEmpty()) {
                    allScriptureRefs.take(50)
                } else {
                    allScriptureRefs.filter { ref ->
                        ref.volume.lowercase().contains(query) ||
                        ref.book.lowercase().contains(query) ||
                        "${ref.chapter}".contains(query) ||
                        "${ref.verse}".contains(query) ||
                        "${ref.book} ${ref.chapter}:${ref.verse}".lowercase().contains(query) ||
                        ref.text.lowercase().contains(query)
                    }.take(100)
                }
                
                adapter.clear()
                adapter.addAll(filteredRefs.map { "${it.volume} - ${it.book} ${it.chapter}:${it.verse}" })
                adapter.notifyDataSetChanged()
            }
        })

        // Handle item selection
        resultsListView.setOnItemClickListener { _, _, position, _ ->
            if (position < filteredRefs.size) {
                val selectedRef = filteredRefs[position]
                
                // Create the link
                val link = Link(
                    id = UUID.randomUUID().toString(),
                    sourceVolume = this.volumeName,
                    sourceBook = this.bookName,
                    sourceChapter = this.chapterNum,
                    sourceVerse = verse.verse,
                    linkedVolume = selectedRef.volume,
                    linkedBook = selectedRef.book,
                    linkedChapter = selectedRef.chapter,
                    linkedVerse = selectedRef.verse,
                    timestamp = System.currentTimeMillis()
                )
                AnnotationRepository.saveLink(context, link)
                Toast.makeText(
                    context,
                    "Linked to ${selectedRef.book} ${selectedRef.chapter}:${selectedRef.verse}",
                    Toast.LENGTH_SHORT
                ).show()
                onLinkAdded()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // Helper data class for scripture references
    private data class ScriptureReference(
        val volume: String,
        val book: String,
        val chapter: Int,
        val verse: Int,
        val text: String
    )


    private fun showViewLinksDialog(context: Context, verse: Verse, links: List<Link>) {
        val linkDescriptions = links.map { link ->
            // Determine which side of the link we're viewing from
            val isSource = link.sourceVolume == volumeName && link.sourceBook == bookName && 
                          link.sourceChapter == chapterNum && link.sourceVerse == verse.verse
            
            if (isSource) {
                // Fetch the linked verse text
                val verseText = getVerseText(context, link.linkedVolume, link.linkedBook, link.linkedChapter, link.linkedVerse)
                "→ ${link.linkedBook} ${link.linkedChapter}:${link.linkedVerse}\n\"$verseText\""
            } else {
                // Fetch the source verse text
                val verseText = getVerseText(context, link.sourceVolume, link.sourceBook, link.sourceChapter, link.sourceVerse)
                "← ${link.sourceBook} ${link.sourceChapter}:${link.sourceVerse}\n\"$verseText\""
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Linked Scriptures")
            .setItems(linkDescriptions.toTypedArray()) { _, which ->
                val selectedLink = links[which]
                val isSource = selectedLink.sourceVolume == volumeName && selectedLink.sourceBook == bookName && 
                              selectedLink.sourceChapter == chapterNum && selectedLink.sourceVerse == verse.verse
                
                // Show detailed view with options
                showLinkDetailDialog(context, selectedLink, isSource)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showLinkDetailDialog(context: Context, link: Link, isSource: Boolean) {
        val targetVolume = if (isSource) link.linkedVolume else link.sourceVolume
        val targetBook = if (isSource) link.linkedBook else link.sourceBook
        val targetChapter = if (isSource) link.linkedChapter else link.sourceChapter
        val targetVerse = if (isSource) link.linkedVerse else link.sourceVerse
        
        val verseText = getVerseText(context, targetVolume, targetBook, targetChapter, targetVerse)
        val reference = "$targetBook $targetChapter:$targetVerse"
        val message = "$reference\n\n\"$verseText\""
        
        AlertDialog.Builder(context)
            .setTitle("Linked Scripture")
            .setMessage(message)
            .setPositiveButton("Remove Link") { _, _ ->
                AlertDialog.Builder(context)
                    .setTitle("Remove Link?")
                    .setMessage("Remove this scripture link?")
                    .setPositiveButton("Remove") { _, _ ->
                        AnnotationRepository.deleteLink(context, link.id)
                        Toast.makeText(context, "Link removed", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun getVerseText(context: Context, volume: String, book: String, chapter: Int, verse: Int): String {
        // Map volume names to JSON file names
        val volumeFiles = mapOf(
            "Book of Mormon" to "book-of-mormon.json",
            "Old Testament" to "old-testament.json",
            "New Testament" to "new-testament.json",
            "Doctrine and Covenants" to "doctrine-and-covenants.json",
            "Pearl of Great Price" to "pearl-of-great-price.json"
        )
        
        val fileName = volumeFiles[volume] ?: return "Text not found"
        
        try {
            val scriptureData = JsonUtils.getScriptures(context, fileName)
            val foundBook = scriptureData?.books?.find { it.book == book }
            val foundChapter = foundBook?.chapters?.find { it.chapter == chapter }
            val foundVerse = foundChapter?.verses?.find { it.verse == verse }
            
            return foundVerse?.text ?: "Text not found"
        } catch (e: Exception) {
            return "Error loading text"
        }
    }

    private fun showDeleteAnnotationDialog(context: Context, verse: Verse, onAnnotationDeleted: () -> Unit) {
        val notes = NotesRepository.getNotesForVerse(context, volumeName, bookName, chapterNum, verse.verse)
        val highlights = AnnotationRepository.getHighlightsForVerse(context, volumeName, bookName, chapterNum, verse.verse)
        val tags = AnnotationRepository.getTagsForVerse(context, volumeName, bookName, chapterNum, verse.verse)
        val links = AnnotationRepository.getLinksForVerse(context, volumeName, bookName, chapterNum, verse.verse)

        val items = mutableListOf<String>()
        val types = mutableListOf<String>()
        val ids = mutableListOf<String>()

        notes.forEach {
            items.add("Note: ${it.noteContent.take(30)}...")
            types.add("note")
            ids.add(it.id)
        }
        highlights.forEach {
            items.add("Highlight: ${it.selectedText.take(30)}...")
            types.add("highlight")
            ids.add(it.id)
        }
        tags.forEach {
            items.add("Tag: ${it.tagName}")
            types.add("tag")
            ids.add(it.id)
        }
        links.forEach {
            items.add("Link: ${it.linkedBook} ${it.linkedChapter}:${it.linkedVerse}")
            types.add("link")
            ids.add(it.id)
        }

        if (items.isEmpty()) {
            Toast.makeText(context, "No annotations to delete", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(context)
            .setTitle("Delete Annotation")
            .setItems(items.toTypedArray()) { _, which ->
                AlertDialog.Builder(context)
                    .setTitle("Confirm Delete")
                    .setMessage("Delete this annotation?")
                    .setPositiveButton("Delete") { _, _ ->
                        when (types[which]) {
                            "note" -> NotesRepository.deleteNote(context, ids[which])
                            "highlight" -> AnnotationRepository.deleteHighlight(context, ids[which])
                            "tag" -> AnnotationRepository.deleteTag(context, ids[which])
                            "link" -> AnnotationRepository.deleteLink(context, ids[which])
                        }
                        Toast.makeText(context, "Annotation deleted", Toast.LENGTH_SHORT).show()
                        onAnnotationDeleted()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showViewHighlightDialog(
        context: Context,
        highlight: Highlight,
        verse: Verse,
        onChanged: () -> Unit
    ) {
        val message = "\"${highlight.selectedText}\"\n\nHighlight color: ${getColorName(highlight.color)}"
        
        AlertDialog.Builder(context)
            .setTitle("Highlight")
            .setMessage(message)
            .setPositiveButton("Change Color") { _, _ ->
                showChangeHighlightColorDialog(context, highlight, onChanged)
            }
            .setNegativeButton("Delete") { _, _ ->
                AlertDialog.Builder(context)
                    .setTitle("Delete Highlight")
                    .setMessage("Are you sure you want to delete this highlight?")
                    .setPositiveButton("Delete") { _, _ ->
                        AnnotationRepository.deleteHighlight(context, highlight.id)
                        Toast.makeText(context, "Highlight deleted", Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNeutralButton("Close", null)
            .show()
    }

    private fun showViewNoteDialog(
        context: Context,
        note: Note,
        verse: Verse,
        onChanged: () -> Unit
    ) {
        val message = "\"${note.selectedText}\"\n\n${note.noteContent}"
        
        AlertDialog.Builder(context)
            .setTitle("Note")
            .setMessage(message)
            .setPositiveButton("Edit") { _, _ ->
                showEditNoteDialog(context, note, onChanged)
            }
            .setNegativeButton("Delete") { _, _ ->
                AlertDialog.Builder(context)
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note?")
                    .setPositiveButton("Delete") { _, _ ->
                        NotesRepository.deleteNote(context, note.id)
                        Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNeutralButton("Close", null)
            .show()
    }

    private fun showChangeHighlightColorDialog(
        context: Context,
        highlight: Highlight,
        onChanged: () -> Unit
    ) {
        val rootLayout = LinearLayout(context)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setPadding(40, 20, 40, 20)

        val colors = arrayOf(
            Color.parseColor("#FFFACD"), // LemonChiffon
            Color.parseColor("#98FB98"), // PaleGreen
            Color.parseColor("#B0E0E6"), // PowderBlue
            Color.parseColor("#F08080"), // LightCoral
            Color.parseColor("#DDA0DD")  // Plum
        )
        var selectedColor = highlight.color

        val colorSwatchesLayout = LinearLayout(context)
        colorSwatchesLayout.orientation = LinearLayout.HORIZONTAL
        colorSwatchesLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL
        rootLayout.addView(colorSwatchesLayout)

        val swatchViews = mutableListOf<View>()
        val swatchSize = (40 * context.resources.displayMetrics.density).toInt()
        val swatchMargin = (6 * context.resources.displayMetrics.density).toInt()

        for (color in colors) {
            val swatch = View(context)
            swatch.setBackgroundColor(color)
            val params = LinearLayout.LayoutParams(swatchSize, swatchSize)
            params.setMargins(swatchMargin, 0, swatchMargin, 0)
            swatch.layoutParams = params

            swatch.setOnClickListener {
                selectedColor = color
                for (v in swatchViews) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }
                swatch.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
            }

            colorSwatchesLayout.addView(swatch)
            swatchViews.add(swatch)
            
            // Highlight current color
            if (color == highlight.color) {
                swatch.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Change Highlight Color")
            .setView(rootLayout)
            .setPositiveButton("Save") { _, _ ->
                val updatedHighlight = highlight.copy(color = selectedColor)
                AnnotationRepository.saveHighlight(context, updatedHighlight)
                Toast.makeText(context, "Highlight color updated", Toast.LENGTH_SHORT).show()
                onChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditNoteDialog(
        context: Context,
        note: Note,
        onChanged: () -> Unit
    ) {
        val rootLayout = LinearLayout(context)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.setPadding(40, 20, 40, 20)

        val input = EditText(context)
        input.setText(note.noteContent)
        input.hint = "Enter your note"
        input.setTextColor(Color.BLACK)
        input.setHintTextColor(Color.GRAY)
        rootLayout.addView(input)

        val colorPickerTitle = TextView(context)
        colorPickerTitle.text = "Highlight Color"
        colorPickerTitle.setTextColor(Color.BLACK)
        val titleBottomPadding = (8 * context.resources.displayMetrics.density).toInt()
        colorPickerTitle.setPadding(0, 20, 0, titleBottomPadding)
        rootLayout.addView(colorPickerTitle)

        val colors = arrayOf(
            Color.parseColor("#FFFACD"),
            Color.parseColor("#98FB98"),
            Color.parseColor("#B0E0E6"),
            Color.parseColor("#F08080"),
            Color.parseColor("#DDA0DD")
        )
        var selectedColor = note.color

        val colorSwatchesLayout = LinearLayout(context)
        colorSwatchesLayout.orientation = LinearLayout.HORIZONTAL
        colorSwatchesLayout.gravity = android.view.Gravity.CENTER_HORIZONTAL
        rootLayout.addView(colorSwatchesLayout)

        val swatchViews = mutableListOf<View>()
        val swatchSize = (40 * context.resources.displayMetrics.density).toInt()
        val swatchMargin = (6 * context.resources.displayMetrics.density).toInt()

        for (color in colors) {
            val swatch = View(context)
            swatch.setBackgroundColor(color)
            val params = LinearLayout.LayoutParams(swatchSize, swatchSize)
            params.setMargins(swatchMargin, 0, swatchMargin, 0)
            swatch.layoutParams = params

            swatch.setOnClickListener {
                selectedColor = color
                for (v in swatchViews) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                }
                swatch.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
            }

            colorSwatchesLayout.addView(swatch)
            swatchViews.add(swatch)
            
            if (color == note.color) {
                swatch.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
            }
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Note")
            .setView(rootLayout)
            .setPositiveButton("Save") { _, _ ->
                val content = input.text.toString()
                if (content.isNotEmpty()) {
                    val updatedNote = note.copy(noteContent = content, color = selectedColor)
                    NotesRepository.saveNote(context, updatedNote)
                    Toast.makeText(context, "Note updated", Toast.LENGTH_SHORT).show()
                    onChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getColorName(color: Int): String {
        return when (color) {
            Color.parseColor("#FFFACD") -> "Yellow"
            Color.parseColor("#98FB98") -> "Green"
            Color.parseColor("#B0E0E6") -> "Blue"
            Color.parseColor("#F08080") -> "Red"
            Color.parseColor("#DDA0DD") -> "Purple"
            else -> "Custom"
        }
    }
}

    // Dictionary lookup methods
    private fun showDefinition(context: Context, word: String) {
        try {
            val jsonString = context.assets.open("dictionary_webster1828.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            
            // Clean the word for lookup (remove punctuation, lowercase)
            val cleanWord = word.replace(Regex("[^a-zA-Z]"), "").lowercase()
            
            // Try to find the exact word first
            var definition: String? = null
            for (i in 0 until jsonArray.length()) {
                val entry = jsonArray.getJSONObject(i)
                if (entry.getString("word").equals(cleanWord, ignoreCase = true)) {
                    definition = entry.getString("definition")
                    break
                }
            }
            
            // If not found, try without common suffixes
            if (definition == null) {
                val variations = listOf(
                    cleanWord,
                    cleanWord.removeSuffix("s"),  // plural
                    cleanWord.removeSuffix("es"), // plural
                    cleanWord.removeSuffix("ed"), // past tense
                    cleanWord.removeSuffix("ing"), // present participle
                    cleanWord.removeSuffix("ly")  // adverb
                )
                
                for (variation in variations) {
                    for (i in 0 until jsonArray.length()) {
                        val entry = jsonArray.getJSONObject(i)
                        if (entry.getString("word").equals(variation, ignoreCase = true)) {
                            definition = entry.getString("definition")
                            break
                        }
                    }
                    if (definition != null) break
                }
            }
            
            if (definition != null) {
                showDefinitionDialog(context, word, definition)
            } else {
                Toast.makeText(context, "Definition not found for '$word'", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading dictionary: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDefinitionDialog(context: Context, word: String, definition: String) {
        AlertDialog.Builder(context)
            .setTitle("1828 Dictionary: $word")
            .setMessage(definition)
            .setPositiveButton("Close", null)
            .show()
    }
