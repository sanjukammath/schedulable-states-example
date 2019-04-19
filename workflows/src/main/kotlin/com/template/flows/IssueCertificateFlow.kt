package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.CertificateContract
import com.template.states.CertificateState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import java.time.Instant

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class IssueCertificateInitiator(private val owner: Party, private val text: String) : FlowLogic<SignedTransaction>() {
    companion object {
        object GENERATING_TRANSACTION : Step("Generating transaction based on new IOU.")
        object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
        object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        println("Entered the issue flow")

        val issuer = ourIdentity

        val certificateState = CertificateState(issuer, owner, Instant.parse(text))

        progressTracker.currentStep = GENERATING_TRANSACTION

        val transactionBuilder = TransactionBuilder(notary)
                .addOutputState(certificateState, CertificateContract.ID)
                .addCommand(CertificateContract.Commands.Issue(), issuer.owningKey, owner.owningKey)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val partSignedTx  = serviceHub.signInitialTransaction(transactionBuilder)

        progressTracker.currentStep = GATHERING_SIGS
        val counterPartySession = initiateFlow(owner)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(counterPartySession), GATHERING_SIGS.childProgressTracker()))

        progressTracker.currentStep = FINALISING_TRANSACTION
        return subFlow(FinalityFlow(fullySignedTx, listOf(counterPartySession), FINALISING_TRANSACTION.childProgressTracker()))
    }
}

@InitiatedBy(IssueCertificateInitiator::class)
class IssueCertificateResponder(val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    @Throws(FlowException::class)
    override fun call() : SignedTransaction{
        val signTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction." using (output is CertificateState)
                }
            }
        }

        val expectedTxId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(counterPartySession, expectedTxId))
    }
}

