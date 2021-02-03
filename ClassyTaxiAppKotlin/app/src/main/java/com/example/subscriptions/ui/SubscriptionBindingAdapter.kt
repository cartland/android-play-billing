/*
 * Copyright 2018 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.subscriptions.ui

import android.databinding.BindingAdapter
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.example.subscriptions.R
import com.example.subscriptions.data.ContentResource
import com.example.subscriptions.data.SubscriptionStatus
import kotlinx.android.synthetic.main.fragment_home.view.home_basic_image
import kotlinx.android.synthetic.main.fragment_home.view.home_basic_text
import kotlinx.android.synthetic.main.fragment_premium.view.premium_premium_image
import kotlinx.android.synthetic.main.fragment_premium.view.premium_premium_text
import java.text.SimpleDateFormat
import java.util.Calendar

private const val TAG = "BindingAdapter"

@BindingAdapter("monitorSubscriptions")
fun monitorSubscriptions(view: View, subscriptions: List<SubscriptionStatus>?) {
    // Do nothing.
    // TODO: Improve LiveData scope management.
    // If this BindingAdapter is removed, then most of the LiveData fields in
    // SubscriptionStatusViewModel will not update when the data changes.
}

/**
 * Update basic content when the URL changes.
 *
 * When the image URL content changes, the binding adapter triggers this view in the layout XML.
 * See the layout XML files for the app:updateBasicContent attribute.
 */
@BindingAdapter("updateBasicContent")
fun updateBasicContent(view: View, basicContent: ContentResource?) {
    val image = view.home_basic_image
    val textView = view.home_basic_text
    val url = basicContent?.url
    if (url != null) {
        image.run {
            Log.d(TAG, "Loading image for basic content: $url")
            visibility = View.VISIBLE
            Glide.with(view.context)
                    .load(url)
                    .into(this)
        }
        textView.run {
            text = view.resources.getString(R.string.basic_content_text)
        }
    } else {
        image.run {
            visibility = View.GONE
        }
        textView.run {
            text = view.resources.getString(R.string.no_basic_content)
        }
    }
}

/**
 * Update premium content on the Premium fragment when the URL changes.
 *
 * When the image URL content changes, the binding adapter triggers this view in the layout XML.
 * See the layout XML files for the app:updatePremiumContent attribute.
 */
@BindingAdapter("updatePremiumContent")
fun updatePremiumContent(view: View, premiumContent: ContentResource?) {
    val image = view.premium_premium_image
    val textView = view.premium_premium_text
    val url = premiumContent?.url
    if (url != null) {
        image.run {
            Log.d(TAG, "Loading image for premium content: $url")
            visibility = View.VISIBLE
            Glide.with(context)
                    .load(url)
                    .into(this)
        }
        textView.run {
            text = view.resources.getString(R.string.premium_content_text)
        }
    } else {
        image.run {
            visibility = View.GONE
        }
        textView.run {
            text = resources.getString(R.string.no_premium_content)
        }
    }
}

/**
 * Update views on the Settings fragment when the subscription changes.
 *
 * When the subscription changes, the binding adapter triggers this view in the layout XML.
 * See the layout XML files for the app:updateSettingsViews attribute.
 */
@BindingAdapter("updateSettingsViews")
fun updateSettingsViews(view: View, subscriptions: List<SubscriptionStatus>?) {

}

/**
 * Get a readable expiry date from a subscription.
 */
fun getHumanReadableExpiryDate(subscription: SubscriptionStatus): String {
    val milliSeconds = subscription.activeUntilMillisec
    val formatter = SimpleDateFormat.getDateInstance()
    val calendar = Calendar.getInstance()
    calendar.setTimeInMillis(milliSeconds)
    if (milliSeconds == 0L) {
        Log.d(TAG, "Suspicious time: 0 milliseconds. JSON: ${subscription}")
    } else {
        Log.d(TAG, "Expiry time millis: ${subscription.activeUntilMillisec}")
    }
    return formatter.format(calendar.getTime())
}
