package com.example.icare.model

class ModelNews {
    var imageResId: Int = 0
    var title: String = ""

    constructor()

    constructor(imageResId: Int, title: String) {
        this.imageResId = imageResId
        this.title = title
    }
}