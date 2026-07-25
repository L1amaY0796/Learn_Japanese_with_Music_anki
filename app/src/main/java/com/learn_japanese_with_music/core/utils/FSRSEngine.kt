package com.learn_japanese_with_music.core.utils

import kotlin.math.*

/**
 * FSRS-6 (Free Spaced Repetition Scheduler) Implementation
 * Based on DSR (Difficulty, Stability, Retrievability) model.
 */
object FSRSEngine {

    // Default weights (w0 - w16) based on FSRS v4/v5 averages
    private val defaultWeights = doubleArrayOf(
        0.4, 0.6, 2.4, 5.8, // Initial stability for Again, Hard, Good, Easy
        4.93, 0.94, 0.86, 0.01, // Difficulty related
        1.49, 0.14, // Stability related
        0.94, 2.18, 0.05, 0.34, 1.26, // Lapse related
        0.29, 2.61 // Review related
    )

    // FSRS-6 factor for R=0.9
    // R(t, S) = (1 + factor * t/S)^-w20
    private const val w20 = 0.4 
    private val factor = (0.9.pow(-1.0 / w20) - 1.0)

    fun calculateRetrievability(daysElapsed: Double, stability: Double): Double {
        if (stability == 0.0) return 0.0
        return (1.0 + factor * daysElapsed / stability).pow(-w20)
    }

    fun calculateInterval(stability: Double, desiredRetention: Double = 0.9): Int {
        if (stability == 0.0) return 0
        // I = S / factor * (DR^-1/w20 - 1)
        val interval = (stability / factor * (desiredRetention.pow(-1.0 / w20) - 1.0))
        return max(1, interval.roundToInt())
    }

    fun initDifficulty(grade: Int): Double {
        // D0(G) = w4 - exp(w5 * (G-1)) + 1
        val w4 = defaultWeights[4]
        val w5 = defaultWeights[5]
        return (w4 - exp(w5 * (grade - 1)) + 1.0).coerceIn(1.0, 10.0)
    }

    fun initStability(grade: Int): Double {
        // S0(G) = w[G-1]
        return defaultWeights[grade - 1].coerceAtLeast(0.1)
    }

    fun nextDifficulty(currentD: Double, grade: Int): Double {
        // Delta D = -w6 * (G - 3)
        // Mean Reversion: D' = w7 * w4 + (1 - w7) * D'
        val w4 = defaultWeights[4]
        val w6 = defaultWeights[6]
        val w7 = defaultWeights[7]
        
        val deltaD = -w6 * (grade - 3)
        var nextD = currentD + deltaD
        
        // Mean reversion
        nextD = w7 * w4 + (1.0 - w7) * nextD
        
        return nextD.coerceIn(1.0, 10.0)
    }

    fun nextStabilitySuccess(currentD: Double, currentS: Double, currentR: Double, grade: Int): Double {
        val w8 = defaultWeights[8]
        val w9 = defaultWeights[9]
        val w10 = defaultWeights[10]
        val w15 = defaultWeights[15]
        val w16 = defaultWeights[16]

        val hardPenalty = if (grade == 2) w15 else 1.0
        val easyBonus = if (grade == 4) w16 else 1.0
        
        // S' = S * (1 + exp(w8) * (11 - D) * S^-w9 * (exp(w10 * (1 - R)) - 1) * Penalty/Bonus)
        val inc = 1.0 + exp(w8) * (11.0 - currentD) * currentS.pow(-w9) * (exp(w10 * (1.0 - currentR)) - 1.0) * hardPenalty * easyBonus
        return (currentS * inc).coerceAtLeast(0.1)
    }

    fun nextStabilityFailure(currentD: Double, currentS: Double, currentR: Double): Double {
        val w11 = defaultWeights[11]
        val w12 = defaultWeights[12]
        val w13 = defaultWeights[13]
        val w14 = defaultWeights[14]

        // S' = w11 * D^-w12 * ((S+1)^w13 - 1) * exp(w14 * (1-R))
        val nextS = w11 * currentD.pow(-w12) * ((currentS + 1.0).pow(w13) - 1.0) * exp(w14 * (1.0 - currentR))
        return nextS.coerceIn(0.1, currentS)
    }
}
