package com.example.icare.model

class ModelAppointment{
    var appointmentId: String = ""
    var userId: String = ""
    var appointmentTime: String = ""
    var requestTime: Long = 0
    var updateTime: Long = 0
    var updateUserId: String = ""
    var status: String = ""
    var comment: String = ""
    var description: String = ""
    var doctor: String = ""
    var reason: String = ""

    constructor()
    constructor(
        appointmentId: String,
        userId: String,
        appointmentTime: String,
        requestTime: Long,
        updateTime: Long,
        updateUserId: String,
        status: String,
        comment: String,
        description: String,
        doctor: String,
        reason: String
    ) {
        this.appointmentId = appointmentId
        this.userId = userId
        this.appointmentTime = appointmentTime
        this.requestTime = requestTime
        this.updateTime = updateTime
        this.updateUserId = updateUserId
        this.status = status
        this.comment = comment
        this.description = description
        this.doctor = doctor
        this.reason = reason
    }

    constructor(
        appointmentId: String,
        userId: String,
        appointmentTime: String,
        status: String,
        description: String
    ) {
        this.appointmentId = appointmentId
        this.userId = userId
        this.appointmentTime = appointmentTime
        this.status = status
        this.description = description
    }


}


