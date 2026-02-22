package com.serhat.dijitalgozluk.data.gps

import kotlin.math.*

/**
 * Basit 2D Extended Kalman Filter (EKF) örneği
 * GPS + IMU (ivme, eğim) ile konum ve hız tahmini
 *
 * State: [x, y, vx, vy]
 * Ölçüm: [lat, lon, hız, eğim]
 */
class EKF(
    private var state: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0, 0.0), // [x, y, vx, vy]
    private var P: Array<DoubleArray> = Array(4) { DoubleArray(4) { 0.0 } }, // Covariance
    private val Q: Array<DoubleArray> = Array(4) { DoubleArray(4) { 0.0 } }, // Process noise
    private val R: Array<DoubleArray> = Array(4) { DoubleArray(4) { 0.0 } }  // Measurement noise
) {
    /**
     * Prediction adımı
     * dt: zaman adımı (saniye)
     * ax, ay: ivme (IMU'dan)
     */
    fun predict(dt: Double, ax: Double, ay: Double) {
        // State update: x = x + vx*dt + 0.5*ax*dt^2
        state[0] += state[2] * dt + 0.5 * ax * dt * dt
        state[1] += state[3] * dt + 0.5 * ay * dt * dt
        // Velocity update: vx = vx + ax*dt
        state[2] += ax * dt
        state[3] += ay * dt
        // Covariance update (basit): P = P + Q
        for (i in 0..3) for (j in 0..3) P[i][j] += Q[i][j]
    }

    /**
     * Ölçüm güncellemesi
     * measurement: [lat, lon, hız, eğim]
     */
    fun update(measurement: DoubleArray) {
        // Ölçüm modeli: H
        val H = Array(4) { DoubleArray(4) { 0.0 } }
        for (i in 0..3) H[i][i] = 1.0
        // Innovation: y = z - Hx
        val y = DoubleArray(4) { measurement[it] - state[it] }
        // S = HPH^T + R
        val S = Array(4) { DoubleArray(4) { 0.0 } }
        for (i in 0..3) for (j in 0..3) S[i][j] = P[i][j] + R[i][j]
        // Kalman gain: K = PH^T S^-1 (basit diagonal için)
        val K = DoubleArray(4) { P[it][it] / S[it][it] }
        // State update: x = x + K*y
        for (i in 0..3) state[i] += K[i] * y[i]
        // Covariance update: P = (I - K*H)P
        for (i in 0..3) P[i][i] = (1 - K[i]) * P[i][i]
    }

    fun getState(): DoubleArray = state
    fun reset(initialState: DoubleArray, initialP: Array<DoubleArray>) {
        state = initialState.copyOf()
        for (i in 0..3) for (j in 0..3) P[i][j] = initialP[i][j]
    }
}
