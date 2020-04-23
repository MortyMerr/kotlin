// FILE: annotation.kt

package kotlin.native.concurrent

@Target(AnnotationTarget.CLASS)
annotation class ThreadLocal

// FILE: test.kt

import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KProperty

data class DataClass(val l: Int)

class AtomicReference(val wrapped: MutableBox)

var global = MutableBox(2)

class MutableBox(var x: Int)
class ImmutableBox(val x: Int)
class AtomicInt(val x: Int)
class AtomicLong(val x: Int)

object Singleton {
    val dataClass = DataClass(1)
    <!PREFER_ATOMIC_REFERENCE!>val box: MutableBox = MutableBox(42)<!>
    <!PREFER_ATOMIC_REFERENCE!>val boxImmutable = ImmutableBox(2)<!>
    val boxAtomic = AtomicReference(MutableBox(42))
    val boxPropertyReadOnly: MutableBox
        get() = MutableBox(4)
    var boxPropertyWithoutBacking = MutableBox(4)
        set(value) {
            global = value
        }
    val primitve = 0
    val atomicInt = AtomicInt(1)
    val atomicLong = AtomicLong(1)
}

@ThreadLocal
object SingletonThreadLocal {
    val box: MutableBox = MutableBox(42)
    val boxImmutable = ImmutableBox(2)
    val boxAtomic = AtomicReference(MutableBox(42))
    val boxPropertyReadOnly: MutableBox
        get() = MutableBox(4)
    var boxPropertyWithoutBacking = MutableBox(4)
        set(value) {
            global = value
        }
    val primitve = 0
}
