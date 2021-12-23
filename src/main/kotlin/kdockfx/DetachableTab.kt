package kdockfx

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Node
import javafx.scene.control.Tab

class DetachableTab(title: String = "", content: Node? = null) : Tab(title, content) {
    private val _isDetachableProperty: BooleanProperty = SimpleBooleanProperty(true)
    var isDetachable: Boolean
    get() = _isDetachableProperty.value
    set(value) = _isDetachableProperty.set(value)
    fun isDetachableProperty() = _isDetachableProperty
}