package com.wanderingledger.app

import com.wanderingledger.core.data.CompanionCommentary
import com.wanderingledger.feature.companions.CompanionCommentaryUi

/** Maps a core commentary value into the companions UI model. */
internal fun CompanionCommentary.toUi(): CompanionCommentaryUi =
    CompanionCommentaryUi(
        companionId = companionId,
        companionName = companionName,
        line = line,
        tone = tone,
    )
