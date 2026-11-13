package com.vaani.keyboard

import android.app.Activity
import android.os.Bundle
import android.widget.EditText

/**
 * A plain Activity with a plain `EditText`, used only by the instrumented
 * IME test (requirement #3) to drive Vaani's real on-screen keys against a
 * real focused field and assert on the committed text — the same technique
 * real third-party keyboard apps use to validate themselves in CI.
 */
class TestEditTextActivity : Activity() {
    lateinit var editText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editText = EditText(this).apply { id = android.R.id.edit }
        setContentView(editText)
    }
}
