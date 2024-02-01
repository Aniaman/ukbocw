package com.example.ukbocw.utils


class AuthenticatorHeader {

    fun getMainCustomerHeader(): HashMap<String, String> {
        val userHeaders = HashMap<String, String>()
        userHeaders["x-api-key"] =
            "81da502ffc3704996c28cfaf3d33013b18cf438a6e9d374deb5e2dd9d3c28eff"
        return userHeaders
    }
}

