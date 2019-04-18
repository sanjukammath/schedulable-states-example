package com.template.contracts

import com.template.states.CertificateState
import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class CertificateStateTests{
    val alice = TestIdentity(CordaX500Name("Alice", "", "GB")).party
    val bob = TestIdentity(CordaX500Name("Bob", "", "GB")).party

    @Test
    fun certificateStateHasIssuerOwnerAndValidityParamsOfCorrectTypeInConstructor() {
        CertificateState(alice, bob, Instant.parse("2018-04-18T10:14:35.00Z"))
    }

    @Test
    fun certificateStateHasGettersForIssuerOwnerAndValidity() {
        val certificateState = CertificateState(alice, bob, Instant.parse("2018-04-18T10:14:35.00Z"))
        assertEquals(alice, certificateState.issuer)
        assertEquals(bob, certificateState.owner)
        assertEquals(Instant.parse("2018-04-18T10:14:35.00Z"), certificateState.validity)
    }

    @Test
    fun tokenStateImplementsContractState() {
        assert(CertificateState(alice, bob, Instant.parse("2018-04-18T10:14:35.00Z")) is ContractState)
    }

    @Test
    fun tokenStateHasTwoParticipantsTheIssuerAndTheOwner() {
        val certificateState = CertificateState(alice, bob, Instant.parse("2018-04-18T10:14:35.00Z"))
        assertEquals(2, certificateState.participants.size)
        assert(certificateState.participants.contains(alice))
        assert(certificateState.participants.contains(bob))
    }
}
