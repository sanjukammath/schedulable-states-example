package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.CertificateContract
import com.template.states.CertificateState
import net.corda.core.contracts.StateRef
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step

// *********
// * Flows *
// *********
@InitiatingFlow
@SchedulableFlow
class ExpireCertificateInitiator(private val stateRef: StateRef) : FlowLogic<Unit>() {
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
    override fun call() {

        val input = serviceHub.toStateAndRef<CertificateState>(stateRef)
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        val outputState = input.state.data.copy(validity = null)
        if (input.state.data.issuer != ourIdentity) {
            return
        }

        val owner = outputState.owner

        progressTracker.currentStep = GENERATING_TRANSACTION
        val transactionBuilder = TransactionBuilder()
        transactionBuilder.notary = notary
        transactionBuilder.addInputState(input)
        transactionBuilder.addOutputState(outputState)
        transactionBuilder.addCommand(CertificateContract.Commands.Expire(),ourIdentity.owningKey, owner.owningKey)

        progressTracker.currentStep = VERIFYING_TRANSACTION
        transactionBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val partSignedTx  = serviceHub.signInitialTransaction(transactionBuilder)

        progressTracker.currentStep = GATHERING_SIGS
        val counterPartySession = initiateFlow(owner)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, listOf(counterPartySession), GATHERING_SIGS.childProgressTracker()))

        progressTracker.currentStep = FINALISING_TRANSACTION
        subFlow(FinalityFlow(fullySignedTx, listOf(counterPartySession), FINALISING_TRANSACTION.childProgressTracker()))
    }
}

@InitiatedBy(ExpireCertificateInitiator::class)
class ExpireCertificateResponder(val counterPartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call() : SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterPartySession) {
            override fun checkTransaction(stx: SignedTransaction) {}
        }

        val expectedTxId = subFlow(signTransactionFlow).id

        return subFlow(ReceiveFinalityFlow(counterPartySession, expectedTxId))
    }
}
