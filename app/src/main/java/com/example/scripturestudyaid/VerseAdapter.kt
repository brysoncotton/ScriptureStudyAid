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
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVerseText: TextView = view.findViewById(R.id.tvVerseText)
        val flNoteContainer: FrameLayout = view.findViewById(R.id.flNoteContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val verse = verses[position]
        val context = holder.itemView.context
        val fullString = "${verse.verse} ${verse.text}"
        val spannable = SpannableString(fullString)

        // Set the verse number to superscript
        val endOfNum = verse.verse.toString().length
        spannable.setSpan(SuperscriptSpan(), 0, endOfNum, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(RelativeSizeSpan(0.7f), 0, endOfNum, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Load notes for underline
        val notes = NotesRepository.getNotesForVerse(context, volumeName, bookName, chapterNum, verse.verse)
        for (note in notes) {
            val start = note.selectionStart.coerceIn(0, fullString.length)
            val end = note.selectionEnd.coerceIn(0, fullString.length)
            if (start < end) {
                spannable.setSpan(BackgroundColorSpan(note.color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        holder.tvVerseText.text = spannable

        // Clear previous notes
        holder.flNoteContainer.removeAllViews()

        // Post to ensure layout is ready for line calculation
        holder.tvVerseText.post {
            if (holder.tvVerseText.layout != null) {
                renderNotes(context, holder, notes)
            }
        }

        // Custom Selection Action Mode
        holder.tvVerseText.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.menu_selection, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (item?.itemId == R.id.add_note) { // Add Note
                    val start = holder.tvVerseText.selectionStart
                    val end = holder.tvVerseText.selectionEnd
                    if (start != -1 && end != -1 && start != end) {
                        val selectedText = holder.tvVerseText.text.subSequence(start, end).toString()
                        showAddNoteDialog(context, verse, start, end, selectedText) {
                            val pos = holder.adapterPosition
                            if (pos != RecyclerView.NO_POSITION) {
                                notifyItemChanged(pos)
                            }
                        }
                    }
                    mode?.finish()
                    return true
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
    }

    private fun renderNotes(context: Context, holder: ViewHolder, notes: List<Note>) {
        holder.flNoteContainer.removeAllViews()
        val layout = holder.tvVerseText.layout ?: return

        for (note in notes) {
            // Find line for the start of the note
            // Ensure index is within bounds (text might have changed? unlikely but be safe)
            val index = note.selectionStart.coerceIn(0, holder.tvVerseText.text.length)
            val line = layout.getLineForOffset(index)
            val top = layout.getLineTop(line)

            val icon = ImageView(context)
            icon.setImageResource(R.drawable.ic_note) // Ensure this drawable exists
            icon.setColorFilter(note.color, PorterDuff.Mode.SRC_IN)
            val params = FrameLayout.LayoutParams(40, 40) // px size, maybe convert dp
            // approximate dp to px
            val density = context.resources.displayMetrics.density
            params.width = (24 * density).toInt()
            params.height = (24 * density).toInt()
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

    override fun getItemCount() = verses.size

    fun updateVerses(newVerses: List<Verse>, volume: String, book: String, chapter: Int) {
        this.verses = newVerses
        this.volumeName = volume
        this.bookName = book
        this.chapterNum = chapter
        notifyDataSetChanged()
    }
}
