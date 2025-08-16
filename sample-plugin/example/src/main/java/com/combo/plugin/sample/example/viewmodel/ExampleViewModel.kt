package com.combo.plugin.sample.example.viewmodel

import com.combo.plugin.sample.common.viewmodel.BaseViewModel
import com.combo.plugin.sample.example.state.ExampleState

class ExampleViewModel : BaseViewModel<ExampleState>(
        initialState = ExampleState()
) {


}