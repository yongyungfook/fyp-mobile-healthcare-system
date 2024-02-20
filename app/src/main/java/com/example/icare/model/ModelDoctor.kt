package com.example.icare.model

class ModelDoctor {
    var imageResId: Int = 0
    var name: String = ""
    var job: String = ""
    var imageResId2: Int = 0
    var name2: String = ""
    var job2: String = ""

    constructor()

    constructor(imageResId: Int, name: String, job: String, imageResId2: Int, name2: String, job2: String) {
        this.imageResId = imageResId
        this.name = name
        this.job = job
        this.imageResId2 = imageResId2
        this.name2 = name2
        this.job2 = job2
    }
}