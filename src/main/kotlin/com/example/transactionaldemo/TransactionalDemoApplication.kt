package com.example.transactionaldemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class TransactionalDemoApplication

fun main(args: Array<String>) {
	runApplication<TransactionalDemoApplication>(*args)
}
