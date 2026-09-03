package com.andrerinas.openheadunit.newui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Small helper so each screen can build a custom-constructor ViewModel inline without boilerplate. */
inline fun <VM : ViewModel> huViewModelFactory(crossinline creator: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }
