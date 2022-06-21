package com.jnj.vaccinetracker.common.validators

import com.jnj.vaccinetracker.common.domain.entities.Configuration
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class ParticipantIdValidator @Inject constructor(
    private val getConfigurationUseCase: GetConfigurationUseCase,
) {
    companion object {
        private const val MIN_LENGTH = 3

        /**
         * Normally is would be 50 but we subtract 7 chars to make room for duplicate id suffix (~ddHHmm)
         */
        private const val MAX_LENGTH = 43
        private val AUTO_GENERATED_PARTICIPANT_ID_REGEX = Regex("^[a-z]{2,3}-[a-z0-9]{4,}-[0-9]{2,}-[0-9]{4,}", RegexOption.IGNORE_CASE)

    }

    /**
     * @param participantId manually entered or from barcode
     */
    private fun Configuration.validateManualParticipantId(participantId: String): Boolean {
        return if (!participantIDRegex.isNullOrEmpty()) {
            logInfo("checking '$participantId' with regex: $participantIDRegex")
            try {
                val regex = participantIDRegex.toRegex()
                val isValid = regex.matches(participantId)
                isValid.also { logInfo("regex check completed - valid: $it") }
            } catch (e: Exception) {
                logError("Invalid regex defined in configuration $participantIDRegex", e)
                false
            }
        } else {
            true
        }
    }

    private fun validateAutoGeneratedParticipantId(participantId: String): Boolean {
        return AUTO_GENERATED_PARTICIPANT_ID_REGEX.matches(participantId)
    }

    /**
     * First check if it does not contain white spaces and if length is valid.
     * Afterwards check if regex is present, check if regex is correct.
     */
    suspend fun validate(participantId: String): Boolean {
        var isValid = participantId.none { it.isWhitespace() } && participantId.length >= MIN_LENGTH && participantId.length <= MAX_LENGTH

        if (isValid) {
            val config = getConfigurationUseCase.getMasterData()
            isValid = if (config.isAutoGenerateParticipantId) {
                validateAutoGeneratedParticipantId(participantId)
            } else {
                config.validateManualParticipantId(participantId)
            }
        }
        return isValid
    }
}