package com.template.contracts

import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test
import kotlin.test.assertEquals

class CertificateStateTests{
    val alice = TestIdentity(CordaX500Name("Alice", "", "GB")).party
    val bob = TestIdentity(CordaX500Name("Bob", "", "GB")).party

}
