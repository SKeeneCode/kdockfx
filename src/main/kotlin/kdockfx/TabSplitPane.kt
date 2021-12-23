package kdockfx

import javafx.collections.ListChangeListener
import javafx.scene.control.SplitPane

class TabSplitPane : SplitPane() {
    init {
        items.addListener(ListChangeListener { change ->
            if (change.next()) {
                change.addedSubList.filterIsInstance<DetachableTabPane>().forEach { it.parentSplitPane = this }
                change.removed.filterIsInstance<DetachableTabPane>().forEach { it.parentSplitPane = null }
            }
        })
    }
}