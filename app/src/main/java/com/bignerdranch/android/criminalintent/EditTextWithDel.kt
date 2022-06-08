package com.bignerdranch.android.criminalintent

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.EditText


class EditTextWithDel : androidx.appcompat.widget.AppCompatEditText {
    private var imgInable: Drawable? = null
    private val imgAble: Drawable? = null
    private var mContext: Context

    constructor(context: Context) : super(context) {
        mContext = context
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        mContext = context
        init()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun init() {
        imgInable = mContext.resources.getDrawable(R.drawable.ic_delete_gray)
        addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {
                setDrawable()
            }
        })
        setDrawable()
    }

    // Устанавливаем на удаление картинок
    private fun setDrawable() {
        if (length() < 1) setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            null,
            null
        ) else setCompoundDrawablesWithIntrinsicBounds(null, null, imgInable, null)
    }

    // обрабатываем событие удаления
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (imgInable != null && event.action == MotionEvent.ACTION_UP) {
            val eventX = event.rawX.toInt()
            val eventY = event.rawY.toInt()
            val rect = Rect()
            getGlobalVisibleRect(rect)
            rect.left = rect.right - 100
            if (rect.contains(eventX, eventY)) setText("")
        }
        return super.onTouchEvent(event)
    }


    companion object {
        private const val TAG = "EditTextWithDel"
    }
}