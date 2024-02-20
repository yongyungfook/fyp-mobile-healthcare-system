package com.example.icare.model

class ModelInventory {
    var itemId: String = ""
    var userId: String = ""
    var itemName: String = ""
    var image: String = ""
    var addedTime: Long = 0
    var description: String = ""
    var price: Double = 0.0
    var stock: Int = 0
    var updateTime: Long = 0
    var updateUserId: String = ""

    constructor()

    constructor(
        itemId: String,
        userId: String,
        itemName: String,
        image: String,
        addedTime: Long,
        description: String,
        stock: Int,
        updateTime: Long,
        updateUserId: String
    ) {
        this.itemId = itemId
        this.userId = userId
        this.itemName = itemName
        this.image = image
        this.addedTime = addedTime
        this.description = description
        this.stock = stock
        this.updateTime = updateTime
        this.updateUserId = updateUserId
    }
}