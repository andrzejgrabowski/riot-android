/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.fragments.terms

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import im.vector.R
import im.vector.ui.arch.LiveEvent
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.Log
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.core.model.MatrixError
import org.matrix.androidsdk.features.terms.GetTermsResponse
import org.matrix.androidsdk.features.terms.TermsManager

class AcceptTermsViewModel : ViewModel() {

    lateinit var termsArgs: ServiceTermsArgs

    var termsList: MutableLiveData<List<Term>> = MutableLiveData()
    var isLoading: MutableLiveData<Boolean> = MutableLiveData()
    var hasError: MutableLiveData<Boolean> = MutableLiveData()
    var successfullyAccepted: MutableLiveData<Boolean> = MutableLiveData()

    var popError: MutableLiveData<LiveEvent<Int>> = MutableLiveData()

    var mxSession: MXSession? = null
    var termsManager: TermsManager? = null

    fun markTermAsAccepted(url: String, accepted: Boolean) {
        termsList.value?.map {
            if (it.url == url) {
                it.copy(accepted = accepted)
            } else it
        }?.let {
            termsList.postValue(it)
        }
    }

    fun initSession(session: MXSession?) {
        mxSession = session
        termsManager = mxSession?.termsManager
    }

    fun acceptTerms() {
        val acceptedTerms = termsList.value ?: return

        isLoading.postValue(true)
        val agreedUrls = acceptedTerms.map { it.url }

        termsManager?.agreeToTerms(termsArgs.type,
                termsArgs.baseURL,
                agreedUrls,
                termsArgs.token,
                object : ApiCallback<Unit> {
                    override fun onSuccess(info: Unit) {
                        isLoading.postValue(false)
                        successfullyAccepted.postValue(true)
                    }

                    override fun onUnexpectedError(e: java.lang.Exception?) {
                        isLoading.postValue(false)
                        popError.postValue(LiveEvent(R.string.unknown_error))
                        Log.e(LOG_TAG, "Failed to agree to terms ", e)
                    }

                    override fun onNetworkError(e: java.lang.Exception?) {
                        isLoading.postValue(false)
                        popError.postValue(LiveEvent(R.string.unknown_error))
                        Log.e(LOG_TAG, "Failed to agree to terms ", e)
                    }

                    override fun onMatrixError(e: MatrixError?) {
                        isLoading.postValue(false)
                        popError.postValue(LiveEvent(R.string.unknown_error))
                        Log.e(LOG_TAG, "Failed to agree to terms " + e?.message)
                    }
                }
        )
    }

    fun reviewTerm(url: String) {

    }

    fun loadTerms() {
        isLoading.postValue(true)
        hasError.postValue(false)

        termsManager?.get(termsArgs.type, termsArgs.baseURL, object : ApiCallback<GetTermsResponse> {
            override fun onSuccess(info: GetTermsResponse) {

                val terms = mutableListOf<Term>()
                info.serverResponse.getLocalizedPrivacyPolicies()?.let {
                    Log.e("FOO", it.localizedUrl)
                    terms.add(
                            Term(it.localizedUrl ?: "",
                                    it.localizedName ?: "",
                                    // TODO i18n
                                    "Utiliser des robots, des passerelles, des widgets ou des packs de stickers",
                                    accepted = info.alreadyAcceptedTermUrls.contains(it.localizedUrl)
                            )
                    )
                }
                info.serverResponse.getLocalizedTermOfServices()?.let {
                    terms.add(
                            Term(it.localizedUrl ?: "",
                                    it.localizedName ?: "",
                                    // TODO i18n
                                    "Utiliser des robots, des passerelles, des widgets ou des packs de stickers",
                                    accepted = info.alreadyAcceptedTermUrls.contains(it.localizedUrl)
                            )
                    )
                }
                isLoading.postValue(false)
                termsList.postValue(terms)
            }


            override fun onUnexpectedError(e: Exception?) {
                hasError.postValue(true)
                isLoading.postValue(false)
            }

            override fun onNetworkError(e: Exception?) {
                hasError.postValue(true)
                isLoading.postValue(false)
            }

            override fun onMatrixError(e: MatrixError?) {
                hasError.postValue(true)
                isLoading.postValue(false)
            }

        })
    }

    companion object {
        private val LOG_TAG = AcceptTermsViewModel::javaClass.name
    }

}

data class Term(
        val url: String,
        val name: String,
        val description: String? = null,
        val version: String? = null,
        val accepted: Boolean = false
)