Я сосредоточился на анализе потенциально опасных объявлений, потому как анализ тел функций мне показался трудоемким и сложным

Кажется, обработал все пять кейсов из последнего параграфа https://kotlinlang.org/docs/reference/native/concurrency.html
Если этого маловато, то буду думать еще

2) ThreadLocal (USELESS_THREAD_LOCAL)
    - только глобальные объявления
    - `val` или `var` с делегатом
    - нет backingField (readOnly property/setter без присваивания field)
3) SharedImuutable (USELESS_SHARED_IMMUTABLE)
    - только глобальные объявления
    - `val` или `var` с делегатом
    - нет backingField (readOnly property/setter без присваивания field)
4) Singleton (MUTABLE_SINGLETON):
    - содержит мутабельное свойство
    - нет делегата
    - не помечен @ThreadLocal
    - есть backing field
5) Enum (MUTABLE_ENUM):
    - содержит мутабельное свойство
    - нет делегата
    - ThreadLocal не рассматриваем, enum frozen всегда
    - есть backing field 
6) Подсказанный кейс с мутабельным объектом в свойстве (PREFER_ATOMIC_REFERNCE)
    - объявлен в синглтоне без @ThreadLocal
    - `val`
    - `kind == ClassKind.CLASS` (мутабельные enum и object проверяются другой инспекцией)
    - потенциально мутабелен (не примитив, строка или data class)
    - есть backing field
    - не Atomic
```kotlin
class MutableBox(var x: Int)
object Singleton { 
    val box: MutableBox = MutableBox(42) // варнингом не подсветится, но при мутации упадет, потому что мутируем объект в иерархии frozen родителя
}
```

## Могу подумать, как сделать, если внесененных изменений маловато:
### Мутирование объекта после вызова у него `freeze()`
Не очень понятно, как быть с ветвлением кода, но, возможно, можно написать инспекцию, которая проверяет отсутствие мутаций объекта после вызова у него `freeze()`

### Присваивание backing field в сеттере enum не обрабатывается MUTABLE_ENUM, хотя приведет к крашу
Код, проверяющий отсутствие наличие записи в `field`, находится в `SetterBackingFieldAssignmentInspection`. Условие нужно инвертировать. 
Я не уверен, как переиспользовать этот код в native, не правя при этом конфиг градла. Можно просто скопировать, но я бы уточнил этот момент
```
enum class EnumWithSetter {
    ONE;

    var fieldWithSetter: Int = 0
        set(value) {
            field = value
        }
}
```

### lazy values allowed, unless cyclic frozen structures were attempted to be created
Показалось сложным, отложил в сторону. Вероятно, я бы рекурсивно двигался по инструкциям в lazy property body и искал бы цикл ссылок

### Моки функций библиотек
Я долго думал над этим советом, но так и не понял, как это может помочь. 
Возможно, речь об инспекции, проверяющей не мутируется ли НЕ на главном потоке глобальное значение, не помеченное @ThreadLocal/@SharedImmutable.
И чтобы проверить поток на этапе статического анализа, она сравнивает название parent с функциями `launch`, `async`, `runBlocking` и т.д.
Показалась, что очень много корнер кейсов, но если это перспективно, мог бы закопаться.

P.S. Спасибо, за интересное задание :)
