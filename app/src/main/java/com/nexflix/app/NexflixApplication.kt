package com.nexflix.app

import android.app.Application
import android.content.Context
import android.content.Intent

class NexflixApplication : Application() {

            override fun attachBaseContext(base: Context?) {
                        super.attachBaseContext(base)

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