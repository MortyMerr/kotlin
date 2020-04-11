// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME

// FILE: ListId.java
import java.util.List;
import org.jetbrains.annotations.NotNull;

class ListId {
    @NotNull
    static <T> List<T> id(List<T> v) {
        return v;
    }
}

// FILE: test.kt

fun <T> problematic(lss: List<List<T>>): List<T> = lss.flatMap { ListId.id(it) }

fun box() = problematic(listOf(listOf("OK")))[0]
