package com.anur.vcardpro.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.anur.vcardpro.MainActivity
import com.anur.vcardpro.R
import com.anur.vcardpro.model.ContactShare
import com.anur.vcardpro.model.VisitorContactData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReceivedContactsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // In ReceivedContactsWidget.kt, CHANGE the updateAppWidget function:
    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_received_contacts)

            try {
                // Load the selected contact (not all contacts)
                val prefs = context.getSharedPreferences("contact_widget_data", Context.MODE_PRIVATE)
                val contactJson = prefs.getString("selected_contact", null)

                if (contactJson != null) {
                    val gson = Gson()
                    val contact: ContactShare = gson.fromJson(contactJson, ContactShare::class.java)

                    val visitorData = contact.visitorContactData?.let {
                        try {
                            gson.fromJson(it, VisitorContactData::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val name = visitorData?.name ?: contact.name ?: "Unknown"
                    val phone = contact.viewerPhone ?: visitorData?.phone ?: "No phone"
                    val email = contact.viewerEmail ?: visitorData?.email ?: "No email"

                    views.setTextViewText(R.id.widget_title, "📱 $name")
                    views.setTextViewText(R.id.widget_contacts, "📞 $phone\n📧 $email")

                } else {
                    views.setTextViewText(R.id.widget_title, "📱 Contact Widget")
                    views.setTextViewText(R.id.widget_contacts, "No contact selected")
                }

            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_contacts, "Error loading contact")
            }

            // Click to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}