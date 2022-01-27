package com.example.transactionaldemo

import org.junit.jupiter.api.Test

internal class PersonManagerTest {

    @Test
    fun f1() {
        println("f1: ${dd(1)}")
    }

    @Test
    fun f2() {
        println("f2: ${dd(2)}")
    }

    fun dd(i: Int): List<Int> {
        return try {
            if (i == 1) {
                listOf(1,2,3)
            } else {
                throw Exception("dkdkd")
            }
        } catch (e: Exception) {
           throw e
        }
    }
}