package org.fossasia.openevent.general.auth

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.fossasia.openevent.general.common.SingleLiveEvent
import org.fossasia.openevent.general.data.Network
import timber.log.Timber

class LoginViewModel(
    private val authService: AuthService,
    private val network: Network
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val mutableProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mutableProgress
    private val mutableUser = MutableLiveData<User>()
    val user: LiveData<User> = mutableUser
    private val mutableError = MutableLiveData<String>()
    val error: LiveData<String> = mutableError
    private val mutableShowNoInternetDialog = MutableLiveData<Boolean>()
    val showNoInternetDialog: LiveData<Boolean> = mutableShowNoInternetDialog
    private val mutableRequestTokenSuccess = MutableLiveData<Boolean>()
    val requestTokenSuccess: LiveData<Boolean> = mutableRequestTokenSuccess
    private val mutableIsCorrectEmail = MutableLiveData<Boolean>()
    val isCorrectEmail: LiveData<Boolean> = mutableIsCorrectEmail
    private val mutableLoggedIn = SingleLiveEvent<Boolean>()
    var loggedIn: LiveData<Boolean> = mutableLoggedIn

    fun isLoggedIn() = authService.isLoggedIn()

    fun login(email: String, password: String) {
        if (!isConnected()) return
        if (hasErrors(email, password)) return
        compositeDisposable.add(authService.login(email, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                mutableProgress.value = true
            }.doFinally {
                mutableProgress.value = false
            }.subscribe({
                mutableLoggedIn.value = true
            }, {
                mutableError.value = "Unable to Login. Please check your credentials"
            })
        )
    }

    private fun hasErrors(email: String?, password: String?): Boolean {
        if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
            mutableError.value = "Email or Password cannot be empty!"
            return true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mutableError.value = "Invalid email address!"
            return true
        }
        return false
    }

    fun sendResetPasswordEmail(email: String) {
        if (!isConnected()) return
        compositeDisposable.add(authService.sendResetPasswordEmail(email)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                mutableProgress.value = true
            }.doFinally {
                mutableProgress.value = false
            }.subscribe({
                mutableRequestTokenSuccess.value = verifyMessage(it.message)
            }, {
                mutableRequestTokenSuccess.value = verifyMessage(it.message.toString())
                mutableError.value = "Email address not present in server. Please check your email"
            })
        )
    }

    private fun verifyMessage(message: String): Boolean {
        if (message == "Email Sent") {
            return true
        }
        return false
    }

    fun fetchProfile() {
        if (!isConnected()) return
        compositeDisposable.add(authService.getProfile()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                mutableProgress.value = true
            }.doFinally {
                mutableProgress.value = false
            }.subscribe({ it ->
                Timber.d("User Fetched")
                mutableUser.value = it
            }) {
                Timber.e(it, "Failure")
                mutableError.value = "Failure"
            })
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun checkEmail(email: String) {
        mutableIsCorrectEmail.value = email.isNotEmpty() &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isConnected(): Boolean {
        val isConnected = network.isNetworkConnected()
        if (!isConnected) mutableShowNoInternetDialog.value = true
        return isConnected
    }
}
