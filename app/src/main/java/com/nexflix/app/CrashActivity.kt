package com.nexflix.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)

                            val errorText = intent.getStringExtra("error") ?: "Unknown error"

                                    val root = LinearLayout(this).apply {
                                                    orientation = LinearLayout.VERTICAL
                                                                setPadding(32, 64, 32, 32)
                                    }

                                            val title = TextView(this).apply {
                                                            text = "Nexflix crashed - copy this and send it"
                                                                        textSize = 18f
                                                                                    setPadding(0, 0, 0, 24)
                                            }

                                                    val scrollView = ScrollView(this).apply {
                                                                    layoutParams = LinearLayout.LayoutParams(
                                                                                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                                                                    )
                                                    }

                                                            val textView = TextView(this).apply {
                                                                            text = errorText
                                                                                        textIsSelectable = true
                                                                                                    setPadding(16, 16, 16, 16)
                                                            }
                                                                    scrollView.addView(textView)

                                                                            val copyButton = Button(this).apply {
                                                                                            text = "Copy Error"
                                                                                                        setOnClickListener {
                                                                                                                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                                                                                            val clip = ClipData.newPlainText("Nexflix crash", errorText)
                                                                                                                                                            clipboard.setPrimaryClip(clip)
                                                                                                                                                                            Toast.makeText(this@CrashActivity, "Copied", Toast.LENGTH_SHORT).show()
                                                                                                        }
                                                                            }

                                                                                    val closeButton = Button(this).apply {
                                                                                                    text = "Close App"
                                                                                                                setOnClickListener { finishAffinity() }
                                                                                    }

                                                                                            root.addView(title)
                                                                                                    root.addView(scrollView)
                                                                                                            root.addView(copyButton)
                                                                                                                    root.addView(closeButton)

                                                                                                                            setContentView(root)
        }
}
                                                                                    }
                                                                                                        }
                                                                            }
                                                            }
                                                                    )
                                                    }
                                            }
                                    }
        }
}