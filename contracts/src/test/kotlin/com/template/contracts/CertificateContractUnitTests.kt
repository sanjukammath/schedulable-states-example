package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import com.template.states.CertificateState
import net.corda.core.contracts.BelongsToContract
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.makeTestIdentityService
import java.time.Instant

abstract class CertificateContractUnitTests {
    protected val ledgerServices = MockServices(
            listOf("com.template.contracts", "net.corda.testing.contracts", "net.corda.finance.contracts"),
            identityService = makeTestIdentityService(),
            initialIdentity = TestIdentity(CordaX500Name("TestIdentity", "", "GB")))
    protected val alice = TestIdentity(CordaX500Name("Alice", "", "GB"))
    protected val bob = TestIdentity(CordaX500Name("Bob", "", "GB"))
    protected val charlie = TestIdentity(CordaX500Name("Bob", "", "GB"))

    protected class DummyState : ContractState {
        override val participants: List<AbstractParty> get() = listOf()
    }

    protected class DummyCommand : CommandData

    protected val issuedCertificate = CertificateState(alice.party, bob.party, Instant.parse("2019-05-18T10:14:35.00Z"))
    protected val expiredCertificate = CertificateState(alice.party, bob.party, null)
}