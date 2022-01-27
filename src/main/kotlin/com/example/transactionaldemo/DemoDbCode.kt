package com.example.transactionaldemo

import com.example.transactionaldemo.PersonRepositoryJdbc.Companion.ROW_MAPPER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.jdbc.support.KeyHolder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.function.ServerResponse.async
import java.sql.Connection
import java.sql.ResultSet
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id


@Entity
class Person(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    var name: String
) {
    override fun toString(): String {
        return "Person(id=$id, name='$name')"
    }
}

data class PersonDto(val name: String)

@Repository
interface PersonRepository : JpaRepository<Person, Long>

@Repository
interface PersonRepositoryJdbc {
    fun findAll(): List<Person>
    fun save(person: Person): Person?
    fun findById(id: Long): Person?
    fun deleteById(id: Long): Long

    companion object {
        val ROW_MAPPER: RowMapper<Person> = RowMapper<Person> { resultSet: ResultSet, rowNum: Int ->
            Person(
                resultSet.getLong("id"), resultSet.getString("name"))
        }
    }
}

@Component
class PersonManager(
        val repository: PersonRepositoryJdbcImpl
       // val repository: PersonRepository
        )
{

    fun getAll(): List<Person> {
        return repository.findAll()
    }

    @Transactional(rollbackFor = [Exception::class])
    fun deleteById(id: Long) {
        repository.deleteById(id)
        if (id == 3L) {
            runBlocking {
                waitAndThrow("Id == 3")
            }
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun add(person: Person): Person {
        println("add person: ${person}")
        person.id = null
        val save = repository.save(person)
        if (person.name == "XYZ") {
            runBlocking {
                waitAndThrow("Name == XYZ")
            }
        }
        if (save == null) {
            throw Exception("Can't create person")
        }
        return save

    }

    @Transactional(rollbackFor = [Exception::class, IllegalArgumentException::class])
    fun asyncDeleteById(id: Long) {
        repository.deleteById(id)
        if (id == 3L) {
            CoroutineScope(IO).launch {
                async {
                    asyncWaitAndThrow("async job")
                }
            }
        }
    }

}

@Component
class PersonRepositoryJdbcImpl @Autowired constructor(private val jdbcTemplate: JdbcTemplate) : PersonRepositoryJdbc {
    override fun findAll(): List<Person> {
        return jdbcTemplate.query("SELECT * FROM Person", ROW_MAPPER)
    }

    override fun save(person: Person): Person? {
        val insert = ("insert into person(name) values (?)")
        val keyHolder: KeyHolder = GeneratedKeyHolder()

        jdbcTemplate.update({ connection: Connection ->
            val ps = connection.prepareStatement(insert, arrayOf("id"))
            ps.setString(1, person.name)
            ps
        }, keyHolder)

        if (keyHolder.key == null) {
            throw Exception("Can't create person")
        }

        return findById(keyHolder.key as Long)
    }

    override fun findById(id: Long): Person? {
        return jdbcTemplate.query("SELECT * FROM person WHERE id = $id", ROW_MAPPER).firstOrNull()
    }

    override fun deleteById(id: Long): Long {
        return jdbcTemplate.update("DELETE FROM person WHERE id = $id").toLong()
    }

}


fun asyncWaitAndThrow(msg: String) {
    print("Wait and...")
    println("Throw: $msg")
    throw Exception(msg)
}

suspend fun waitAndThrow(msg: String) {
    print("Wait and...")
    delay(1000L)
    println("Throw: $msg")
    throw Exception(msg)
}


@RestController
@RequestMapping("/persons")
class PersonControllers(val manager: PersonManager) {

    @GetMapping
    fun getPersons(): List<Person> {
        return manager.getAll()
    }

    @PostMapping
    fun createPerson(@RequestBody dto: PersonDto): Person {
        return manager.add(Person(name = dto.name))
    }

    @DeleteMapping("/{id}")
    fun deletePerson(@PathVariable("id") id: Long) {
        manager.deleteById(id)
    }

    @DeleteMapping("/async/{id}")
    fun asyncDeletePerson(@PathVariable("id") id: Long) {
        manager.asyncDeleteById(id)
    }

}
