package com.template.states

import com.template.contracts.CertificateContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant

// *********
// * State *
// *********
@BelongsToContract(CertificateContract::class)
data class CertificateState(val issuer: Party,
                            val owner: Party,
                            val validity: Instant) : ContractState, SchedulableState{

    override val participants: List<AbstractParty> = listOf(issuer, owner)

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
