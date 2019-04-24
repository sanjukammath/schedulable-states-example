package com.template.contracts

import net.corda.core.identity.CordaX500Name
import com.template.states.CertificateState
import com.template.contracts.CertificateContract
import com.template.contracts.CertificateContract.Companion.ID
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.ledger
import org.junit.Test

class CertificateContractIssueTests : CertificateContractUnitTests() {

    @Test
    fun `issue obligation transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(ID, DummyState())
                command(listOf(alice.publicKey, bob.publicKey), CertificateContract.Commands.Issue())
                output(ID, issuedCertificate)
                this `fails with` "No inputs should be consumed when issuing an obligation."
            }
            transaction {
                output(ID, issuedCertificate)
                command(listOf(alice.publicKey, bob.publicKey), CertificateContract.Commands.Issue())
                verifies() // As there are no input states.
            }
        }
    }
}