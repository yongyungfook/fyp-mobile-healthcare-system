package com.example.icare.model

class ModelAccount {
    var uid: String? = null
    var name: String? = null
    var role: String? = null
    var email: String? = null
    var gender: String? = null
    var regDate: Long? = 0
    var phoneNumber: String? = null

    constructor()

    constructor(uid: String, name: String, role: String,  email: String, gender: String, regDate: Long, phoneNumber: String) {
        this.uid = uid
        this.name = name
        this.role = role
        this.email = email
        this.gender = gender
        this.regDate = regDate
        this.phoneNumber = phoneNumber
    }
}