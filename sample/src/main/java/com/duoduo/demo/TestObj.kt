package com.duoduo.demo

import java.io.Serializable

/**
 * Created by tianhetong on 2022/12/30
 */
data class TestObj(
    var name: String?,
    var age: Int?,
    var sex: Boolean,
    var address: String?,
    ) : Serializable