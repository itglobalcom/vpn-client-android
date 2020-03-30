package com.itglobal.vpn_client_android

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.itglobal.vpn_client_android.interactor.UserInteractor
import com.itglobal.vpn_client_android.models.Country
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_countries.*
import javax.inject.Inject

class CountriesDialogFragment(
    private val serverClickListener: (Country) -> Unit,
    private val dismissListener: (Boolean) -> Unit
) : BottomSheetDialogFragment() {

    @Inject
    lateinit var interactor: UserInteractor

    private val adapter = CountriesAdapter { item -> selectServer(item) }
    private var disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        VpnApp.INSTANCE.provideAppComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_countries, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rvCountries.layoutManager = LinearLayoutManager(context)
        rvCountries.adapter = adapter
        btnRefresh.setOnClickListener { loadCountries(true) }
        loadCountries(false)
    }

    private fun loadCountries(forceLoad: Boolean) {
        btnRefresh.isEnabled = false
        progress.visibility = View.VISIBLE
        disposables.add(interactor.getCountries(forceLoad)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    btnRefresh.isEnabled = true
                    progress.visibility = View.GONE
                    if (result != null) {
                        val countries = mutableListOf<Country>()
                        countries.addAll(result)
                        countries.add(0, Country(0, "", getString(R.string.default_location), null))
                        adapter.setData(countries)
                    }
                },
                {
                    btnRefresh.isEnabled = true
                    progress.visibility = View.GONE
                    Toast.makeText(requireContext(), R.string.default_error_message, Toast.LENGTH_LONG).show()
                }
            ))
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListener(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    private fun selectServer(country: Country) {
        serverClickListener(country)
        dialog?.dismiss()
    }

    companion object {
        fun getInstance(
            listener: (Country) -> Unit,
            dismissListener: (Boolean) -> Unit
        ): CountriesDialogFragment {
            return CountriesDialogFragment(listener, dismissListener)
        }
    }
}