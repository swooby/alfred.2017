package com.swooby.alfred

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.widget.AppCompatSpinner
import android.annotation.SuppressLint
import android.view.View.OnTouchListener

/**
 * https://stackoverflow.com/a/69320599/252308
 */
class UserTouchSpinner : AppCompatSpinner {
    companion object {
        private const val MODE_THEME = -1
    }

    /**
     * A clone of AdapterView.OnItemSelectedListener that adds a `userTouched: Boolean` parameter to each method.
     */
    interface OnItemSelectedListener {
        /**
         *
         * Callback method to be invoked when an item in this view has been
         * selected. This callback is invoked only when the newly selected
         * position is different from the previously selected position or if
         * there was no selected item.
         *
         * Implementers can call getItemAtPosition(position) if they need to access the
         * data associated with the selected item.
         *
         * @param parent The AdapterView where the selection happened
         * @param view The view within the AdapterView that was clicked
         * @param position The position of the view in the adapter
         * @param id The row id of the item that is selected
         * @param userTouched true if the user touched the view, otherwise false
         */
        fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long, userTouched: Boolean)

        /**
         * Callback method to be invoked when the selection disappears from this
         * view. The selection can disappear for instance when touch is activated
         * or when the adapter becomes empty.
         *
         * @param parent The AdapterView that now contains no selected item.
         * @param userTouched true if the user touched the view, otherwise false
         */
        fun onNothingSelected(parent: AdapterView<*>?, userTouched: Boolean)
    }

    private var userTouched = false
    private var externalOnTouchListener: OnTouchListener? = null
    @SuppressLint("ClickableViewAccessibility")
    private var internalOnTouchListener = OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> userTouched = true
        }
        externalOnTouchListener?.onTouch(v, event) ?: false
    }
    private var externalOnItemSelectedListener: OnItemSelectedListener? = null
    private val internalOnItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            externalOnItemSelectedListener?.onItemSelected(parent, view, position, id, userTouched)
            userTouched = false
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            externalOnItemSelectedListener?.onNothingSelected(parent, userTouched)
            userTouched = false
        }
    }

    constructor(context: Context) : this(context, null)

    @Suppress("unused")
    constructor(context: Context, mode: Int) : this(context, null, android.R.attr.spinnerStyle, mode)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, android.R.attr.spinnerStyle)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : this(context, attrs, defStyle, MODE_THEME)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int, mode: Int) : this(context, attrs, defStyle, mode, null)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int, mode: Int, popupTheme: Resources.Theme?) :
            super(context, attrs, defStyle, mode, popupTheme) {
        super.setOnTouchListener(internalOnTouchListener)
        super.setOnItemSelectedListener(internalOnItemSelectedListener)
    }

    override fun setOnTouchListener(listener: OnTouchListener?) {
        externalOnTouchListener = listener
    }

    override fun setOnItemSelectedListener(listener: AdapterView.OnItemSelectedListener?) {
        throw UnsupportedOperationException("Use setOnItemSelectedListener(listener: MySpinner.OnItemSelectedListener?) instead")
    }

    @Suppress("unused")
    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        externalOnItemSelectedListener = listener
    }
}