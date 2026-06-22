package com.example.bantunow.data.model

abstract class Transaction(
    var walletID:String? = null,
    var type:String? = null,
    var amount:Long = 0,
    var status:String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"

        const val TYPE_PAYMENT = "payment"
        const val TYPE_TOP_UP = "topup"
        const val TYPE_WITHDRAWAL = "withdrawal"
    }
}

class Payment(
    transactionID:String? = null,
    walletID: String? = null,
    var taskID:String? = null,
    amount: Long
) : Transaction(walletID,TYPE_PAYMENT,amount){
}

class Top_up(
    transactionID: String? = null,
    walletID: String? = null,
    var paymentMethod: String? = null,
    var referenceNo:String? = null,
    amount: Long
) : Transaction(walletID,TYPE_TOP_UP,amount){
}

class Withdrawal(
    transactionID: String? = null,
    walletID: String? = null,
    var bankName: String? = null,
    var accNumber : String? = null,
    amount: Long
) : Transaction(walletID,TYPE_WITHDRAWAL,amount)