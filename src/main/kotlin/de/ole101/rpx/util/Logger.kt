package de.ole101.rpx.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Logger {
    private val logger: Logger = LoggerFactory.getLogger("ResourcePackExtractor")

    fun info(message: String) {
        logger.info(message)
    }

    fun debug(message: String) {
        logger.debug(message)
    }

    fun warn(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            logger.warn(message, throwable)
        } else {
            logger.warn(message)
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            logger.error(message, throwable)
        } else {
            logger.error(message)
        }
    }

    fun trace(message: String) {
        logger.trace(message)
    }
}
