package org.fossasia.openevent.general.event

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.fossasia.openevent.general.data.Preference
import org.fossasia.openevent.general.search.SAVED_LOCATION
import timber.log.Timber

class EventsViewModel(private val eventService: EventService, private val preference: Preference) :
    ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val mutableProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mutableProgress
    private val mutableEvents = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = mutableEvents
    private val mutableError = MutableLiveData<String>()
    val error: LiveData<String> = mutableError
    private val mutableShowShimmerEvents = MutableLiveData<Boolean>()
    val showShimmerEvents: LiveData<Boolean> = mutableShowShimmerEvents
    private val mutableShowNoEventLayout = MutableLiveData<Boolean>()
    val showNoEventLayout: LiveData<Boolean> = mutableShowNoEventLayout

    var savedLocation: String? = null

    fun loadLocation() {
        savedLocation = preference.getString(SAVED_LOCATION)
    }

    fun loadLocationEvents() {
        val query = "[{\"name\":\"location-name\",\"op\":\"ilike\",\"val\":\"%$savedLocation%\"}]"
        mutableShowNoEventLayout.value = false
        compositeDisposable.add(eventService.getEventsByLocation(query)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                mutableProgress.value = false
                mutableShowShimmerEvents.value = false
            }.subscribe({
                mutableShowNoEventLayout.value = it.isEmpty()
                mutableEvents.value = it
            }, {
                Timber.e(it, "Error fetching events")
                mutableError.value = "Error fetching events"
            })
        )
    }

    fun retryLoadLocationEvents() {
        mutableShowShimmerEvents.value = true
        loadLocationEvents()
    }

    fun loadEvents() {
        mutableShowNoEventLayout.value = false
        compositeDisposable.add(eventService.getEvents()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe({
                mutableProgress.value = true
            }).doFinally({
                mutableProgress.value = false
            }).subscribe({
                mutableShowNoEventLayout.value = it.isEmpty()
                mutableEvents.value = it
            }, {
                Timber.e(it, "Error fetching events")
                mutableError.value = "Error fetching events"
            })
        )
    }

    fun setFavorite(eventId: Long, favorite: Boolean) {
        compositeDisposable.add(eventService.setFavorite(eventId, favorite)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("Success")
            }, {
                Timber.e(it, "Error")
                mutableError.value = "Error"
            })
        )
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}
