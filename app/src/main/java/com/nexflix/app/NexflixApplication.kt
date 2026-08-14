package com.nexflix.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Environment
import java.io.File

class NexflixApplication : Application() {

            override fun attachBaseContext(base: Context?) {
                        super.attachBaseContext(base)

                                writeLog("=== App attachBaseContext reached ===")

                                        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                                                    val trace = throwable.stackTraceToString()
                                                                writeLog("=== CRASH ===\n$trace")
                                                                
                                                                            try {
                                                                                                val intent = Intent(this, CrashActivity::class.java)
                                                                                                                intent.putExtra("error", trace)
                                                                                                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                                                                                                                startActivity(intent)
                                                                            } catch (e: Exception) {
                                                                                                writeLog("Could not launch CrashActivity: ${e.stackTraceToString()}")
                                                                            }
                                                                                        android.os.Process.killProcess(android.os.Process.myPid())
                                                                                                    System.exit(10)
                                        }

                                                writeLog("=== Handler registered successfully ===")
            }

                private fun writeLog(message: String) {
                                try {
                                                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Nexflix")
                                                                if (!dir.exists()) dir.mkdirs()
                                                                            val file = File(dir, "crash_log.txt")
                                                                                        file.appendText("${System.currentTimeMillis()}: $message\n\n")
                                } catch (e: Exception) {
                                                    try {
                                                                        val file = File(getExternalFilesDir(null), "crash_log.txt")
                                                                                        file.appendText("${System.currentTimeMillis()}: $message\n\n")
                                                    } catch (e2: Exception) {
                                                    }
                                }
                }
}
                                                    }
                                                    }
                                }
                                }
                }
                                                                            }
                                                                            }}
            }
}