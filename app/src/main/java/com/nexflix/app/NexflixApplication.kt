package com.nexflix.app

import android.app.Application
import android.content.Intent

class NexflixApplication : Application() {

        override fun onCreate() {
                    super.onCreate()

                            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                                        try {
                                                            val trace = throwable.stackTraceToString()
                                                                            val intent = Intent(this, CrashActivity::class.java)
                                                                                            intent.putExtra("error", trace)
                                                                                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                                                                                            startActivity(intent)
                                        } catch (e: Exception) {
                                        }
                                                    android.os.Process.killProcess(android.os.Process.myPid())
                                                                System.exit(10)
                            }
        }
}
                                        }
                                        }}
        }
}