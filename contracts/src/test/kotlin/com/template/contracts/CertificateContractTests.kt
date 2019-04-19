package com.template.contracts

import com.template.states.CertificateState
import net.corda.core.contracts.Contract
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.DummyCommandData
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.time.Instant

class CertificateContractTests {
    private val alice = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val bob = TestIdentity(CordaX500Name("Bob", "", "GB"))
    private val ledgerServices = MockServices(listOf("com.template.contracts.CertificateContract", "com.template.workflows.IssueCertificateFlow"))
    private val certificateState = CertificateState(alice.party, bob.party, Instant.parse("2018-04-18T10:14:35.00Z"))

    @Test
    fun certificateContractImplementsContract() {
        assert((CertificateContract() is Contract))
    }

    @Test
    fun certificateContractRequiresOneCommandInTheTransaction() {
        ledgerServices.ledger {
            transaction {
                // Has wrong command type, will fail.
                output(CertificateContract.ID, certificateState)
                command(alice.publicKey, DummyCommandData)
                fails()
            }
            transaction {
                // Has correct command type, will verify.
                output(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Issue())
                verifies()
            }
            transaction {
                // Has multiple commands, will fail.
                output(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Issue())
                command(alice.publicKey, DummyCommandData)
                fails()
            }
            transaction {
                // Has correct command type, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                verifies()
            }
            transaction {
                // Has correct command type, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Expire())
                verifies()
            }
            transaction {
                // Has zero commands, will fail.
                output(CertificateContract.ID, certificateState)
                fails()
            }
            transaction {
                // Has zero commands, will fail.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState)
                fails()
            }
        }
    }

    @Test
    fun issueCertificateRequiresZeroInputsInTheTransaction(){
        ledgerServices.ledger {
            transaction {
                // Has an input, will fail.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Issue())
                fails()
            }
            transaction {
                // Has no input, will verify.
                output(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun revokeCertificateRequiresOneInputInTheTransaction(){
        ledgerServices.ledger {
            transaction {
                // Has an input, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                verifies()
            }
            transaction {
                // Has no input, will fail.
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                fails()
            }
        }
    }

    @Test
    fun expireCertificateRequiresOneInputInTheTransaction(){
        ledgerServices.ledger {
            transaction {
                // Has an input, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Expire())
                verifies()
            }
            transaction {
                // Has no input, will fail.
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Expire())
                fails()
            }
        }
    }

    @Test
    fun certificateContractRequiresOneOutputState(){
        ledgerServices.ledger {
            transaction {
                // Has an output, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Expire())
                verifies()
            }
            transaction {
                // Has an output, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                verifies()
            }
            transaction {
                // Has no output, will fail.
                input(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Expire())
                fails()
            }
            transaction {
                // Has no output, will fail.
                command(alice.publicKey, CertificateContract.Commands.Issue())
                fails()
            }
            transaction {
                // Has no output, will fail.
                input(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                fails()
            }
        }
    }


    @Test
    fun certificateContractRequiresIssuerAndOwnerTobeDifferent(){
        ledgerServices.ledger {
            transaction {
                // Issuer is different from Owner, will verify.
                input(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Issue())
                verifies()
            }
            transaction {
                // Issuer is same as owner, will fail.
                input(CertificateContract.ID, certificateState.copy(owner = alice.party))
                command(alice.publicKey, CertificateContract.Commands.Issue())
                fails()
            }
        }
    }

    @Test
    fun certificateContractRequiresThatIssuerCannotBeChanged() {
        ledgerServices.ledger {
            transaction {
                // Issuer is same in Input and output, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Expire())
                verifies()
            }
            transaction {
                // Issuer is different in Input and output, will fail.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null, issuer = bob.party))
                command(alice.publicKey, CertificateContract.Commands.Expire())
                fails()
            }
            transaction {
                // Issuer is same in Input and output, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                verifies()
            }
            transaction {
                // Issuer is different in Input and output, will fail.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null, issuer = bob.party))
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                fails()
            }
        }
    }

    @Test
    fun certificateContractRequiresThatOwnerCannotBeChanged() {
        ledgerServices.ledger {
            transaction {
                // Owner is same in Input and output, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Expire())
                verifies()
            }
            transaction {
                // Owner is different in Input and output, will fail.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null, owner = alice.party))
                command(alice.publicKey, CertificateContract.Commands.Expire())
                fails()
            }
            transaction {
                // Owner is same in Input and output, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                verifies()
            }
            transaction {
                // Owner is different in Input and output, will fail.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null, owner = alice.party))
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                fails()
            }
        }
    }

    @Test
    fun certificateContractRequiresIssuerSignForAnyStateChange(){
        ledgerServices.ledger {
            transaction {
                // Has correct sign requirement, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Expire())
                verifies()
            }
            transaction {
                // Has correct sign requirement, will verify.
                input(CertificateContract.ID, certificateState)
                output(CertificateContract.ID, certificateState.copy(validity = null))
                command(alice.publicKey, CertificateContract.Commands.Revoke())
                verifies()
            }
            transaction {
                // Has correct sign requirement, will verify.
                output(CertificateContract.ID, certificateState)
                command(alice.publicKey, CertificateContract.Commands.Issue())
                verifies()
            }
            transaction {
                // Has incorrect sign requirement, will fail.
                input(CertificateContract.ID, certificateState)
                command(bob.publicKey, CertificateContract.Commands.Expire())
                fails()
            }
            transaction {
                // Has incorrect sign requirement, will fail.
                command(bob.publicKey, CertificateContract.Commands.Issue())
                fails()
            }
            transaction {
                // Has incorrect sign requirement, will fail.
                input(CertificateContract.ID, certificateState)
                command(bob.publicKey,CertificateContract.Commands.Revoke())
                fails()
            }
        }
    }
}