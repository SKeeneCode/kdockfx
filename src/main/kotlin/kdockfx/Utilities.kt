package kdockfx

import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import kotlin.reflect.KProperty

operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>) = value
operator fun <T> Property<T>.setValue(thisRef: Any, property: KProperty<*>, value: T?) = setValue(value)