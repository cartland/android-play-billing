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

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.example.subscriptions.Constants
import com.example.subscriptions.R
import com.example.subscriptions.SubApp
import com.example.subscriptions.billing.isAccountHold
import com.example.subscriptions.billing.isBasicContent
import com.example.subscriptions.billing.isGracePeriod
import com.example.subscriptions.billing.isPaused
import com.example.subscriptions.billing.isPremiumContent
import com.example.subscriptions.billing.isSubscriptionRestore
import com.example.subscriptions.billing.isTransferRequired
import com.example.subscriptions.data.SubscriptionStatus
import com.example.subscriptions.utils.basicTextForSubscription
import com.example.subscriptions.utils.premiumTextForSubscription
import com.google.firebase.iid.FirebaseInstanceId

class SubscriptionStatusViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Data repository.
     */
    private val repository = (application as SubApp).repository

    /**
     * Live data is true when there are pending network requests.
     */
    val loading: LiveData<Boolean>
        get() = repository.loading

    /**
     * Subscriptions LiveData.
     */
    val subscriptions = MediatorLiveData<List<SubscriptionStatus>>()

    val showPaywallUI = MutableLiveData<Boolean>()
    val showBasicSubscriptionUI = MutableLiveData<Boolean>()
    val showPremiumSubscriptionUI = MutableLiveData<Boolean>()
    val showUpgradeUI = MutableLiveData<Boolean>()
    val showRestoreUI = MutableLiveData<Boolean>()
    val restoreMessageWithDate = MutableLiveData<String>()
    val showGracePeriodUI = MutableLiveData<Boolean>()
    val showAccountHoldUI = MutableLiveData<Boolean>()
    val showAccountPausedUI = MutableLiveData<Boolean>()
    val showTransferUI = MutableLiveData<Boolean>()
    val transferSettingsText = MutableLiveData<String>()
    val basicSettingsButtonText = MutableLiveData<String>()
    val premiumSettingsButtonText = MutableLiveData<String>()

    init {
        // Database changes are observed by the ViewModel.
        subscriptions.addSource(repository.subscriptions) {
            Log.d("Repository", "Subscriptions updated: ${it?.size}")
            subscriptions.postValue(it)
            // Update attributes.
            if (it == null) {
                return@addSource
            }
            var paywallToBeShown = true
            var basicSubscription = false
            var premiumSubscription = false
            var upgradeOption = false
            var restoreOption = false
            var restoreMessage = ""
            var gracePeriodOption = false
            var accountHoldOption = false
            var accountPausedOption = false
            var transferOption = false
            var transferMessage = ""
            var basicButtonMessage =
                    application.resources.getString(R.string.subscription_option_basic_message)
            var premiumButtonMessage =
                    application.resources.getString(R.string.subscription_option_premium_message)
            // Update based on subscription information.
            var basicRequiresTransfer = false
            var premiumRequiresTransfer = false
            // Iterate through all subscriptions.
            for (subscription in it) {
                if (isSubscriptionRestore(subscription)) {
                    Log.d(TAG, "restore VISIBLE")
                    restoreOption = true
                    val expiryDate = getHumanReadableExpiryDate(subscription)
                    restoreMessage = application.resources.getString(
                            R.string.restore_message_with_date,
                            expiryDate
                    )
                    paywallToBeShown = false
                }
                if (isGracePeriod(subscription)) {
                    Log.d(TAG, "grace period VISIBLE")
                    gracePeriodOption = true
                    paywallToBeShown = false
                }
                if (isTransferRequired(subscription)) {
                    Log.d(TAG, "transfer VISIBLE")
                    transferOption = true
                    paywallToBeShown = false
                }
                if (isAccountHold(subscription)) {
                    Log.d(TAG, "account hold VISIBLE")
                    accountHoldOption = true
                    paywallToBeShown = false
                }
                if (isPaused(subscription)) {
                    Log.d(TAG, "account paused VISIBLE")
                    accountPausedOption = true
                    paywallToBeShown = false
                }
                if (isBasicContent(subscription) || isPremiumContent(subscription)) {
                    Log.d(TAG, "basic VISIBLE")
                    basicSubscription = true
                    paywallToBeShown = false
                }
                if (isPremiumContent(subscription)) {
                    Log.d(TAG, "premium VISIBLE")
                    premiumSubscription = true
                    paywallToBeShown = false
                    upgradeOption = false
                }
                if (isBasicContent(subscription) && !isPremiumContent(subscription) && !premiumSubscription) {
                    Log.d(TAG, "basic VISIBLE")
                    // Upgrade message will be hidden if a premium subscription is found later.
                    upgradeOption = true
                    paywallToBeShown = false
                }
                // Settings page information.
                when (subscription.sku) {
                    Constants.BASIC_SKU -> {
                        basicButtonMessage = basicTextForSubscription(application.resources, subscription)
                        if (isTransferRequired(subscription)) {
                            basicRequiresTransfer = true
                        }
                    }
                    Constants.PREMIUM_SKU -> {
                        premiumButtonMessage = premiumTextForSubscription(application.resources, subscription)
                        if (isTransferRequired(subscription)) {
                            premiumRequiresTransfer = true
                        }
                    }
                }
            }
            val message = when {
                basicRequiresTransfer && premiumRequiresTransfer -> {
                    val basicName = application.resources.getString(R.string.basic_button_text)
                    val premiumName = application.resources.getString(R.string.premium_button_text)
                    application.resources.getString(
                            R.string.transfer_message_with_two_skus, basicName, premiumName)
                }
                basicRequiresTransfer -> {
                    val basicName = application.resources.getString(R.string.basic_button_text)
                    application.resources.getString(R.string.transfer_message_with_sku, basicName)
                }
                premiumRequiresTransfer -> {
                    val premiumName = application.resources.getString(R.string.premium_button_text)
                    application.resources.getString(R.string.transfer_message_with_sku, premiumName)
                }
                else -> null
            }
            if (message != null) {
                Log.d(TAG, "transfer VISIBLE")
                transferMessage = message
            } else {
                transferMessage = application.resources.getString(R.string.transfer_message)
            }
            showPaywallUI.postValue(paywallToBeShown)
            showBasicSubscriptionUI.postValue(basicSubscription)
            showPremiumSubscriptionUI.postValue(premiumSubscription)
            showUpgradeUI.postValue(upgradeOption)
            showRestoreUI.postValue(restoreOption)
            restoreMessageWithDate.postValue(restoreMessage)
            showGracePeriodUI.postValue(gracePeriodOption)
            showAccountHoldUI.postValue(accountHoldOption)
            showAccountPausedUI.postValue(accountPausedOption)
            showTransferUI.postValue(transferOption)
            transferSettingsText.postValue(transferMessage)
            basicSettingsButtonText.postValue(basicButtonMessage)
            premiumSettingsButtonText.postValue(premiumButtonMessage)
        }
    }

    /**
     * Live Data with the basic content.
     */
    val basicContent = repository.basicContent

    /**
     * Live Data with the premium content.
     */
    val premiumContent = repository.premiumContent

    fun unregisterInstanceId() {
        // Unregister current Instance ID before the user signs out.
        // This is an authenticated call, so you cannot do this after the sign-out has completed.
        instanceIdToken?.let {
            repository.unregisterInstanceId(it)
        }
    }

    fun userChanged() {
        repository.deleteLocalUserData()
        FirebaseInstanceId.getInstance().token?.let {
            registerInstanceId(it)
        }
        repository.fetchSubscriptions()
    }

    fun manualRefresh() {
        repository.fetchSubscriptions()
    }

    /**
     * Keep track of the last Instance ID to be registered, so that it
     * can be unregistered when the user signs out.
     */
    private var instanceIdToken: String? = null

    /**
     * Register Instance ID.
     */
    private fun registerInstanceId(token: String) {
        repository.registerInstanceId(token)
        // Keep track of the Instance ID so that it can be unregistered.
        instanceIdToken = token
    }

    /**
     * Register a new subscription.
     */
    fun registerSubscription(sku: String, purchaseToken: String) =
            repository.registerSubscription(sku, purchaseToken)

    /**
     * Transfer the subscription to this account.
     */
    fun transferSubscriptions() {
        Log.d(TAG, "transferSubscriptions")
        subscriptions.value?.let {
            for (subscription in it) {
                val sku = subscription.sku
                val purchaseToken = subscription.purchaseToken
                if (sku != null && purchaseToken != null) {
                    repository.transferSubscription(sku = sku, purchaseToken = purchaseToken)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SubViewModel"
    }

}
