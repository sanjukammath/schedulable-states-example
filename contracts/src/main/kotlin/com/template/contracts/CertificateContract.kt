package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class CertificateContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        val ID = CertificateContract::class.qualifiedName!!
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Issue : Commands
        class Revoke: Commands
        class Expire: Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        requireThat {
            "No inputs should be consumed when issuing an obligation." using (tx.inputStates.isEmpty())
        }
    }


}